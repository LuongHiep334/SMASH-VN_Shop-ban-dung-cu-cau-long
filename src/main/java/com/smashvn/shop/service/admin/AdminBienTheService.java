package com.smashvn.shop.service.admin;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhieuNhap;
import com.smashvn.shop.entity.PhieuNhapChiTiet;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuNhapRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.util.RacketSpecUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBienTheService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ThuocTinhRepository thuocTinhRepository;
    private final InventoryLotService inventoryLotService;
    private final PhieuNhapRepository phieuNhapRepository;
    private final NhanVienRepository nhanVienRepository;

    private static final String TRANG_THAI_DANG_BAN = "dang_ban";
    private static final String TRANG_THAI_NGUNG_KINH_DOANH = "ngung_kinh_doanh";

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    // 1. Lấy danh sách biến thể theo ID Sản phẩm gốc
    public List<SanPhamChiTiet> layDanhSachBienThe(Integer idSanPham) {
        return sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
    }

    // 2. Thêm biến thể mới (hoặc tự động nhập lô mới nếu biến thể đã tồn tại)
    @Transactional
    public void themBienThe(Integer idSanPham, BigDecimal giaBan, Integer soLuongTon,
            String mauSac, String trongLuong, String kichThuoc, String mucCang, MultipartFile fileAnh) throws Exception {
        themBienThe(idSanPham, giaBan, null, soLuongTon, mauSac, trongLuong, kichThuoc, mucCang, fileAnh, null);
    }

    @Transactional
    public void themBienThe(Integer idSanPham, BigDecimal giaBan, BigDecimal giaNhap, Integer soLuongTon,
            String mauSac, String trongLuong, String kichThuoc, String mucCang, MultipartFile fileAnh, Integer idNguoiDung) throws Exception {

        SanPham sp = sanPhamRepository.findById(idSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm gốc"));

        com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromDanhMuc(sp.getDanhMuc());
        if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            throw new IllegalArgumentException("Hộp cầu chỉ được phép có duy nhất một biến thể mặc định!");
        }

        String cleanMauSac = normalizeText(mauSac);
        if (cleanMauSac != null && cleanMauSac.length() > 50) {
            throw new IllegalArgumentException("Màu sắc không được vượt quá 50 ký tự.");
        }
        String cleanTrongLuong = null;
        String cleanKichThuoc = null;
        String cleanMucCang = null;

        if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
            cleanTrongLuong = normalizeCode(trongLuong);
            if (cleanTrongLuong == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_TRONG_LUONG_VOT.contains(cleanTrongLuong)) {
                throw new IllegalArgumentException("Trọng lượng vợt không hợp lệ (Chỉ chấp nhận: 3U, 4U, 5U)!");
            }
            cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);
        } else if (catType == com.smashvn.shop.constant.CategoryType.GIAY) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_GIAY.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước giày không hợp lệ (Chỉ chấp nhận: 36 đến 46)!");
            }
        } else if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_TRANG_PHUC.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước trang phục không hợp lệ (Chỉ chấp nhận: XS đến 3XL)!");
            }
        } else if (cleanMauSac == null) {
            throw new IllegalArgumentException("Màu sắc không được để trống!");
        }

        validateBasicFinancial(giaBan, soLuongTon);

        if (giaNhap != null && giaNhap.compareTo(BigDecimal.ZERO) > 0 && giaBan != null && giaNhap.compareTo(giaBan) > 0) {
            throw new IllegalArgumentException("Giá nhập hiện tại đang cao hơn giá bán (Giá nhập: "
                    + String.format("%,.0f", giaNhap) + " đ > Giá bán: " + String.format("%,.0f", giaBan) + " đ). Vui lòng kiểm tra và nhập lại!");
        }

        final String fMau = cleanMauSac != null ? cleanMauSac.toLowerCase() : "";
        final String fTrong = cleanTrongLuong != null ? cleanTrongLuong.toLowerCase() : "";
        final String fKich = cleanKichThuoc != null ? cleanKichThuoc.toLowerCase() : "";

        SanPhamChiTiet existingVariant = sanPhamChiTietRepository.findBySanPham_Id(idSanPham).stream()
                .filter(bt -> (bt.getMauSac() == null ? "" : bt.getMauSac().toLowerCase()).equals(fMau)
                && (bt.getTrongLuong() == null ? "" : bt.getTrongLuong().toLowerCase()).equals(fTrong)
                && (bt.getKichThuoc() == null ? "" : bt.getKichThuoc().toLowerCase()).equals(fKich))
                .findFirst()
                .orElse(null);

        BigDecimal actualGiaNhap = (giaNhap != null && giaNhap.compareTo(BigDecimal.ZERO) > 0) ? giaNhap : giaBan;

        if (existingVariant != null) {
            // Biến thể đã tồn tại -> Mở lại nếu đang ngưng kinh doanh & Nhập lô hàng mới cho biến thể này thay vì tạo mới
            if (TRANG_THAI_NGUNG_KINH_DOANH.equals(existingVariant.getTrangThai()) && (soLuongTon == null || soLuongTon > 0)) {
                existingVariant.setTrangThai(TRANG_THAI_DANG_BAN);
                sanPhamChiTietRepository.save(existingVariant);
            }
            inventoryLotService.nhapLoMoi(existingVariant.getId(), soLuongTon != null ? soLuongTon : 0, actualGiaNhap, idNguoiDung);
            return;
        }

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
                                log.error("Xóa file ảnh tạm thất bại sau rollback khi thêm biến thể: {}", path, e);
                            }
                        }
                    }
                }
            }
            );
        }

        String secureFileName = saveImageSecurely(fileAnh, false, uploadedFiles);
        if (secureFileName == null || secureFileName.isEmpty()) {
            List<SanPhamChiTiet> existingVariants = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
            for (SanPhamChiTiet existing : existingVariants) {
                if (existing.getMauSac() != null && existing.getMauSac().equalsIgnoreCase(cleanMauSac)
                        && existing.getHinhAnhSanPham() != null && !existing.getHinhAnhSanPham().isEmpty()) {
                    secureFileName = existing.getHinhAnhSanPham();
                    break;
                }
            }
            if (secureFileName == null && !existingVariants.isEmpty()) {
                secureFileName = existingVariants.get(0).getHinhAnhSanPham();
            }
            if (secureFileName == null) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc.");
            }
        }

        // Đảm bảo sản phẩm gốc có maSanPham trước khi sinh SKU
        if (sp.getMaSanPham() == null || sp.getMaSanPham().isBlank()) {
            sp.setMaSanPham(com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
            sp = sanPhamRepository.save(sp);
        }

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(giaBan);
        spct.setGiaNhap(actualGiaNhap);
        spct.setSoLuongTon(soLuongTon != null ? soLuongTon : 0);
        spct.setTrangThai(TRANG_THAI_DANG_BAN);

        saveOrUpdateAttribute(spct, "Màu sắc", cleanMauSac);
        saveOrUpdateAttribute(spct, "Trọng lượng", cleanTrongLuong);
        saveOrUpdateAttribute(spct, "Kích thước", cleanKichThuoc);
        saveOrUpdateAttribute(spct, "Sức căng", cleanMucCang);

        spct.setHinhAnhSanPham(secureFileName);
        spct = sanPhamChiTietRepository.save(spct);

        if (spct.getSku() == null) {
            spct.setSku(com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
            spct = sanPhamChiTietRepository.save(spct);
        }

        // Tự động tạo Phiếu nhập hàng khởi tạo lưu vào Lịch sử nhập hàng nếu biến thể mới có số lượng tồn > 0
        if (soLuongTon != null && soLuongTon > 0) {
            String maPN = inventoryLotService.generateMaPhieuNhap();
            NhanVien creator = null;
            if (idNguoiDung != null) {
                creator = nhanVienRepository.findByTaiKhoanId(idNguoiDung);
            }
            if (creator == null) {
                creator = nhanVienRepository.findAll().stream().findFirst().orElse(null);
            }
            LocalDateTime now = LocalDateTime.now();
            BigDecimal thanhTien = actualGiaNhap.multiply(BigDecimal.valueOf(soLuongTon));
            List<PhieuNhapChiTiet> chiTietList = new ArrayList<>();
            PhieuNhap phieuNhap = PhieuNhap.builder()
                    .maPhieuNhap(maPN)
                    .nhanVien(creator)
                    .ngayNhap(now)
                    .tongTien(thanhTien)
                    .ghiChu("Khởi tạo tồn kho khi thêm mới biến thể SP " + sp.getTenSanPham())
                    .ngayTao(now)
                    .ngayCapNhat(now)
                    .chiTietList(chiTietList)
                    .build();

            PhieuNhapChiTiet pnct = PhieuNhapChiTiet.builder()
                    .phieuNhap(phieuNhap)
                    .sanPhamChiTiet(spct)
                    .soLuong(soLuongTon)
                    .giaNhap(actualGiaNhap)
                    .thanhTien(thanhTien)
                    .build();
            chiTietList.add(pnct);
            phieuNhapRepository.save(phieuNhap);
            log.info("[AdminBienTheService] Đã tạo phiếu nhập ban đầu [{}] cho biến thể #{} SP '{}', SL={}, Giá nhập={}",
                    maPN, spct.getId(), sp.getTenSanPham(), soLuongTon, actualGiaNhap);
        }

        if (catType == com.smashvn.shop.constant.CategoryType.VOT && cleanMucCang != null) {
            updateMucCangAllVariants(idSanPham, cleanMucCang);
        }
    }

    @Transactional
    public void xoaBienThe(Integer idBienThe) {
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
        spct.setTrangThai(TRANG_THAI_NGUNG_KINH_DOANH);
        sanPhamChiTietRepository.save(spct);
    }

    @Transactional
    public void moBanLaiBienThe(Integer idBienThe) {
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
        spct.setTrangThai(TRANG_THAI_DANG_BAN);
        sanPhamChiTietRepository.save(spct);
    }

    public SanPhamChiTiet layBienTheTheoId(Integer idBienThe) {
        return sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể này"));
    }

    @Transactional
    public void capNhatBienThe(Integer idBienThe, BigDecimal giaBan, Integer soLuongTon,
            String mauSac, String trongLuong, String kichThuoc, String mucCang, MultipartFile fileAnh) throws Exception {

        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idBienThe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể để sửa"));

        com.smashvn.shop.constant.CategoryType catType = com.smashvn.shop.constant.CategoryType.fromDanhMuc(spct.getSanPham().getDanhMuc());
        if (catType == com.smashvn.shop.constant.CategoryType.HOP_CAU) {
            mauSac = "Mặc định";
            trongLuong = null;
            kichThuoc = null;
            mucCang = null;
        } else if (mauSac != null) {
            mauSac = mauSac.replaceAll("(?i),\\s*Mặc\\s*định", "").trim();
        }

        String cleanMauSac = normalizeText(mauSac);
        if (cleanMauSac != null && cleanMauSac.length() > 50) {
            throw new IllegalArgumentException("Màu sắc không được vượt quá 50 ký tự.");
        }
        String cleanTrongLuong = null;
        String cleanKichThuoc = null;
        String cleanMucCang = null;

        if (catType == com.smashvn.shop.constant.CategoryType.VOT) {
            cleanTrongLuong = normalizeCode(trongLuong);
            if (cleanTrongLuong == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_TRONG_LUONG_VOT.contains(cleanTrongLuong)) {
                throw new IllegalArgumentException("Trọng lượng vợt không hợp lệ (Chỉ chấp nhận: 3U, 4U, 5U)!");
            }
            cleanMucCang = RacketSpecUtils.sanitizeRecommendedTension(mucCang);
        } else if (catType == com.smashvn.shop.constant.CategoryType.GIAY) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_GIAY.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước giày không hợp lệ (Chỉ chấp nhận: 36 đến 46)!");
            }
        } else if (catType == com.smashvn.shop.constant.CategoryType.TRANG_PHUC) {
            cleanKichThuoc = normalizeCode(kichThuoc);
            if (cleanKichThuoc == null || !com.smashvn.shop.constant.SanPhamAttributeConfig.ALLOWED_KICH_THUOC_TRANG_PHUC.contains(cleanKichThuoc)) {
                throw new IllegalArgumentException("Kích thước trang phục không hợp lệ (Chỉ chấp nhận: XS đến 3XL)!");
            }
        } else if (catType != com.smashvn.shop.constant.CategoryType.HOP_CAU && cleanMauSac == null) {
            throw new IllegalArgumentException("Màu sắc không được để trống!");
        }

        validateBasicFinancial(giaBan, null);

        String targetAttrKey = inventoryLotService.buildAttributeKey(spct);
        List<SanPhamChiTiet> sameKeySpcts = sanPhamChiTietRepository.findBySanPham_Id(spct.getSanPham().getId()).stream()
                .filter(s -> inventoryLotService.buildAttributeKey(s).equals(targetAttrKey))
                .toList();

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
                                log.error("Xóa file ảnh tạm thất bại sau rollback khi cập nhật biến thể: {}", path, e);
                            }
                        }
                    }
                }
            }
            );
        }

        String secureFileName = saveImageSecurely(fileAnh, false, uploadedFiles);

        if (soLuongTon != null) {
            spct.setSoLuongTon(soLuongTon);
        }

        saveOrUpdateAttribute(spct, "Màu sắc", cleanMauSac);
        saveOrUpdateAttribute(spct, "Trọng lượng", cleanTrongLuong);
        saveOrUpdateAttribute(spct, "Kích thước", cleanKichThuoc);
        saveOrUpdateAttribute(spct, "Sức căng", cleanMucCang);

        // Đồng bộ giá bán và hình ảnh cho tất cả SPCT cùng nhóm AttributeKey
        for (SanPhamChiTiet member : sameKeySpcts) {
            member.setGiaBan(giaBan);
            if (secureFileName != null) {
                member.setHinhAnhSanPham(secureFileName);
            }
            sanPhamChiTietRepository.save(member);
        }
        sanPhamChiTietRepository.save(spct);

        if (catType == com.smashvn.shop.constant.CategoryType.VOT && cleanMucCang != null) {
            updateMucCangAllVariants(spct.getSanPham().getId(), cleanMucCang);
        }
    }

    private void saveOrUpdateAttribute(SanPhamChiTiet spct, String tenThuocTinh, String giaTri) {
        if (spct.getSanPhamChiTietThuocTinhs() == null) {
            spct.setSanPhamChiTietThuocTinhs(new java.util.LinkedHashSet<>());
        }

        String targetName = tenThuocTinh;
        ThuocTinh catTT = null;
        if (spct.getSanPham() != null && spct.getSanPham().getDanhMuc() != null && spct.getSanPham().getDanhMuc().getThuocTinhList() != null) {
            for (ThuocTinh att : spct.getSanPham().getDanhMuc().getThuocTinhList()) {
                if (att.getTenThuocTinh() != null) {
                    if (att.getTenThuocTinh().equalsIgnoreCase(tenThuocTinh)) {
                        targetName = att.getTenThuocTinh();
                        catTT = att;
                        break;
                    } else if (("Kích thước".equalsIgnoreCase(tenThuocTinh) || "Size".equalsIgnoreCase(tenThuocTinh))
                            && ("Kích thước".equalsIgnoreCase(att.getTenThuocTinh()) || "Size".equalsIgnoreCase(att.getTenThuocTinh()))) {
                        targetName = att.getTenThuocTinh();
                        catTT = att;
                        break;
                    }
                }
            }
        }

        final String finalTargetName = targetName;
        final ThuocTinh finalCatTT = catTT;

        SanPhamChiTietThuocTinh existing = spct.getSanPhamChiTietThuocTinhs().stream()
                .filter(tt -> tt.getThuocTinh() != null && (finalTargetName.equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh())
                || (("Kích thước".equalsIgnoreCase(finalTargetName) || "Size".equalsIgnoreCase(finalTargetName))
                && ("Kích thước".equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh()) || "Size".equalsIgnoreCase(tt.getThuocTinh().getTenThuocTinh())))))
                .findFirst()
                .orElse(null);

        if (giaTri == null || giaTri.isBlank()) {
            if (existing != null) {
                spct.getSanPhamChiTietThuocTinhs().remove(existing);
            }
            return;
        }

        if (existing != null) {
            existing.setGiaTri(giaTri.trim());
            if (finalCatTT != null && !existing.getThuocTinh().getId().equals(finalCatTT.getId())) {
                existing.setThuocTinh(finalCatTT);
            }
        } else {
            ThuocTinh tt = finalCatTT;
            if (tt == null) {
                tt = thuocTinhRepository.findByTenThuocTinhIgnoreCase(finalTargetName)
                        .orElseGet(() -> thuocTinhRepository.save(ThuocTinh.builder()
                        .tenThuocTinh(finalTargetName.trim())
                        .trangThai(true)
                        .build()));
            }
            SanPhamChiTietThuocTinh val = SanPhamChiTietThuocTinh.builder()
                    .sanPhamChiTiet(spct)
                    .thuocTinh(tt)
                    .giaTri(giaTri.trim())
                    .build();
            spct.getSanPhamChiTietThuocTinhs().add(val);
        }
    }

    private void updateMucCangAllVariants(Integer idSanPham, String cleanMucCang) {
        List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPham_Id(idSanPham);
        for (SanPhamChiTiet v : variants) {
            saveOrUpdateAttribute(v, "Sức căng", cleanMucCang);
            sanPhamChiTietRepository.save(v);
        }
    }

    private String normalizeText(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCode(String s) {
        String text = normalizeText(s);
        return text == null ? null : text.toUpperCase();
    }

    private void validateBasicFinancial(BigDecimal giaBan, Integer soLuongTon) {
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0.");
        }
        if (soLuongTon != null && soLuongTon < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho phải lớn hơn hoặc bằng 0.");
        }
    }

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");

    private String saveImageSecurely(MultipartFile file, boolean requireImage, List<Path> uploadedFiles) throws Exception {
        if (file == null || file.isEmpty()) {
            if (requireImage) {
                throw new IllegalArgumentException("Hình ảnh sản phẩm là bắt buộc.");
            }
            return null;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn 5MB.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP).");
        }
        String safeFileName = UUID.randomUUID().toString() + ext;
        Path targetUploadDir = Paths.get(uploadPathConfig, "product").toAbsolutePath().normalize();
        if (!Files.exists(targetUploadDir)) {
            Files.createDirectories(targetUploadDir);
        }
        Path targetFilePath = targetUploadDir.resolve(safeFileName).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        uploadedFiles.add(targetFilePath);
        return safeFileName;
    }
}
