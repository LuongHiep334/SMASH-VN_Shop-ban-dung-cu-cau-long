package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeStockReservationService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserveReplacementStock(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 1. Lock HoaDon bằng Pessimistic Write Lock
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // 2. Validate loại yêu cầu
        if (!"DOI".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra())) {
            throw new IllegalStateException("Chỉ áp dụng cho yêu cầu ĐỔI HÀNG.");
        }

        // 3. Validate trạng thái xử lý hàng cũ
        ReturnInventoryStatus invStatus = hd.getTrangThaiXuLyHangHoan();
        if (invStatus != ReturnInventoryStatus.DA_HOAN_KHO && invStatus != ReturnInventoryStatus.DA_CHUYEN_KHO_LOI) {
            throw new IllegalStateException("Hàng hoàn phải được kiểm định và xử lý kho trước khi giao hàng đổi.");
        }

        // 4. Validate ReturnStatus & Inconsistent state guard
        ReturnStatus currentReturn = hd.getTrangThaiHoanHang();
        if (currentReturn == ReturnStatus.EXCHANGE_STOCK_ALLOCATED || currentReturn == ReturnStatus.EXCHANGE_SHIPPING || currentReturn == ReturnStatus.EXCHANGED) {
            log.info("[EXCHANGE_STOCK_RESERVATION] Đơn #{} đã ở trạng thái {} trước đó. Bỏ qua phân bổ kho trùng.", idHoaDon, currentReturn);
            return;
        }

        if (currentReturn != ReturnStatus.RETURNED) {
            throw new IllegalStateException("Chỉ đơn hàng ở trạng thái Đã kiểm hàng (RETURNED) mới được phép phân bổ kho đổi hàng. Trạng thái hiện tại: " + (currentReturn != null ? currentReturn.name() : "NULL"));
        }

        // Check inconsistent state: RETURNED + existing GHN_EXCHANGE in TichHopVanChuyen
        Integer existingExchangeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE'",
                Integer.class, idHoaDon
        );
        if (existingExchangeCount != null && existingExchangeCount > 0) {
            throw new IllegalStateException("Phát hiện vận đơn giao hàng đổi đã tồn tại nhưng chưa có trạng thái phân bổ tồn kho. Vui lòng kiểm tra thủ công trước khi tiếp tục.");
        }

        // 5. Group exact SPCT ID & quantities
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
        if (items.isEmpty()) {
            throw new IllegalStateException("Đơn hàng không có sản phẩm chi tiết.");
        }

        Map<Integer, Integer> exactQuantityMap = new LinkedHashMap<>();
        Map<Integer, SanPhamChiTiet> spctMap = new HashMap<>();
        for (HoaDonChiTiet item : items) {
            SanPhamChiTiet spct = item.getSanPhamChiTiet();
            if (spct == null) {
                throw new IllegalStateException("Sản phẩm chi tiết trong đơn hàng không tồn tại.");
            }
            spctMap.put(spct.getId(), spct);
            exactQuantityMap.merge(spct.getId(), item.getSoLuong(), Integer::sum);
        }

        // 6. Lock parent SanPham IDs in strictly ascending order (deadlock prevention)
        List<Integer> sortedProductIds = spctMap.values().stream()
                .map(s -> s.getSanPham().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (Integer spId : sortedProductIds) {
            sanPhamRepository.findByIdWithLock(spId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + spId));
        }

        // 7. Lock exact SPCTs & ALL-OR-NOTHING Stock Check
        List<Integer> sortedSpctIds = new ArrayList<>(exactQuantityMap.keySet());
        Collections.sort(sortedSpctIds);

        List<SanPhamChiTiet> lockedSpcts = new ArrayList<>();
        StringBuilder errorMsgBuilder = new StringBuilder();
        boolean hasInsufficient = false;

        for (Integer spctId : sortedSpctIds) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(spctId).orElseThrow();
            lockedSpcts.add(spct);
            int needed = exactQuantityMap.get(spctId);
            int currentStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            if (currentStock < needed) {
                hasInsufficient = true;
                if (!errorMsgBuilder.isEmpty()) errorMsgBuilder.append(" ");
                errorMsgBuilder.append(String.format("Sản phẩm '%s' (ID: %d) không đủ tồn kho để đổi (Cần: %d, Khả dụng: %d).",
                        spct.getSanPham().getTenSanPham(), spctId, needed, currentStock));
            }
        }

        if (hasInsufficient) {
            log.warn("[EXCHANGE_STOCK_RESERVATION] Phân bổ kho cho đơn #{} thất bại: {}", idHoaDon, errorMsgBuilder);
            throw new IllegalStateException(errorMsgBuilder.toString().trim());
        }

        // 8. Apply exact SPCT stock decrements (ALL-OR-NOTHING SUCCESS)
        for (SanPhamChiTiet spct : lockedSpcts) {
            int needed = exactQuantityMap.get(spct.getId());
            spct.setSoLuongTon(spct.getSoLuongTon() - needed);
            sanPhamChiTietRepository.save(spct);
        }

        // 9. Update HoaDon ReturnStatus to EXCHANGE_STOCK_ALLOCATED
        hd.setTrangThaiHoanHang(ReturnStatus.EXCHANGE_STOCK_ALLOCATED);
        hoaDonRepository.save(hd);

        // 10. Audit log
        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                (long) hd.getId(),
                "UPDATE",
                "trangThaiHoanHang=RETURNED",
                "trangThaiHoanHang=EXCHANGE_STOCK_ALLOCATED",
                clientIp,
                "[EXCHANGE_STOCK_ALLOCATED] Đã phân bổ tồn kho thành công cho đơn đổi hàng #" + idHoaDon,
                roleStr
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeExchangeShipping(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturn = hd.getTrangThaiHoanHang();
        if (currentReturn == ReturnStatus.EXCHANGE_SHIPPING || currentReturn == ReturnStatus.EXCHANGED) {
            log.info("[EXCHANGE_SHIPPING] Đơn #{} đã ở trạng thái {} trước đó.", idHoaDon, currentReturn);
            return;
        }

        if (currentReturn != ReturnStatus.EXCHANGE_STOCK_ALLOCATED) {
            throw new IllegalStateException("Đơn hàng chưa được phân bổ tồn kho (EXCHANGE_STOCK_ALLOCATED). Trạng thái hiện tại: " + (currentReturn != null ? currentReturn.name() : "NULL"));
        }

        // Verify GHN_EXCHANGE shipment exists in TichHopVanChuyen
        Integer exchangeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE'",
                Integer.class, idHoaDon
        );
        if (exchangeCount == null || exchangeCount == 0) {
            throw new IllegalStateException("Chưa tìm thấy vận đơn GHN_EXCHANGE trong CSDL cho đơn hàng #" + idHoaDon);
        }

        hd.setTrangThaiHoanHang(ReturnStatus.EXCHANGE_SHIPPING);
        hoaDonRepository.save(hd);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                (long) hd.getId(),
                "UPDATE",
                "trangThaiHoanHang=EXCHANGE_STOCK_ALLOCATED",
                "trangThaiHoanHang=EXCHANGE_SHIPPING",
                clientIp,
                "[EXCHANGE_SHIPPING] Đã tạo vận đơn GHN_EXCHANGE và chuyển đơn sang trạng thái đang giao sản phẩm mới.",
                roleStr
        );
    }
}
