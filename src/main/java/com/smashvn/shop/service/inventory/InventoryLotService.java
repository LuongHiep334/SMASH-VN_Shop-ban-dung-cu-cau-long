package com.smashvn.shop.service.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dto.inventory.*;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLotService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final com.smashvn.shop.repository.EditLogRepository editLogRepository;
    private final com.smashvn.shop.repository.PhieuNhapRepository phieuNhapRepository;
    private final com.smashvn.shop.repository.PhieuNhapChiTietRepository phieuNhapChiTietRepository;
    private final com.smashvn.shop.repository.NhanVienRepository nhanVienRepository;
    private final com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;
    private final AuditService auditService;

    @Value("${inventory.lot.enabled-from:2026-08-07T08:30:00}")
    private String enabledFromStr;

    /**
     * Sinh mã phiếu nhập tự động theo định dạng PN000001, PN000002...
     */
    public synchronized String generateMaPhieuNhap() {
        Integer maxId = phieuNhapRepository.findMaxId();
        int nextId = (maxId != null) ? maxId + 1 : 1;
        String code = String.format("PN%06d", nextId);
        while (phieuNhapRepository.findByMaPhieuNhap(code).isPresent()) {
            nextId++;
            code = String.format("PN%06d", nextId);
        }
        return code;
    }

    /**
     * Phân bổ tồn kho FIFO hai giai đoạn (Two-Phase Allocation)
     */
    @Transactional
    public AllocationResult allocateFifo(List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return new AllocationResult(AllocationStatus.SUCCESS, Collections.emptyList(), "Danh sách yêu cầu rỗng");
        }

        Map<Integer, SanPhamChiTiet> repSpctMap = new HashMap<>();
        for (OrderItemRequest req : itemRequests) {
            SanPhamChiTiet rep = sanPhamChiTietRepository.findById(req.getRepresentativeSpctId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể đại diện ID: " + req.getRepresentativeSpctId()));
            repSpctMap.put(req.getRepresentativeSpctId(), rep);
        }

        List<Integer> sortedProductIds = repSpctMap.values().stream()
                .map(spct -> (spct != null && spct.getSanPham() != null) ? spct.getSanPham().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<Integer, SanPham> lockedProductMap = new HashMap<>();
        for (Integer spId : sortedProductIds) {
            SanPham sp = sanPhamRepository.findByIdWithLock(spId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + spId));
            lockedProductMap.put(spId, sp);
        }

        // Preserve writes already made by this transaction before refreshing cached lots.
        entityManager.flush();
        List<LotAllocation> plannedAllocations = new ArrayList<>();
        Map<Integer, Integer> plannedByLot = new HashMap<>();
        boolean hasInsufficientStock = false;
        StringBuilder errorMessageBuilder = new StringBuilder();

        for (OrderItemRequest req : itemRequests) {
            SanPhamChiTiet repSpct = repSpctMap.get(req.getRepresentativeSpctId());
            Integer productId = repSpct.getSanPham().getId();

            List<SanPhamChiTiet> candidates = sanPhamChiTietRepository.findActiveCandidatesBySanPhamId(productId);
            // A candidate may have been loaded before waiting for the parent lock.
            candidates.forEach(entityManager::refresh);
            // Compare attributes from the same refreshed snapshot, including the representative.
            String targetAttrKey = buildAttributeKey(repSpct);
            List<SanPhamChiTiet> matchingCandidates = candidates.stream()
                    .filter(c -> buildAttributeKey(c).equals(targetAttrKey))
                    .sorted(Comparator.comparing(SanPhamChiTiet::getId))
                    .collect(Collectors.toList());

            int totalAvailableStock = matchingCandidates.stream()
                    .mapToInt(c -> (c.getSoLuongTon() != null ? c.getSoLuongTon() : 0) - plannedByLot.getOrDefault(c.getId(), 0))
                    .sum();

            if (totalAvailableStock < req.getQuantity()) {
                hasInsufficientStock = true;
                errorMessageBuilder.append(String.format("Sản phẩm '%s' (%s) không đủ tồn kho (Cần: %d, Khả dụng: %d). ",
                        repSpct.getSanPham().getTenSanPham(), targetAttrKey, req.getQuantity(), totalAvailableStock));
                continue;
            }

            int remainingNeeded = req.getQuantity();
            for (SanPhamChiTiet candidate : matchingCandidates) {
                if (remainingNeeded <= 0) break;
                int currentStock = (candidate.getSoLuongTon() != null ? candidate.getSoLuongTon() : 0)
                        - plannedByLot.getOrDefault(candidate.getId(), 0);
                if (currentStock <= 0) continue;

                int allocateQty = Math.min(currentStock, remainingNeeded);
                plannedAllocations.add(new LotAllocation(
                        req.getSourceLineId(),
                        req.getRepresentativeSpctId(),
                        candidate,
                        allocateQty
                ));
                remainingNeeded -= allocateQty;
                plannedByLot.merge(candidate.getId(), allocateQty, Integer::sum);
            }
        }

        if (hasInsufficientStock) {
            log.warn("[InventoryLotService] Phân bổ FIFO thất bại do thiếu kho: {}", errorMessageBuilder.toString());
            return new AllocationResult(AllocationStatus.INSUFFICIENT_STOCK, Collections.emptyList(), errorMessageBuilder.toString().trim());
        }

        for (LotAllocation alloc : plannedAllocations) {
            SanPhamChiTiet spct = alloc.allocatedSpct();
            int newStock = spct.getSoLuongTon() - alloc.quantityAllocated();
            spct.setSoLuongTon(newStock);
            sanPhamChiTietRepository.save(spct);
        }

        log.info("[InventoryLotService] Phân bổ FIFO thành công cho {} dòng hàng", itemRequests.size());
        return new AllocationResult(AllocationStatus.SUCCESS, plannedAllocations, "Phân bổ tồn kho FIFO thành công");
    }

    @Transactional
    public void hoanKho(List<RestockItemRequest> requests) {
        hoanKhoHangLoat(requests);
    }

    @Transactional
    public void hoanKhoHangLoat(List<RestockItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        List<RestockItemRequest> validRequests = requests.stream()
                .filter(r -> r.isConBanDuoc() && r.getQuantityToRestock() > 0 && r.getIdSanPhamChiTiet() != null)
                .collect(Collectors.toList());

        if (validRequests.isEmpty()) return;

        Map<Integer, SanPhamChiTiet> spctMap = new HashMap<>();
        for (RestockItemRequest req : validRequests) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(req.getIdSanPhamChiTiet())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SPCT ID: " + req.getIdSanPhamChiTiet()));
            spctMap.put(req.getIdSanPhamChiTiet(), spct);
        }

        List<Integer> sortedProductIds = spctMap.values().stream()
                .map(spct -> spct.getSanPham().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (Integer spId : sortedProductIds) {
            sanPhamRepository.findByIdWithLock(spId);
        }

        // Refresh once under the parent locks, before adding any returned quantities.
        entityManager.flush();
        spctMap.values().forEach(entityManager::refresh);

        for (RestockItemRequest req : validRequests) {
            SanPhamChiTiet spct = spctMap.get(req.getIdSanPhamChiTiet());
            int oldStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            spct.setSoLuongTon(oldStock + req.getQuantityToRestock());
            sanPhamChiTietRepository.save(spct);
            log.info("[InventoryLotService] Hoàn kho SPCT #{}: {} -> {}", spct.getId(), oldStock, spct.getSoLuongTon());
        }
    }

    /**
     * Nhập hàng cho biến thể: Tạo PhieuNhap + PhieuNhapChiTiet và tính lại Giá vốn bình quân (Weighted Average Cost)
     */
    @Transactional
    public SanPhamChiTiet nhapLoMoi(Integer representativeSpctId, int soLuongNhap, BigDecimal giaNhap, Integer idNguoiDung) {
        return nhapLoMoi(representativeSpctId, soLuongNhap, giaNhap, idNguoiDung, null);
    }

    @Transactional
    public SanPhamChiTiet nhapLoMoi(Integer representativeSpctId, int soLuongNhap, BigDecimal giaNhap, Integer idNguoiDung, String ghiChu) {
        if (soLuongNhap <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        if (giaNhap == null || giaNhap.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá nhập phải lớn hơn 0");
        }

        SanPhamChiTiet targetSpct = sanPhamChiTietRepository.findById(representativeSpctId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể ID: " + representativeSpctId));

        SanPham sanPham = targetSpct.getSanPham();
        sanPhamRepository.findByIdWithLock(sanPham.getId());

        // Lấy NhanVien
        NhanVien nhanVien = null;
        if (idNguoiDung != null) {
            nhanVien = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
        }
        if (nhanVien == null) {
            List<NhanVien> allNv = nhanVienRepository.findAll();
            if (!allNv.isEmpty()) {
                nhanVien = allNv.get(0);
            }
        }
        if (nhanVien == null) {
            throw new IllegalStateException("Không tìm thấy nhân viên thực hiện nhập hàng");
        }

        LocalDateTime now = LocalDateTime.now();
        String maPN = generateMaPhieuNhap();

        BigDecimal thanhTien = giaNhap.multiply(BigDecimal.valueOf(soLuongNhap));

        PhieuNhap phieuNhap = PhieuNhap.builder()
                .maPhieuNhap(maPN)
                .nhanVien(nhanVien)
                .ngayNhap(now)
                .tongTien(thanhTien)
                .ghiChu((ghiChu != null && !ghiChu.isBlank()) ? ghiChu : ("Nhập hàng cho SP " + sanPham.getTenSanPham()))
                .ngayTao(now)
                .ngayCapNhat(now)
                .build();

        PhieuNhapChiTiet pnct = PhieuNhapChiTiet.builder()
                .phieuNhap(phieuNhap)
                .sanPhamChiTiet(targetSpct)
                .soLuong(soLuongNhap)
                .giaNhap(giaNhap)
                .thanhTien(thanhTien)
                .build();

        phieuNhap.getChiTietList().add(pnct);
        PhieuNhap savedPN = phieuNhapRepository.save(phieuNhap);

        // Tính Giá Vốn Bình Quân Gia Quyền (Weighted Average Purchase Cost)
        int tonCu = targetSpct.getSoLuongTon() != null ? targetSpct.getSoLuongTon() : 0;
        int tonMoi = tonCu + soLuongNhap;
        BigDecimal giaVonCu = targetSpct.getGiaNhap() != null ? targetSpct.getGiaNhap() : BigDecimal.ZERO;

        BigDecimal giaVonMoi;
        if (tonCu > 0 && giaVonCu.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalOldCost = giaVonCu.multiply(BigDecimal.valueOf(tonCu));
            BigDecimal totalNewCost = giaNhap.multiply(BigDecimal.valueOf(soLuongNhap));
            giaVonMoi = totalOldCost.add(totalNewCost).divide(BigDecimal.valueOf(tonMoi), 2, java.math.RoundingMode.HALF_UP);
        } else {
            giaVonMoi = giaNhap;
        }

        targetSpct.setSoLuongTon(tonMoi);
        targetSpct.setGiaNhap(giaVonMoi);
        targetSpct.setNgayCapNhat(now);

        SanPhamChiTiet savedSpct = sanPhamChiTietRepository.save(targetSpct);

        if (idNguoiDung != null) {
            String note = String.format("Nhập hàng [%s] cho biến thể #%d SP '%s': Thêm SL=%d (Tồn cũ=%d -> Tồn mới=%d), Giá nhập=%s, Giá vốn bình quân mới=%s",
                    maPN, representativeSpctId, sanPham.getTenSanPham(), soLuongNhap, tonCu, tonMoi, giaNhap, giaVonMoi);
            auditService.log(idNguoiDung, "SanPhamChiTiet", representativeSpctId.longValue(), "UPDATE",
                    String.valueOf(tonCu), String.valueOf(tonMoi), "127.0.0.1", note, "ADMIN");
        }

        log.info("[InventoryLotService] Đã nhập hàng [{}] SPCT #{}: SL nhập={}, Giá nhập={}, Tồn cũ {} -> {}, Giá vốn cũ {} -> mới {}",
                maPN, savedSpct.getId(), soLuongNhap, giaNhap, tonCu, tonMoi, giaVonCu, giaVonMoi);

        return savedSpct;
    }

    /**
     * Lấy lịch sử các đợt nhập hàng theo PhieuNhapChiTiet cho 1 biến thể cụ thể
     */
    @Transactional(readOnly = true)
    public List<PhieuNhapChiTietDTO> getLichSuPhieuNhapBySpct(Integer idSpct) {
        if (idSpct == null) return Collections.emptyList();

        List<PhieuNhapChiTiet> list = phieuNhapChiTietRepository.findBySpctIdWithReceiptDetails(idSpct);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<PhieuNhapChiTietDTO> dtos = new ArrayList<>();

        for (PhieuNhapChiTiet pnct : list) {
            PhieuNhap pn = pnct.getPhieuNhap();
            SanPhamChiTiet spct = pnct.getSanPhamChiTiet();
            String phanLoai = spct != null ? spct.getPhanLoaiHienThi() : "";
            String nhanVienName = (pn != null && pn.getNhanVien() != null) ? pn.getNhanVien().getHoTen() : "N/A";

            PhieuNhapChiTietDTO dto = PhieuNhapChiTietDTO.builder()
                    .id(pnct.getId())
                    .idPhieuNhap(pn != null ? pn.getId() : null)
                    .maPhieuNhap(pn != null ? pn.getMaPhieuNhap() : "N/A")
                    .ngayNhap(pn != null ? pn.getNgayNhap() : null)
                    .ngayNhapHienThi(pn != null && pn.getNgayNhap() != null ? pn.getNgayNhap().format(dtf) : "N/A")
                    .idSpct(idSpct)
                    .phanLoaiHienThi(phanLoai)
                    .soLuongNhap(pnct.getSoLuong())
                    .giaNhap(pnct.getGiaNhap())
                    .thanhTien(pnct.getThanhTien())
                    .tenNhanVien(nhanVienName)
                    .ghiChu(pn != null ? pn.getGhiChu() : "")
                    .build();
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Lấy tổng quan (Summary) về tồn kho và lịch sử nhập hàng cho 1 biến thể cụ thể
     */
    @Transactional(readOnly = true)
    public BienTheImportSummaryDTO getSummaryBySpct(Integer idSpct) {
        if (idSpct == null) return new BienTheImportSummaryDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);

        Optional<SanPhamChiTiet> spctOpt = sanPhamChiTietRepository.findById(idSpct);
        if (spctOpt.isEmpty()) return new BienTheImportSummaryDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);

        SanPhamChiTiet spct = spctOpt.get();
        Long totalImported = phieuNhapChiTietRepository.sumSoLuongNhapBySpctId(idSpct);
        Long countImportTimes = phieuNhapChiTietRepository.countSoLanNhapBySpctId(idSpct);

        int currentStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
        long sumImported = (totalImported != null && totalImported > 0) ? totalImported : (long) currentStock;
        long importTimes = (countImportTimes != null && countImportTimes > 0) ? countImportTimes : (currentStock > 0 ? 1L : 0L);

        return BienTheImportSummaryDTO.builder()
                .tonKhoHienTai(currentStock)
                .giaVonBinhQuan(spct.getGiaNhap() != null ? spct.getGiaNhap() : BigDecimal.ZERO)
                .giaBanHienTai(spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO)
                .tongSoLuongTungNhap(sumImported)
                .soLanNhapHang(importTimes)
                .build();
    }

    /**
     * Lấy thông tin chi tiết một phiếu nhập (dùng cho Modal xem chi tiết phiếu nhập)
     */
    @Transactional(readOnly = true)
    public PhieuNhapDetailDTO getPhieuNhapDetail(Integer idPhieuNhap) {
        PhieuNhap pn = phieuNhapRepository.findById(idPhieuNhap)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhập ID: " + idPhieuNhap));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String nvName = (pn.getNhanVien() != null) ? pn.getNhanVien().getHoTen() : "N/A";

        List<PhieuNhapDetailDTO.ItemDetail> items = new ArrayList<>();
        if (pn.getChiTietList() != null) {
            for (PhieuNhapChiTiet pnct : pn.getChiTietList()) {
                SanPhamChiTiet spct = pnct.getSanPhamChiTiet();
                String tenSP = (spct != null && spct.getSanPham() != null) ? spct.getSanPham().getTenSanPham() : "";
                String phanLoai = spct != null ? spct.getPhanLoaiHienThi() : "";

                items.add(PhieuNhapDetailDTO.ItemDetail.builder()
                        .idSpct(spct != null ? spct.getId() : null)
                        .tenSanPham(tenSP)
                        .phanLoaiHienThi(phanLoai)
                        .soLuong(pnct.getSoLuong())
                        .giaNhap(pnct.getGiaNhap())
                        .thanhTien(pnct.getThanhTien())
                        .build());
            }
        }

        return PhieuNhapDetailDTO.builder()
                .id(pn.getId())
                .maPhieuNhap(pn.getMaPhieuNhap())
                .ngayNhap(pn.getNgayNhap())
                .ngayNhapHienThi(pn.getNgayNhap() != null ? pn.getNgayNhap().format(dtf) : "N/A")
                .tenNhanVien(nvName)
                .ghiChu(pn.getGhiChu())
                .tongTien(pn.getTongTien())
                .chiTietList(items)
                .build();
    }

    /**
     * Tính toán gom nhóm biến thể theo AttributeKey cho 1 Sản Phẩm
     */
    @Transactional(readOnly = true)
    public List<VariantGroupDTO> calculateAggregatedVariants(Integer idSanPham) {
        List<SanPhamChiTiet> allSpcts = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        if (allSpcts.isEmpty()) return Collections.emptyList();

        Map<String, List<SanPhamChiTiet>> grouped = allSpcts.stream()
                .collect(Collectors.groupingBy(this::buildAttributeKey));

        List<VariantGroupDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<SanPhamChiTiet>> entry : grouped.entrySet()) {
            String attrKey = entry.getKey();
            List<SanPhamChiTiet> groupSpcts = entry.getValue();

            // Tìm minSpctId làm đại diện
            SanPhamChiTiet repSpct = groupSpcts.stream()
                    .min(Comparator.comparing(SanPhamChiTiet::getId))
                    .orElse(groupSpcts.get(0));

            int totalStock = groupSpcts.stream()
                    .mapToInt(s -> s.getSoLuongTon() != null ? s.getSoLuongTon() : 0)
                    .sum();

            // Tìm ảnh fallback
            String mainImageUrl = findFallbackMainImage(groupSpcts);

            VariantGroupDTO groupDto = VariantGroupDTO.builder()
                    .attributeKey(attrKey)
                    .displayTitle(getDisplayTitle(repSpct))
                    .representativeSpctId(repSpct.getId())
                    .representativeSpct(repSpct)
                    .giaBan(repSpct.getGiaBan())
                    .trangThai(repSpct.getTrangThai())
                    .isDangBan(repSpct.getTrangThaiValue() != null && repSpct.getTrangThaiValue())
                    .tongSoLuongTon(totalStock)
                    .soLuongLoActive(groupSpcts.size())
                    .hinhAnhUrl(mainImageUrl)
                    .danhSachSpctLo(groupSpcts)
                    .build();

            result.add(groupDto);
        }

        result.sort(Comparator.comparing(VariantGroupDTO::getRepresentativeSpctId));
        return result;
    }

    /**
     * Tính toán thông tin Tab Lô Hàng (Legacy Lot vs New Lots)
     */
    @Transactional(readOnly = true)
    public List<LoHangDTO> calculateLotSummaries(Integer idSanPham) {
        List<SanPhamChiTiet> allSpcts = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        if (allSpcts.isEmpty()) return Collections.emptyList();

        LocalDateTime enabledFromThreshold;
        try {
            enabledFromThreshold = LocalDateTime.parse(enabledFromStr);
        } catch (Exception e) {
            enabledFromThreshold = LocalDateTime.of(2026, 8, 7, 8, 30);
        }

        Map<String, List<SanPhamChiTiet>> lotGrouped = new LinkedHashMap<>();

        for (SanPhamChiTiet spct : allSpcts) {
            LocalDateTime ngayTao = spct.getNgayTao();
            String lotGroupKey;
            if (ngayTao == null || ngayTao.isBefore(enabledFromThreshold)) {
                lotGroupKey = "LEGACY_LOT";
            } else {
                lotGroupKey = ngayTao.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
            lotGrouped.computeIfAbsent(lotGroupKey, k -> new ArrayList<>()).add(spct);
        }

        List<LoHangDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<SanPhamChiTiet>> entry : lotGrouped.entrySet()) {
            String lotKey = entry.getKey();
            List<SanPhamChiTiet> lotSpcts = entry.getValue();

            boolean isLegacy = "LEGACY_LOT".equals(lotKey);
            int minSpctId = lotSpcts.stream().mapToInt(SanPhamChiTiet::getId).min().orElse(0);

            LocalDateTime ngayTaoLo = (lotSpcts != null && !lotSpcts.isEmpty()) ? lotSpcts.get(0).getNgayTao() : null;
            String maLoDisplay = isLegacy ? "LÔ KHỞI TẠO (KHO BAN ĐẦU)" :

                    String.format("LO-%d-%s-%d", idSanPham, lotKey, minSpctId);
            String thoiGianHienThi = ngayTaoLo != null ? ngayTaoLo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";


            int totalVariants = (int) lotSpcts.stream().map(this::buildAttributeKey).distinct().count();
            int inStockVariants = (int) lotSpcts.stream()
                    .filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() > 0)
                    .map(this::buildAttributeKey)
                    .distinct().count();

            int totalStock = lotSpcts.stream()
                    .mapToInt(s -> s.getSoLuongTon() != null ? s.getSoLuongTon() : 0)
                    .sum();

            BigDecimal totalCapitalValue = BigDecimal.ZERO;
            boolean isUncertain = false;
            for (SanPhamChiTiet s : lotSpcts) {
                if (s.getGiaNhap() != null && s.getSoLuongTon() != null) {
                    totalCapitalValue = totalCapitalValue.add(s.getGiaNhap().multiply(new BigDecimal(s.getSoLuongTon())));
                } else if (s.getSoLuongTon() != null && s.getSoLuongTon() > 0) {
                    isUncertain = true;
                }
            }

            LoHangDTO dto = LoHangDTO.builder()
                    .maLo(maLoDisplay)
                    .ngayTao(ngayTaoLo)
                    .thoiGianHienThi(thoiGianHienThi)
                    .isLegacyLot(isLegacy)
                    .tongSoBienThe(totalVariants)
                    .soBienTheConHang(inStockVariants)
                    .tongSoLuongTonHienTai(totalStock)
                    .tongGiaTriVonHienTai(totalCapitalValue)
                    .coGiaNhapUncertain(isUncertain)
                    .danhSachSpctInLot(lotSpcts)
                    .build();

            result.add(dto);
        }

        return result;
    }

    /**
     * Lấy lịch sử nhập hàng thực tế từ PhieuNhapChiTiet hoặc EditLog audit trail cho sản phẩm
     */
    @Transactional(readOnly = true)
    public List<LichSuNhapHangDTO> getLichSuNhapHang(Integer idSanPham) {
        List<SanPhamChiTiet> spcts = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        if (spcts.isEmpty()) return Collections.emptyList();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // 1. Ưu tiên lấy dữ liệu từ PhieuNhapChiTiet
        List<PhieuNhapChiTiet> pncts = phieuNhapChiTietRepository.findBySanPhamIdWithReceiptDetails(idSanPham);
        if (!pncts.isEmpty()) {
            List<LichSuNhapHangDTO> result = new ArrayList<>();
            for (PhieuNhapChiTiet pnct : pncts) {
                PhieuNhap pn = pnct.getPhieuNhap();
                SanPhamChiTiet spct = pnct.getSanPhamChiTiet();
                String phanLoai = spct != null ? spct.getPhanLoaiHienThi() : "";
                String nvName = (pn != null && pn.getNhanVien() != null) ? pn.getNhanVien().getHoTen() : "Admin";

                LichSuNhapHangDTO dto = LichSuNhapHangDTO.builder()
                        .id(pnct.getId())
                        .idPhieuNhap(pn != null ? pn.getId() : null)
                        .maPhieuNhap(pn != null ? pn.getMaPhieuNhap() : "PN-LEGACY")
                        .idSpct(spct != null ? spct.getId() : null)
                        .phanLoaiHienThi(phanLoai)
                        .thoiGianNhap(pn != null ? pn.getNgayNhap() : null)
                        .thoiGianHienThi((pn != null && pn.getNgayNhap() != null) ? pn.getNgayNhap().format(dtf) : "N/A")
                        .soLuongNhap(pnct.getSoLuong())
                        .giaNhap(pnct.getGiaNhap())
                        .nguoiThucHien(nvName)
                        .ghiChu(pn != null ? pn.getGhiChu() : "")
                        .build();
                result.add(dto);
            }
            return result;
        }

        // 2. Fallback: Parse dữ liệu từ EditLog
        Map<Integer, SanPhamChiTiet> spctMap = spcts.stream()
                .collect(Collectors.toMap(SanPhamChiTiet::getId, s -> s, (s1, s2) -> s1));
        List<Integer> spctIds = new ArrayList<>(spctMap.keySet());

        List<EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiInOrderByThoiGianDesc("SanPhamChiTiet", spctIds);
        List<LichSuNhapHangDTO> result = new ArrayList<>();

        for (EditLog logItem : logs) {
            SanPhamChiTiet spct = spctMap.get(logItem.getIdBanGhi());
            if (spct == null) continue;

            String phanLoai = spct.getPhanLoaiHienThi();
            String ghiChu = logItem.getGhiChu() != null ? logItem.getGhiChu() : "";

            int soLuongNhap = 0;
            Integer tonCu = null;
            Integer tonMoi = null;
            BigDecimal giaNhap = spct.getGiaNhap();
            String maPN = "PN-LEGACY";

            if (ghiChu.contains("[") && ghiChu.contains("]")) {
                try {
                    String extracted = ghiChu.substring(ghiChu.indexOf("[") + 1, ghiChu.indexOf("]")).trim();
                    if (extracted.startsWith("PN")) {
                        maPN = extracted;
                    }
                } catch (Exception ignored) {}
            }

            try {
                if (logItem.getGiaTriCu() != null && !logItem.getGiaTriCu().isBlank()
                        && logItem.getGiaTriMoi() != null && !logItem.getGiaTriMoi().isBlank()) {
                    tonCu = Integer.parseInt(logItem.getGiaTriCu().trim());
                    tonMoi = Integer.parseInt(logItem.getGiaTriMoi().trim());
                    soLuongNhap = Math.max(0, tonMoi - tonCu);
                }
            } catch (Exception ignored) {}

            if (ghiChu.contains("Giá nhập=")) {
                try {
                    String sub = ghiChu.substring(ghiChu.indexOf("Giá nhập=") + 9).trim();
                    if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(",")).trim();
                    if (sub.contains(" ")) sub = sub.substring(0, sub.indexOf(" ")).trim();
                    if (!sub.isEmpty() && !sub.equalsIgnoreCase("null")) {
                        giaNhap = new BigDecimal(sub);
                    }
                } catch (Exception ignored) {}
            }

            if (soLuongNhap <= 0 && ghiChu.contains("SL=")) {
                try {
                    String sub = ghiChu.substring(ghiChu.indexOf("SL=") + 3);
                    if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
                    if (sub.contains(" ")) sub = sub.substring(0, sub.indexOf(" "));
                    soLuongNhap = Integer.parseInt(sub.trim());
                } catch (Exception ignored) {}
            }

            String nguoiThucHien = logItem.getTaiKhoan() != null ? logItem.getTaiKhoan().getUsername() : "Admin";

            LichSuNhapHangDTO dto = LichSuNhapHangDTO.builder()
                    .id(logItem.getId())
                    .idPhieuNhap(null)
                    .maPhieuNhap(maPN)
                    .idSpct(spct.getId())
                    .phanLoaiHienThi(phanLoai)
                    .thoiGianNhap(logItem.getThoiGian())
                    .thoiGianHienThi(logItem.getThoiGian() != null ? logItem.getThoiGian().format(dtf) : "N/A")
                    .soLuongNhap(soLuongNhap > 0 ? soLuongNhap : (tonMoi != null ? tonMoi : (spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)))
                    .tonCu(tonCu)
                    .tonMoi(tonMoi)
                    .giaNhap(giaNhap)
                    .nguoiThucHien(nguoiThucHien)
                    .ghiChu(ghiChu)
                    .build();

            result.add(dto);
        }

        if (result.isEmpty()) {
            for (SanPhamChiTiet spct : spcts) {
                LocalDateTime time = spct.getNgayTao() != null ? spct.getNgayTao() : LocalDateTime.now();
                LichSuNhapHangDTO dto = LichSuNhapHangDTO.builder()
                        .id(spct.getId())
                        .idPhieuNhap(null)
                        .maPhieuNhap("PN-LEGACY")
                        .idSpct(spct.getId())
                        .phanLoaiHienThi(spct.getPhanLoaiHienThi())
                        .thoiGianNhap(time)
                        .thoiGianHienThi(time.format(dtf))
                        .soLuongNhap(spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                        .tonCu(0)
                        .tonMoi(spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0)
                        .giaNhap(spct.getGiaNhap())
                        .nguoiThucHien("Hệ thống")
                        .ghiChu("Khởi tạo ban đầu")
                        .build();
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * Sinh AttributeKey chuẩn hóa từ danh sách thuộc tính của SPCT
     */
    public String buildAttributeKey(SanPhamChiTiet spct) {
        if (spct == null) return "DEFAULT";
        Collection<SanPhamChiTietThuocTinh> attrs = spct.getSanPhamChiTietThuocTinhs();
        if (attrs == null || attrs.isEmpty()) return "DEFAULT";

        com.smashvn.shop.constant.CategoryType catType = (spct.getSanPham() != null && spct.getSanPham().getDanhMuc() != null)
                ? com.smashvn.shop.constant.CategoryType.fromDanhMuc(spct.getSanPham().getDanhMuc())
                : com.smashvn.shop.constant.CategoryType.OTHER;

        List<SanPhamChiTietThuocTinh> validAttrs = attrs.stream()
                .filter(a -> a.getThuocTinh() != null && a.getGiaTri() != null && !a.getGiaTri().isBlank())
                .filter(a -> {
                    String tenTT = a.getThuocTinh().getTenThuocTinh().trim().toLowerCase();
                    if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC || catType == com.smashvn.shop.constant.CategoryType.GIAY) {
                        if (tenTT.contains("căng") || tenTT.contains("trọng lượng") || tenTT.contains("weight")) return false;
                    } else if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        if (tenTT.contains("kích thước") || tenTT.contains("size")) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(a -> a.getThuocTinh().getTenThuocTinh() != null ? a.getThuocTinh().getTenThuocTinh() : ""))
                .collect(Collectors.toList());

        if (validAttrs.isEmpty()) return "DEFAULT";

        return validAttrs.stream()
                .map(a -> a.getThuocTinh().getTenThuocTinh() + "=" + a.getGiaTri())
                .collect(Collectors.joining("|"));
    }

    /**
     * Lấy tiêu đề hiển thị cho SPCT dựa trên các thuộc tính
     */
    public String getDisplayTitle(SanPhamChiTiet spct) {
        if (spct == null) return "";
        Collection<SanPhamChiTietThuocTinh> attrs = spct.getSanPhamChiTietThuocTinhs();
        if (attrs == null || attrs.isEmpty()) return spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "";

        com.smashvn.shop.constant.CategoryType catType = (spct.getSanPham() != null && spct.getSanPham().getDanhMuc() != null)
                ? com.smashvn.shop.constant.CategoryType.fromDanhMuc(spct.getSanPham().getDanhMuc())
                : com.smashvn.shop.constant.CategoryType.OTHER;

        List<String> values = attrs.stream()
                .filter(a -> a.getThuocTinh() != null && a.getGiaTri() != null && !a.getGiaTri().isBlank())
                .filter(a -> {
                    String tenTT = a.getThuocTinh().getTenThuocTinh().trim().toLowerCase();
                    if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC || catType == com.smashvn.shop.constant.CategoryType.GIAY) {
                        if (tenTT.contains("căng") || tenTT.contains("trọng lượng") || tenTT.contains("weight")) return false;
                    } else if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        if (tenTT.contains("kích thước") || tenTT.contains("size")) return false;
                    }
                    return true;
                })
                .map(a -> a.getGiaTri() != null ? a.getGiaTri() : "")
                .collect(Collectors.toList());

        if (values.isEmpty()) return spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "";

        return String.join(" - ", values);
    }



    /**
     * Tim ảnh đại diện fallback cho nhóm SPCT
     */
    private String findFallbackMainImage(List<SanPhamChiTiet> spcts) {
        for (SanPhamChiTiet s : spcts) {
            if (s.getHinhAnhSanPhams() != null && !s.getHinhAnhSanPhams().isEmpty()) {
                for (HinhAnhSanPham ha : s.getHinhAnhSanPhams()) {
                    if (ha.getLaAnhChinh() != null && ha.getLaAnhChinh()) {
                        return ha.getUrlHinhAnh();
                    }
                }
                return s.getHinhAnhSanPhams().get(0).getUrlHinhAnh();
            }
        }
        return "/images/default-product.png";
    }

    // ── PHASE 1: KHO SAN PHAM LOI ────────────────────────────────────────────

    /**
     * Lay danh sach bien the san pham co hang loi (soLuongSpLoi > 0).
     * Chi doc du lieu – khong chuong trinh DB, khong query HoaDon, khong query EditLog.
     * Ket qua duoc sap xep giam dan theo soLuongSpLoi (nhieu loi nhat hien truoc).
     */
    public List<KhoSanPhamLoiView> layDanhSachKhoSanPhamLoi() {
        List<SanPhamChiTiet> danhSach = sanPhamChiTietRepository
                .findBySoLuongSpLoiGreaterThanOrderBySoLuongSpLoiDesc(0);

        List<KhoSanPhamLoiView> result = new ArrayList<>();
        for (SanPhamChiTiet spct : danhSach) {
            if (spct == null) continue;

            Integer idSanPham = (spct.getSanPham() != null) ? spct.getSanPham().getId() : null;
            String tenSanPham = (spct.getSanPham() != null && spct.getSanPham().getTenSanPham() != null)
                    ? spct.getSanPham().getTenSanPham()
                    : "—";

            String phanLoai;
            try {
                phanLoai = spct.getPhanLoaiHienThi();
            } catch (Exception e) {
                phanLoai = "—";
            }

            String hinhAnh;
            try {
                hinhAnh = spct.getHinhAnhUrl();
            } catch (Exception e) {
                hinhAnh = "/images/placeholder.png";
            }

            result.add(new KhoSanPhamLoiView(
                    spct.getId(),
                    idSanPham,
                    tenSanPham,
                    phanLoai,
                    hinhAnh,
                    spct.getSoLuongTon(),
                    spct.getSoLuongSpLoi()
            ));
        }
        return result;
    }

    // ── PHASE 2: CHI TIET KHO SAN PHAM LOI ───────────────────────────────────

    /**
     * Lay chi tiet kho san pham loi cua 1 bien the, bao gom cac don hang nguon da chuyen vao kho loi.
     * Chi doc du lieu – khong ghi DB, khong sua soLuongSpLoi, khong sua don hang.
     */
    @Transactional(readOnly = true)
    public KhoSanPhamLoiDetailView layChiTietKhoSanPhamLoi(Integer idSanPhamChiTiet) {
        if (idSanPhamChiTiet == null) {
            return null;
        }

        Optional<SanPhamChiTiet> spctOpt = sanPhamChiTietRepository.findById(idSanPhamChiTiet);
        if (spctOpt.isEmpty()) {
            return null;
        }
        SanPhamChiTiet spct = spctOpt.get();

        Integer idSanPham = (spct.getSanPham() != null) ? spct.getSanPham().getId() : null;
        String tenSanPham = (spct.getSanPham() != null && spct.getSanPham().getTenSanPham() != null)
                ? spct.getSanPham().getTenSanPham()
                : "—";

        String phanLoai;
        try {
            phanLoai = spct.getPhanLoaiHienThi();
        } catch (Exception e) {
            phanLoai = "—";
        }

        String hinhAnh;
        try {
            hinhAnh = spct.getHinhAnhUrl();
        } catch (Exception e) {
            hinhAnh = "/images/placeholder.png";
        }

        // 1. Query danh sach HoaDonChiTiet lien quan (da chuyen kho loi)
        List<HoaDonChiTiet> hdcts = hoaDonChiTietRepository.findKhoLoiSources(idSanPhamChiTiet);

        // 2. Group theo HoaDon de tinh tong soLuongDaChuyen cho tung don (tranh duplicate record khi cung 1 SPCT co nhieu dong trong 1 don)
        Map<Integer, Integer> qtyPerHoaDon = new LinkedHashMap<>();
        Map<Integer, HoaDon> hoaDonMap = new LinkedHashMap<>();

        for (HoaDonChiTiet hdct : hdcts) {
            if (hdct == null || hdct.getHoaDon() == null || hdct.getHoaDon().getId() == null) continue;
            Integer hdId = hdct.getHoaDon().getId();
            int qty = (hdct.getSoLuong() != null) ? hdct.getSoLuong() : 0;
            qtyPerHoaDon.put(hdId, qtyPerHoaDon.getOrDefault(hdId, 0) + qty);
            hoaDonMap.putIfAbsent(hdId, hdct.getHoaDon());
        }

        List<Integer> hoaDonIds = new ArrayList<>(hoaDonMap.keySet());

        // 3. Batch query EditLogs cho cac hoa don nay (chong N+1 query)
        Map<Integer, EditLog> latestLogMap = new HashMap<>();
        if (!hoaDonIds.isEmpty()) {
            List<EditLog> logs = editLogRepository.findKiemHangLoiLogsBatch(hoaDonIds);
            for (EditLog logItem : logs) {
                if (logItem != null && logItem.getIdBanGhi() != null && !latestLogMap.containsKey(logItem.getIdBanGhi())) {
                    latestLogMap.put(logItem.getIdBanGhi(), logItem);
                }
            }
        }

        // 4. Batch lookup ten NhanVien tu TaiKhoan (chong N+1 query)
        Set<Integer> taiKhoanIds = new HashSet<>();
        for (EditLog logItem : latestLogMap.values()) {
            if (logItem.getTaiKhoan() != null && logItem.getTaiKhoan().getId() != null) {
                taiKhoanIds.add(logItem.getTaiKhoan().getId());
            }
        }
        Map<Integer, String> staffNameMap = new HashMap<>();
        for (Integer tkId : taiKhoanIds) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(tkId);
            if (nv != null && nv.getHoTenNv() != null && !nv.getHoTenNv().isBlank()) {
                staffNameMap.put(tkId, nv.getHoTenNv().trim());
            }
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 5. Build SourceViews
        List<KhoSanPhamLoiSourceView> sourceViews = new ArrayList<>();
        for (Map.Entry<Integer, HoaDon> entry : hoaDonMap.entrySet()) {
            Integer hdId = entry.getKey();
            HoaDon hd = entry.getValue();
            int soLuongDaChuyen = qtyPerHoaDon.getOrDefault(hdId, 0);

            // Parse evidence JSON
            List<String> bangChungList = new ArrayList<>();
            String rawEvidence = hd.getBangChungHoanTra();
            if (rawEvidence != null && !rawEvidence.isBlank()) {
                try {
                    if (rawEvidence.trim().startsWith("[")) {
                        List<String> paths = objectMapper.readValue(rawEvidence, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                        if (paths != null) {
                            for (String p : paths) {
                                if (isValidEvidencePath(p)) {
                                    bangChungList.add(normalizeEvidencePath(p));
                                }
                            }
                        }
                    } else if (isValidEvidencePath(rawEvidence)) {
                        bangChungList.add(normalizeEvidencePath(rawEvidence));
                    }
                } catch (Exception e) {
                    log.warn("Loi parse bangChungHoanTra JSON don #{}: {}", hdId, e.getMessage());
                    if (isValidEvidencePath(rawEvidence)) {
                        bangChungList.add(normalizeEvidencePath(rawEvidence));
                    }
                }
            }

            // Reason
            String lyDo = (hd.getLyDoHoanTra() != null && !hd.getLyDoHoanTra().isBlank())
                    ? hd.getLyDoHoanTra().trim()
                    : "Không có thông tin lý do hoàn trả.";

            // Request Type
            String loaiYeuCau;
            if ("TRA".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra())) {
                loaiYeuCau = "Trả hàng";
            } else if ("DOI".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra())) {
                loaiYeuCau = "Đổi hàng";
            } else if (hd.getLoaiYeuCauDoiTra() != null && !hd.getLoaiYeuCauDoiTra().isBlank()) {
                loaiYeuCau = hd.getLoaiYeuCauDoiTra();
            } else {
                loaiYeuCau = "Trả hàng";
            }

            // Return Status labels
            String trangThaiHoanHangLabel = (hd.getTrangThaiHoanHang() != null)
                    ? hd.getTrangThaiHoanHang().getLabel()
                    : "—";
            String trangThaiXuLyHangHoanLabel = (hd.getTrangThaiXuLyHangHoan() != null)
                    ? hd.getTrangThaiXuLyHangHoan().getLabel()
                    : "—";

            // EditLog / Processor info
            EditLog editLog = latestLogMap.get(hdId);
            String nguoiXuLy = "Không xác định";
            String vaiTroNguoiXuLy = "Không xác định";
            LocalDateTime thoiGianXuLy = null;
            String thoiGianXuLyFormatted = "Không xác định";

            if (editLog != null) {
                thoiGianXuLy = editLog.getThoiGian();
                if (thoiGianXuLy != null) {
                    thoiGianXuLyFormatted = thoiGianXuLy.format(dtf);
                }
                String roleRaw = editLog.getVaiTroThucHien();
                if ("QL".equalsIgnoreCase(roleRaw) || "QUAN_LY".equalsIgnoreCase(roleRaw) || "ROLE_QL".equalsIgnoreCase(roleRaw)) {
                    vaiTroNguoiXuLy = "Quản lý";
                } else if ("NV".equalsIgnoreCase(roleRaw) || "NHAN_VIEN".equalsIgnoreCase(roleRaw) || "ROLE_NV".equalsIgnoreCase(roleRaw)) {
                    vaiTroNguoiXuLy = "Nhân viên";
                } else if ("SYSTEM".equalsIgnoreCase(roleRaw)) {
                    vaiTroNguoiXuLy = "Hệ thống";
                } else if (roleRaw != null && !roleRaw.isBlank()) {
                    vaiTroNguoiXuLy = roleRaw;
                }

                if (editLog.getTaiKhoan() != null) {
                    Integer tkId = editLog.getTaiKhoan().getId();
                    if (staffNameMap.containsKey(tkId)) {
                        nguoiXuLy = staffNameMap.get(tkId);
                    } else if (editLog.getTaiKhoan().getUsername() != null) {
                        nguoiXuLy = editLog.getTaiKhoan().getUsername();
                    }
                } else if ("SYSTEM".equalsIgnoreCase(roleRaw)) {
                    nguoiXuLy = "Hệ thống tự động";
                }
            }

            KhoSanPhamLoiSourceView sourceView = new KhoSanPhamLoiSourceView();
            sourceView.setIdHoaDon(hdId);
            sourceView.setMaDonHang(hd.getMaDonHang());
            sourceView.setSoLuongDaChuyen(soLuongDaChuyen);
            sourceView.setLyDoHoanTra(lyDo);
            sourceView.setLoaiYeuCauDoiTra(loaiYeuCau);
            sourceView.setLoaiYeuCauDoiTraRaw(hd.getLoaiYeuCauDoiTra());
            sourceView.setTrangThaiHoanHang(trangThaiHoanHangLabel);
            sourceView.setTrangThaiXuLyHangHoan(trangThaiXuLyHangHoanLabel);
            sourceView.setBangChungList(bangChungList);
            sourceView.setNguoiXuLy(nguoiXuLy);
            sourceView.setVaiTroNguoiXuLy(vaiTroNguoiXuLy);
            sourceView.setThoiGianXuLy(thoiGianXuLy);
            sourceView.setThoiGianXuLyFormatted(thoiGianXuLyFormatted);

            sourceViews.add(sourceView);
        }

        // 6. Query lich su xu ly kho loi (Phase 3)
        List<EditLog> lichSuXuLyLogs = editLogRepository.findLichSuXuLyKhoLoi(idSanPhamChiTiet);
        for (EditLog logItem : lichSuXuLyLogs) {
            if (logItem.getTaiKhoan() != null && logItem.getTaiKhoan().getId() != null) {
                Integer tkId = logItem.getTaiKhoan().getId();
                if (!staffNameMap.containsKey(tkId)) {
                    NhanVien nv = nhanVienRepository.findByTaiKhoanId(tkId);
                    if (nv != null && nv.getHoTenNv() != null && !nv.getHoTenNv().isBlank()) {
                        staffNameMap.put(tkId, nv.getHoTenNv().trim());
                    }
                }
            }
        }

        List<KhoSanPhamLoiLichSuXuLyView> lichSuXuLyViews = new ArrayList<>();
        for (EditLog logItem : lichSuXuLyLogs) {
            String noteRaw = logItem.getGhiChu() != null ? logItem.getGhiChu() : "";
            String hanhDongDisplay = "Xử lý kho lỗi";
            String hanhDongRaw = "XU_LY";
            String badgeClass = "bg-secondary";
            Integer qty = null;
            String noteDisplay = noteRaw;

            if (noteRaw.contains("[KHO_LOI_SUA_XONG_NHAP_LAI_KHO]")) {
                hanhDongDisplay = FaultyInventoryAction.SUA_XONG_NHAP_LAI_KHO.getLabel();
                hanhDongRaw = "SUA_XONG_NHAP_LAI_KHO";
                badgeClass = FaultyInventoryAction.SUA_XONG_NHAP_LAI_KHO.getBadgeClass();
            } else if (noteRaw.contains("[KHO_LOI_TIEU_HUY]")) {
                hanhDongDisplay = FaultyInventoryAction.TIEU_HUY.getLabel();
                hanhDongRaw = "TIEU_HUY";
                badgeClass = FaultyInventoryAction.TIEU_HUY.getBadgeClass();
            } else if (noteRaw.contains("[KHO_LOI_TRA_NCC]")) {
                hanhDongDisplay = FaultyInventoryAction.TRA_NHA_CUNG_CAP.getLabel();
                hanhDongRaw = "TRA_NHA_CUNG_CAP";
                badgeClass = FaultyInventoryAction.TRA_NHA_CUNG_CAP.getBadgeClass();
            }

            // Extract soLuong=X; lyDo=...
            if (noteRaw.contains("soLuong=")) {
                try {
                    int startQty = noteRaw.indexOf("soLuong=") + 8;
                    int endQty = noteRaw.indexOf(";", startQty);
                    if (endQty > startQty) {
                        qty = Integer.parseInt(noteRaw.substring(startQty, endQty).trim());
                    } else {
                        qty = Integer.parseInt(noteRaw.substring(startQty).trim());
                    }
                } catch (Exception ignored) {}
            }

            if (noteRaw.contains("lyDo=")) {
                int startLyDo = noteRaw.indexOf("lyDo=") + 5;
                noteDisplay = noteRaw.substring(startLyDo).trim();
            } else if (noteRaw.contains("]")) {
                noteDisplay = noteRaw.substring(noteRaw.indexOf("]") + 1).trim();
            }

            String performer = "Không xác định";
            if (logItem.getTaiKhoan() != null) {
                Integer tkId = logItem.getTaiKhoan().getId();
                if (staffNameMap.containsKey(tkId)) {
                    performer = staffNameMap.get(tkId);
                } else if (logItem.getTaiKhoan().getUsername() != null) {
                    performer = logItem.getTaiKhoan().getUsername();
                }
            }

            String performerRole = "Không xác định";
            String roleRaw = logItem.getVaiTroThucHien();
            if ("QL".equalsIgnoreCase(roleRaw) || "QUAN_LY".equalsIgnoreCase(roleRaw) || "ROLE_QL".equalsIgnoreCase(roleRaw)) {
                performerRole = "Quản lý";
            } else if ("NV".equalsIgnoreCase(roleRaw) || "NHAN_VIEN".equalsIgnoreCase(roleRaw) || "ROLE_NV".equalsIgnoreCase(roleRaw)) {
                performerRole = "Nhân viên";
            } else if ("SYSTEM".equalsIgnoreCase(roleRaw)) {
                performerRole = "Hệ thống";
            } else if (roleRaw != null && !roleRaw.isBlank()) {
                performerRole = roleRaw;
            }

            String timeFormatted = logItem.getThoiGian() != null ? logItem.getThoiGian().format(dtf) : "—";

            lichSuXuLyViews.add(new KhoSanPhamLoiLichSuXuLyView(
                    logItem.getId(),
                    logItem.getThoiGian(),
                    timeFormatted,
                    hanhDongDisplay,
                    hanhDongRaw,
                    badgeClass,
                    qty,
                    performer,
                    performerRole,
                    noteDisplay
            ));
        }

        return new KhoSanPhamLoiDetailView(
                spct.getId(),
                idSanPham,
                tenSanPham,
                phanLoai,
                hinhAnh,
                spct.getSoLuongTon(),
                spct.getSoLuongSpLoi(),
                sourceViews,
                lichSuXuLyViews
        );
    }

    // ── PHASE 3: XỬ LÝ SẢN PHẨM TRONG KHO LỖI ─────────────────────────────────

    /**
     * Xử lý sản phẩm đang nằm trong kho lỗi:
     * 1. Sửa xong → Nhập lại kho bán (SUA_XONG_NHAP_LAI_KHO)
     * 2. Tiêu hủy (TIEU_HUY - QL ONLY)
     * 3. Trả nhà cung cấp (TRA_NHA_CUNG_CAP)
     *
     * Thực hiện trong Transaction, dùng PESSIMISTIC_WRITE lock, validate số lượng,
     * không cho âm kho, và ghi EditLog đầy đủ.
     */
    @Transactional
    public void xuLySanPhamLoi(
            Integer idSanPhamChiTiet,
            String hanhDongInput,
            Integer soLuong,
            String ghiChu,
            Integer actingTaiKhoanId,
            String vaiTroThucHien,
            String clientIp
    ) {
        if (idSanPhamChiTiet == null) {
            throw new IllegalArgumentException("Mã biến thể sản phẩm không được để trống.");
        }

        // 1. Pessimistic Lock SPCT
        SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(idSanPhamChiTiet)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm có ID: " + idSanPhamChiTiet));

        // Lock parent SanPham để đồng bộ
        if (spct.getSanPham() != null && spct.getSanPham().getId() != null) {
            sanPhamRepository.findByIdWithLock(spct.getSanPham().getId());
        }

        // 2. Validate Action
        FaultyInventoryAction action = FaultyInventoryAction.fromString(hanhDongInput);
        if (action == null) {
            throw new IllegalArgumentException("Hành động xử lý kho lỗi không hợp lệ.");
        }

        // 3. Phân quyền: TIEU_HUY chỉ dành cho QL
        if (action == FaultyInventoryAction.TIEU_HUY) {
            boolean isQL = "QL".equalsIgnoreCase(vaiTroThucHien) || "QUAN_LY".equalsIgnoreCase(vaiTroThucHien) || "ROLE_QL".equalsIgnoreCase(vaiTroThucHien);
            if (!isQL) {
                throw new org.springframework.security.access.AccessDeniedException("Chỉ Quản lý (QL) mới có quyền thực hiện tiêu hủy sản phẩm lỗi.");
            }
        }

        // 4. Validate Số lượng
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng xử lý phải là số nguyên dương lớn hơn 0.");
        }

        int currentFaulty = spct.getSoLuongSpLoi() != null ? spct.getSoLuongSpLoi() : 0;
        if (soLuong > currentFaulty) {
            throw new IllegalArgumentException("Số lượng xử lý không được vượt quá số lượng sản phẩm lỗi hiện có.");
        }

        // 5. Validate Ghi chú
        if (ghiChu == null || ghiChu.trim().isBlank()) {
            throw new IllegalArgumentException("Lý do / Ghi chú xử lý bắt buộc không được để trống.");
        }
        String cleanGhiChu = ghiChu.trim();
        if (cleanGhiChu.length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
        cleanGhiChu = cleanGhiChu.replace("<", "&lt;").replace(">", "&gt;");

        // 6. Thực hiện cập nhật số lượng
        int oldFaulty = currentFaulty;
        int newFaulty = currentFaulty - soLuong;
        if (newFaulty < 0) {
            throw new IllegalStateException("Số lượng sản phẩm lỗi sau xử lý không được âm.");
        }
        spct.setSoLuongSpLoi(newFaulty);

        int oldTon = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
        int newTon = oldTon;

        if (action == FaultyInventoryAction.SUA_XONG_NHAP_LAI_KHO) {
            // Tăng tồn kho bán khả dụng
            newTon = oldTon + soLuong;
            spct.setSoLuongTon(newTon);
            log.info("[InventoryLotService] Sửa xong SPCT #{}: Tồn kho bán {} -> {}, Kho lỗi {} -> {}",
                    spct.getId(), oldTon, newTon, oldFaulty, newFaulty);
        } else {
            log.info("[InventoryLotService] Xử lý kho lỗi [{}] SPCT #{}: Kho lỗi {} -> {}, Tồn kho bán giữ nguyên ({})",
                    action.name(), spct.getId(), oldFaulty, newFaulty, oldTon);
        }

        sanPhamChiTietRepository.save(spct);

        // 7. Ghi Audit Log vào EditLog
        String giaTriCu = "soLuongSpLoi=" + oldFaulty + ", soLuongTon=" + oldTon;
        String giaTriMoi = "soLuongSpLoi=" + newFaulty + ", soLuongTon=" + newTon;
        String fullNote = action.getLogPrefix() + " soLuong=" + soLuong + "; lyDo=" + cleanGhiChu;

        auditService.log(
                actingTaiKhoanId,
                "SanPhamChiTiet",
                spct.getId().longValue(),
                "UPDATE",
                giaTriCu,
                giaTriMoi,
                clientIp,
                fullNote,
                vaiTroThucHien
        );
    }

    private boolean isValidEvidencePath(String path) {
        if (path == null || path.isBlank()) return false;
        String clean = path.trim().toLowerCase();
        if (clean.startsWith("javascript:") || clean.startsWith("data:") || clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("file:")) {
            return false;
        }
        return clean.startsWith("/uploads/returns/") || clean.startsWith("uploads/returns/");
    }

    private String normalizeEvidencePath(String path) {
        if (path == null) return "";
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p;
    }
}

