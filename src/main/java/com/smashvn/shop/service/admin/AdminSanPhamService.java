package com.smashvn.shop.service.admin;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dto.AttributeValueRequest;
import com.smashvn.shop.dto.BienTheCreateRequest;
import com.smashvn.shop.entity.PhieuNhap;
import com.smashvn.shop.entity.PhieuNhapChiTiet;
import com.smashvn.shop.repository.PhieuNhapRepository;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.util.RacketSpecUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PhieuNhapRepository phieuNhapRepository;
    private final InventoryLotService inventoryLotService;
    private final AuditService auditService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    private String saveImageSecurely(MultipartFile file, String fieldName, List<Path> uploadedFiles) throws Exception {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file " + fieldName + " vượt quá giới hạn 5MB!");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File " + fieldName + " không hợp lệ!");
        }

        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Định dạng file " + fieldName + " không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP)!");
        }

        String safeFileName = UUID.randomUUID().toString() + ext;
        Path targetUploadDir = Paths.get(uploadPathConfig, "product").toAbsolutePath().normalize();

        if (!Files.exists(targetUploadDir)) {
            Files.createDirectories(targetUploadDir);
        }

        Path targetFilePath = targetUploadDir.resolve(safeFileName).normalize();
        if (!targetFilePath.startsWith(targetUploadDir)) {
            throw new IllegalArgumentException("Đường dẫn lưu file không hợp lệ!");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        uploadedFiles.add(targetFilePath);
        return safeFileName;
    }

    // --- HÀM THÊM MỚI (CHỈ LƯU SẢN PHẨM GỐC) ---
    @Transactional
    public void themSanPhamMoi(String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        SanPham sp = new SanPham();
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sp.setTrangThai("dang_ban");

        List<NhanVien> listNV = nhanVienRepository.findAll();
        if (!listNV.isEmpty()) {
            sp.setNhanVien(listNV.get(0));
        }

        sp = sanPhamRepository.save(sp);
        if (sp.getMaSanPham() == null) {
            sp.setMaSanPham(com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
            sanPhamRepository.save(sp);
        }
    }

    // --- HÀM THÊM MỚI CẢ SẢN PHẨM & TỰ ĐỘNG SINH BIẾN THỂ ---
    @Transactional(rollbackFor = Exception.class)
    public void themSanPhamVaBienThe(SanPhamCreateRequest request, Integer idNguoiDung, String remoteAddr) throws Exception {
        List<Path> uploadedFiles = new ArrayList<>();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        for (Path path : uploadedFiles) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception e) {
                                log.error("Xóa file thất bại sau rollback: {}", path, e);
                            }
                        }
                    }
                }
            }
            );
        }

        try {
            if (request == null) {
                throw new IllegalArgumentException("Dữ liệu không hợp lệ");
            }

            Integer idDanhMuc = request.getIdDanhMuc();
            if (idDanhMuc == null || idDanhMuc < 1) {
                throw new IllegalArgumentException("Vui lòng chọn danh mục hợp lệ!");
            }
            DanhMuc dm = danhMucRepository.findById(idDanhMuc)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
            if (Boolean.FALSE.equals(dm.getTrangThai())) {
                throw new IllegalArgumentException("Danh mục này đã bị ẩn, không thể thêm sản phẩm mới vào danh mục!");
            }

            com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromDanhMuc(dm);
            if (catType == com.smashvn.shop.constant.CategoryType.OTHER && dm.getThuocTinhList().isEmpty()) {
                throw new IllegalArgumentException("Danh mục chưa được cấu hình thuộc tính");
            }

            String trimmedTen = (request.getTenSanPham() == null) ? "" : request.getTenSanPham().trim();
            String sanitizedTen = org.jsoup.Jsoup.clean(trimmedTen, org.jsoup.safety.Safelist.none());
            if (sanitizedTen.isEmpty()) {
                throw new IllegalArgumentException("Tên sản phẩm bắt buộc!");
            }
            if (sanitizedTen.length() < 2 || sanitizedTen.length() > 100) {
                throw new IllegalArgumentException("Tên sản phẩm phải có độ dài từ 2 đến 100 ký tự!");
            }
            if (request.getIdThuongHieu() == null || request.getIdThuongHieu() < 1) {
                throw new IllegalArgumentException("Vui lòng chọn thương hiệu hợp lệ!");
            }
            ThuongHieu th = thuongHieuRepository.findById(request.getIdThuongHieu())
                    .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại"));
            if (Boolean.FALSE.equals(th.getTrangThai())) {
                throw new IllegalArgumentException("Hãng/thương hiệu này đã bị ẩn, không thể thêm sản phẩm mới vào thương hiệu này!");
            }

            String sanitizedMoTa = "";
            if (request.getMoTa() != null) {
                String trimmedMoTa = request.getMoTa().trim();
                sanitizedMoTa = org.jsoup.Jsoup.clean(trimmedMoTa, org.jsoup.safety.Safelist.none());
                if (sanitizedMoTa.length() > 2000) {
                    throw new IllegalArgumentException("Mô tả sản phẩm không được vượt quá 2000 ký tự!");
                }
            }

            if (request.getFileAnh() == null || request.getFileAnh().isEmpty()) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc khi thêm mới!");
            }

            String secureMainFileName = saveImageSecurely(request.getFileAnh(), "chính của sản phẩm", uploadedFiles);

            List<MultipartFile> colorImages = request.getColorImages();
            Map<String, Integer> colorIndexByColor = new LinkedHashMap<>();
            List<BienTheCreateRequest> rawVariants = request.getVariants() != null ? request.getVariants() : new ArrayList<>();

            if (catType != com.smashvn.shop.constant.CategoryType.HOP_CAU) {
                if (rawVariants.isEmpty()) {
                    throw new IllegalArgumentException("Danh sách biến thể không được để trống!");
                }
                if (rawVariants.size() > 100) {
                    throw new IllegalArgumentException("Số lượng biến thể được tạo ra vượt quá giới hạn cho phép (Tối đa 100 biến thể)!");
                }

                for (BienTheCreateRequest v : rawVariants) {
                    String normMau = normalizeText(v.getMauSac());
                    if (normMau == null) {
                        normMau = "Mặc định";
                    }
                    Integer idx = v.getColorIndex();
                    if (colorIndexByColor.containsKey(normMau)) {
                        if (!Objects.equals(colorIndexByColor.get(normMau), idx)) {
                            throw new IllegalArgumentException("Tất cả biến thể cùng màu '" + normMau + "' phải dùng thống nhất một chỉ số ảnh màu (colorIndex)!");
                        }
                    } else {
                        colorIndexByColor.put(normMau, idx);
                    }
                }
            }

            Map<Integer, String> savedColorImages = new HashMap<>();
            for (Integer cIdx : new HashSet<>(colorIndexByColor.values())) {
                if (cIdx != null) {
                    if (cIdx < 0 || colorImages == null || cIdx >= colorImages.size()) {
                        throw new IllegalArgumentException("Chỉ số ảnh màu (colorIndex) vượt quá phạm vi danh sách ảnh!");
                    }
                    MultipartFile colorFile = colorImages.get(cIdx);
                    if (colorFile != null && !colorFile.isEmpty()) {
                        String colorFileName = saveImageSecurely(colorFile, "màu index " + cIdx, uploadedFiles);
                        savedColorImages.put(cIdx, colorFileName);
                    }
                }
            }

            SanPham sp = new SanPham();
            sp.setTenSanPham(sanitizedTen);
            sp.setMoTa(sanitizedMoTa);
            sp.setDanhMuc(dm);
            sp.setThuongHieu(thuongHieuRepository.findById(request.getIdThuongHieu())
                    .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại")));
            sp.setTrangThai("dang_ban");

            NhanVien creator = null;
            if (idNguoiDung != null) {
                creator = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
                if (creator == null) {
                    TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
                    if (tk != null && ("QL".equals(tk.getVaiTro()) || "NV".equals(tk.getVaiTro()))) {
                        creator = new NhanVien();
                        creator.setTaiKhoan(tk);
                        String namePrefix = tk.getUsername();
                        if (namePrefix.contains("@")) {
                            namePrefix = namePrefix.split("@")[0];
                        }
                        creator.setHoTenNv("Quản trị viên " + namePrefix);
                        creator.setChucVu("QL".equals(tk.getVaiTro()) ? "Quản lý" : "Nhân viên");
                        creator.setSoDienThoaiNv("0000000000");
                        creator = nhanVienRepository.save(creator);
                    }
                }
            }

            if (creator == null) {
                List<NhanVien> listNV = nhanVienRepository.findAll();
                if (!listNV.isEmpty()) {
                    creator = listNV.get(0);
                } else {
                    throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên thao tác!");
                }
            }
            sp.setNhanVien(creator);
            sp = sanPhamRepository.save(sp);
            if (sp.getMaSanPham() == null) {
                sp.setMaSanPham(com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
                sp = sanPhamRepository.save(sp);
            }

            Set<String> checkDuplicates = new HashSet<>();
            int savedCount = 0;
            List<SanPhamChiTiet> createdSpcts = new ArrayList<>();

            LocalDateTime thoiGianNhap = LocalDateTime.now();

            if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
                BigDecimal gNhap = request.getGiaNhapDefault() != null ? request.getGiaNhapDefault() : BigDecimal.ZERO;
                BigDecimal gBan = request.getGiaBanDefault();
                if (gBan == null || gBan.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Giá bán phải lớn hơn 0 VNĐ!");
                }
                if (gNhap != null && gNhap.compareTo(BigDecimal.ZERO) > 0 && gNhap.compareTo(gBan) > 0) {
                    throw new IllegalArgumentException("Giá nhập hiện tại đang cao hơn giá bán (Giá nhập: " 
                            + String.format("%,.0f", gNhap) + " đ > Giá bán: " + String.format("%,.0f", gBan) + " đ). Vui lòng kiểm tra và nhập lại!");
                }
                Integer sTon = request.getSoLuongTonDefault() != null ? request.getSoLuongTonDefault() : 0;

                SanPhamChiTiet spct = new SanPhamChiTiet();
                spct.setSanPham(sp);
                spct.setGiaNhap(gNhap);
                spct.setGiaBan(gBan);
                spct.setSoLuongTon(sTon);
                spct.setTrangThai("dang_ban");
                spct.setHinhAnhSanPham(secureMainFileName);
                spct.setNgayTao(thoiGianNhap);
                spct.setNgayCapNhat(thoiGianNhap);

                saveConfiguredVariantAttributeIfPresent(spct, dm, "Màu sắc", "Mặc định");
                spct = sanPhamChiTietRepository.save(spct);
                if (spct.getSku() == null) {
                    spct.setSku(com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
                    sanPhamChiTietRepository.save(spct);
                }
                createdSpcts.add(spct);
                savedCount = 1;
            } else {
                String cleanMucCangChung = RacketSpecUtils.sanitizeRecommendedTension(request.getMucCang());

                for (BienTheCreateRequest v : rawVariants) {
                    String normMau = normalizeText(v.getMauSac());
                    if (normMau == null) {
                        normMau = "Mặc định";
                    }
                    String normTrong = null;
                    String normKich = null;
                    String normCang = null;

                    if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
                        normTrong = normalizeCode(v.getTrongLuong());
                        normCang = cleanMucCangChung;
                    } else if (catType == com.smashvn.shop.constant.CategoryType.GIAY || catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC) {
                        normKich = normalizeCode(v.getKichThuoc());
                    }

                    BigDecimal gNhap = v.getGiaNhap() != null ? v.getGiaNhap() : (request.getGiaNhapDefault() != null ? request.getGiaNhapDefault() : BigDecimal.ZERO);
                    BigDecimal gBan = v.getGiaBan() != null ? v.getGiaBan() : request.getGiaBanDefault();
                    if (gBan == null || gBan.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Giá bán của biến thể phải lớn hơn 0 VNĐ!");
                    }
                    if (gNhap != null && gNhap.compareTo(BigDecimal.ZERO) > 0 && gNhap.compareTo(gBan) > 0) {
                        String variantDesc = (normMau != null ? normMau : "Mặc định") 
                                + (normTrong != null ? " - " + normTrong : "") 
                                + (normKich != null ? " - " + normKich : "");
                        throw new IllegalArgumentException("Giá nhập hiện tại đang cao hơn giá bán tại phân loại [" + variantDesc + "] (Giá nhập: " 
                                + String.format("%,.0f", gNhap) + " đ > Giá bán: " + String.format("%,.0f", gBan) + " đ). Vui lòng kiểm tra và nhập lại!");
                    }
                    Integer sTon = v.getSoLuongTon() != null ? v.getSoLuongTon() : (request.getSoLuongTonDefault() != null ? request.getSoLuongTonDefault() : 0);

                    String combKey = buildVariantCombinationKey(v, normMau, normTrong, normKich);
                    if (checkDuplicates.contains(combKey)) {
                        throw new IllegalArgumentException("Có biến thể bị trùng lặp trong danh sách nhập!");
                    }
                    checkDuplicates.add(combKey);

                    Integer cIdx = v.getColorIndex();
                    String variantFileName = (cIdx != null && savedColorImages.containsKey(cIdx)) ? savedColorImages.get(cIdx) : secureMainFileName;

                    SanPhamChiTiet spct = new SanPhamChiTiet();
                    spct.setSanPham(sp);
                    spct.setGiaNhap(gNhap);
                    spct.setGiaBan(gBan);
                    spct.setSoLuongTon(sTon);
                    spct.setTrangThai("dang_ban");
                    spct.setHinhAnhSanPham(variantFileName);
                    spct.setNgayTao(thoiGianNhap);
                    spct.setNgayCapNhat(thoiGianNhap);

                    if (v.getAttributes() != null && !v.getAttributes().isEmpty()) {
                        Set<Integer> savedAttributeIds = new HashSet<>();
                        for (AttributeValueRequest attrReq : v.getAttributes()) {
                            if (attrReq == null || attrReq.getValue() == null || attrReq.getValue().isBlank()) {
                                continue;
                            }
                            ThuocTinh configuredAttribute = resolveConfiguredAttribute(dm, attrReq);
                            if (!savedAttributeIds.add(configuredAttribute.getId())) {
                                throw new IllegalArgumentException(
                                        "Thuộc tính \"" + configuredAttribute.getTenThuocTinh()
                                                + "\" bị lặp trong cùng một biến thể.");
                            }
                            saveVariantAttribute(spct, configuredAttribute, attrReq.getValue());
                        }
                    } else {
                        saveConfiguredVariantAttributeIfPresent(spct, dm, "Màu sắc", normMau);
                        saveConfiguredVariantAttributeIfPresent(spct, dm, "Trọng lượng", normTrong);
                        saveConfiguredVariantAttributeIfPresent(spct, dm, "Kích thước", normKich);
                        saveConfiguredVariantAttributeIfPresent(spct, dm, "Sức căng", normCang);
                    }

                    spct = sanPhamChiTietRepository.save(spct);
                    if (spct.getSku() == null) {
                        spct.setSku(com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
                        sanPhamChiTietRepository.save(spct);
                    }
                    createdSpcts.add(spct);
                    savedCount++;
                }
            }

            // Tự động tạo Phiếu nhập hàng ban đầu để lưu vào Lịch sử nhập hàng cho các biến thể có số lượng nhập > 0
            List<SanPhamChiTiet> variantsWithStock = new ArrayList<>();
            for (SanPhamChiTiet vSp : createdSpcts) {
                if (vSp != null && vSp.getSoLuongTon() != null && vSp.getSoLuongTon() > 0) {
                    variantsWithStock.add(vSp);
                }
            }

            if (!variantsWithStock.isEmpty()) {
                if (creator == null) {
                    creator = nhanVienRepository.findAll().stream().findFirst().orElse(null);
                }
                String maPN = inventoryLotService.generateMaPhieuNhap();
                BigDecimal tongTienPhieuNhap = BigDecimal.ZERO;
                List<PhieuNhapChiTiet> chiTietList = new ArrayList<>();
                PhieuNhap phieuNhap = PhieuNhap.builder()
                        .maPhieuNhap(maPN)
                        .nhanVien(creator)
                        .ngayNhap(thoiGianNhap)
                        .ghiChu("Nhập hàng khởi tạo khi tạo sản phẩm: " + sp.getTenSanPham())
                        .ngayTao(thoiGianNhap)
                        .ngayCapNhat(thoiGianNhap)
                        .chiTietList(chiTietList)
                        .build();

                for (SanPhamChiTiet vSpct : variantsWithStock) {
                    BigDecimal gNhap = (vSpct.getGiaNhap() != null && vSpct.getGiaNhap().compareTo(BigDecimal.ZERO) > 0)
                            ? vSpct.getGiaNhap()
                            : (vSpct.getGiaBan() != null ? vSpct.getGiaBan() : BigDecimal.ZERO);
                    int sl = vSpct.getSoLuongTon();
                    BigDecimal thanhTien = gNhap.multiply(BigDecimal.valueOf(sl));
                    tongTienPhieuNhap = tongTienPhieuNhap.add(thanhTien);

                    PhieuNhapChiTiet pnct = PhieuNhapChiTiet.builder()
                            .phieuNhap(phieuNhap)
                            .sanPhamChiTiet(vSpct)
                            .soLuong(sl)
                            .giaNhap(gNhap)
                            .thanhTien(thanhTien)
                            .build();
                    chiTietList.add(pnct);
                }

                phieuNhap.setTongTien(tongTienPhieuNhap);
                phieuNhapRepository.save(phieuNhap);
                log.info("[AdminSanPhamService] Đã tạo phiếu nhập kho ban đầu [{}] cho sản phẩm '{}' với {} biến thể, tổng tiền={}",
                        maPN, sp.getTenSanPham(), variantsWithStock.size(), tongTienPhieuNhap);
            }

            String role = (creator != null && creator.getChucVu() != null) ? creator.getChucVu() : "Quản trị viên";
            String note = "Thêm sản phẩm '" + sp.getTenSanPham() + "' cùng " + savedCount + " biến thể.";
            auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "INSERT", "", sp.getTenSanPham(), remoteAddr, note, role);

        } catch (Exception e) {
            for (Path path : uploadedFiles) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ex) {
                    log.error("Xóa file thất bại sau exception: {}", path, ex);
                }
            }
            throw e;
        }
    }

    private String buildVariantCombinationKey(
            BienTheCreateRequest variant,
            String normalizedColor,
            String normalizedWeight,
            String normalizedSize) {
        if (variant.getAttributes() == null || variant.getAttributes().isEmpty()) {
            return normalizedColor.toLowerCase() + "|"
                    + (normalizedWeight != null ? normalizedWeight.toLowerCase() : "") + "|"
                    + (normalizedSize != null ? normalizedSize.toLowerCase() : "");
        }

        return variant.getAttributes().stream()
                .filter(attribute -> attribute != null
                        && attribute.getValue() != null
                        && !attribute.getValue().isBlank())
                .map(attribute -> {
                    String identity = attribute.getAttributeId() != null
                            ? "id:" + attribute.getAttributeId()
                            : "name:" + String.valueOf(attribute.getAttributeName()).trim().toLowerCase();
                    return identity + "=" + attribute.getValue().trim().toLowerCase();
                })
                .sorted()
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private ThuocTinh resolveConfiguredAttribute(DanhMuc category, AttributeValueRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thuộc tính biến thể không hợp lệ.");
        }
        return category.getThuocTinhList().stream()
                .filter(attribute -> request.getAttributeId() != null
                        ? request.getAttributeId().equals(attribute.getId())
                        : request.getAttributeName() != null
                                && request.getAttributeName().trim().equalsIgnoreCase(attribute.getTenThuocTinh()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Thuộc tính \"" + request.getAttributeName()
                                + "\" không được cấu hình cho danh mục này."));
    }

    private void saveConfiguredVariantAttributeIfPresent(
            SanPhamChiTiet spct,
            DanhMuc category,
            String attributeName,
            String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        category.getThuocTinhList().stream()
                .filter(attribute -> attributeName.equalsIgnoreCase(attribute.getTenThuocTinh()))
                .findFirst()
                .ifPresent(attribute -> saveVariantAttribute(spct, attribute, value));
    }

    private void saveVariantAttribute(SanPhamChiTiet spct, ThuocTinh tt, String giaTri) {
        if (giaTri == null || giaTri.isBlank()) {
            return;
        }
        SanPhamChiTietThuocTinh attVal = SanPhamChiTietThuocTinh.builder()
                .sanPhamChiTiet(spct)
                .thuocTinh(tt)
                .giaTri(giaTri.trim())
                .build();
        if (spct.getSanPhamChiTietThuocTinhs() == null) {
            spct.setSanPhamChiTietThuocTinhs(new java.util.LinkedHashSet<>());
        }
        spct.getSanPhamChiTietThuocTinhs().add(attVal);
    }

    private String normalizeText(String str) {
        if (str == null) {
            return null;
        }
        String s = str.trim();
        return s.isEmpty() ? null : s;
    }

    private String normalizeCode(String str) {
        if (str == null) {
            return null;
        }
        String s = str.trim().toUpperCase();
        return s.isEmpty() ? null : s;
    }

    public List<SanPham> layTatCaSanPham() {
        return sanPhamRepository.findAllByOrderByIdDesc();
    }

    public SanPham layTheoId(Integer id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm id: " + id));
    }

    @Transactional
    public void capNhatSanPham(Integer id, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa) {
        capNhatSanPham(id, tenSanPham, idDanhMuc, idThuongHieu, moTa, null, null);
    }

    @Transactional
    public void capNhatSanPham(Integer id, String tenSanPham, Integer idDanhMuc, Integer idThuongHieu, String moTa, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = layTheoId(id);
        sp.setTenSanPham(tenSanPham);
        sp.setMoTa(moTa);
        sp.setDanhMuc(danhMucRepository.findById(idDanhMuc).orElseThrow());
        sp.setThuongHieu(thuongHieuRepository.findById(idThuongHieu).orElseThrow());
        sanPhamRepository.save(sp);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dangBan(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = layTheoId(idSanPham);

        // 1. Kiểm tra tên sản phẩm
        if (sp.getTenSanPham() == null || sp.getTenSanPham().trim().isEmpty()) {
            throw new IllegalArgumentException("Không thể đăng bán: Tên sản phẩm không được để trống!");
        }

        // 2. Kiểm tra danh mục
        if (sp.getDanhMuc() == null) {
            throw new IllegalArgumentException("Không thể đăng bán: Sản phẩm chưa được gán danh mục!");
        }
        if (Boolean.FALSE.equals(sp.getDanhMuc().getTrangThai())) {
            throw new IllegalArgumentException("Không thể đăng bán: Danh mục '" + sp.getDanhMuc().getTenDanhMuc() + "' hiện đang bị ẩn!");
        }

        // 3. Kiểm tra thương hiệu
        if (sp.getThuongHieu() == null) {
            throw new IllegalArgumentException("Không thể đăng bán: Sản phẩm chưa được gán thương hiệu!");
        }
        if (Boolean.FALSE.equals(sp.getThuongHieu().getTrangThai())) {
            throw new IllegalArgumentException("Không thể đăng bán: Thương hiệu '" + sp.getThuongHieu().getTenThuongHieu() + "' hiện đang bị ẩn!");
        }

        // 4. Kiểm tra có ít nhất một biến thể
        List<SanPhamChiTiet> variants = sp.getSanPhamChiTiets();
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("Không thể đăng bán: Sản phẩm phải có ít nhất một biến thể (phân loại hàng)!");
        }

        // 5. Kiểm tra có ít nhất một biến thể có giá bán hợp lệ (> 0)
        boolean hasValidPriceVariant = variants.stream()
                .anyMatch(v -> v.getGiaBan() != null && v.getGiaBan().compareTo(BigDecimal.ZERO) > 0);
        if (!hasValidPriceVariant) {
            throw new IllegalArgumentException("Không thể đăng bán: Sản phẩm phải có ít nhất một biến thể với giá bán hợp lệ (> 0 VNĐ)!");
        }

        sp.setTrangThai("dang_ban");
        sp.setNgayCapNhat(LocalDateTime.now());
        sanPhamRepository.save(sp);

        String role = "Quản trị viên";
        if (idNguoiDung != null) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
            if (nv != null && nv.getChucVu() != null) {
                role = nv.getChucVu();
            }
        }
        auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "UPDATE", "ngung_kinh_doanh", "dang_ban", remoteAddr, "Đăng bán sản phẩm: " + sp.getTenSanPham(), role);
        log.info("[PRODUCT_CMS] Product ID: {} ({}) set to DANG_BAN by user: {}", sp.getId(), sp.getTenSanPham(), idNguoiDung);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ngungHienThi(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        SanPham sp = layTheoId(idSanPham);
        sp.setTrangThai("ngung_kinh_doanh");
        sp.setNgayCapNhat(LocalDateTime.now());
        sanPhamRepository.save(sp);

        String role = "Quản trị viên";
        if (idNguoiDung != null) {
            NhanVien nv = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
            if (nv != null && nv.getChucVu() != null) {
                role = nv.getChucVu();
            }
        }
        auditService.log(idNguoiDung, "SanPham", sp.getId().longValue(), "UPDATE", "dang_ban", "ngung_kinh_doanh", remoteAddr, "Ngưng hiển thị sản phẩm: " + sp.getTenSanPham(), role);
        log.info("[PRODUCT_CMS] Product ID: {} ({}) set to NGUNG_HIEN_THI by user: {}", sp.getId(), sp.getTenSanPham(), idNguoiDung);
    }

    @Transactional
    public void xoaSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        ngungHienThi(idSanPham, idNguoiDung, remoteAddr);
    }

    @Transactional
    public void ngungKinhDoanhSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        ngungHienThi(idSanPham, idNguoiDung, remoteAddr);
    }

    @Transactional
    public void moBanLaiSanPham(Integer idSanPham, Integer idNguoiDung, String remoteAddr) {
        dangBan(idSanPham, idNguoiDung, remoteAddr);
    }
}
