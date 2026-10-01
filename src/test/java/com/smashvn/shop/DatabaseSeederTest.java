package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.service.product.DanhGiaService;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class DatabaseSeederTest {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private ThuocTinhRepository thuocTinhRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private GhnConfig ghnConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DanhGiaService danhGiaService;

    @Autowired
    private GioHangChiTietRepository gioHangChiTietRepository;

    @Autowired
    private SanPhamYeuThichRepository sanPhamYeuThichRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;

    private final Map<String, ThuongHieu> brandCache = new HashMap<>();
    private final Map<String, DanhMuc> categoryCache = new HashMap<>();
    private final Map<String, ThuocTinh> attributeCache = new HashMap<>();

    static class RacketSeed {
        String fileName;
        String brandName;
        String productName;
        String color;
        BigDecimal price;
        String customWeight;

        RacketSeed(String fileName, String brandName, String productName, String color, double price) {
            this(fileName, brandName, productName, color, price, null);
        }

        RacketSeed(String fileName, String brandName, String productName, String color, double price, String customWeight) {
            this.fileName = fileName;
            this.brandName = brandName;
            this.productName = productName;
            this.color = color;
            this.price = BigDecimal.valueOf(price);
            this.customWeight = customWeight;
        }
    }

    private ThuongHieu findOrCreateBrand(String brandName) {
        String key = brandName.trim().toLowerCase();
        if (brandCache.containsKey(key)) {
            return brandCache.get(key);
        }
        ThuongHieu th = thuongHieuRepository.findAll().stream()
                .filter(b -> b.getTenThuongHieu().equalsIgnoreCase(brandName))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu newBrand = new ThuongHieu();
                    newBrand.setTenThuongHieu(brandName);
                    newBrand.setTrangThai(true);
                    return thuongHieuRepository.save(newBrand);
                });
        brandCache.put(key, th);
        return th;
    }

    private DanhMuc findOrCreateCategory(String categoryName, String description) {
        String key = categoryName.trim().toLowerCase();
        if (categoryCache.containsKey(key)) {
            return categoryCache.get(key);
        }
        DanhMuc dm = danhMucRepository.findAll().stream()
                .filter(c -> c.getTenDanhMuc().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc newCat = new DanhMuc();
                    newCat.setTenDanhMuc(categoryName);
                    newCat.setTrangThai(true);
                    return danhMucRepository.save(newCat);
                });
        categoryCache.put(key, dm);
        return dm;
    }

    private ThuocTinh getOrCreateThuocTinh(String name) {
        String key = name.trim().toLowerCase();
        if (attributeCache.containsKey(key)) {
            return attributeCache.get(key);
        }
        ThuocTinh tt = thuocTinhRepository.findByTenThuocTinhIgnoreCase(name.trim())
                .orElseGet(() -> thuocTinhRepository.save(ThuocTinh.builder()
                        .tenThuocTinh(name.trim())
                        .trangThai(true)
                        .build()));
        attributeCache.put(key, tt);
        return tt;
    }

    private void setChiTietThuocTinh(SanPhamChiTiet spct, String tenThuocTinh, String giaTri) {
        if (giaTri == null || giaTri.isBlank()) return;
        ThuocTinh tt = getOrCreateThuocTinh(tenThuocTinh);
        if (spct.getSanPhamChiTietThuocTinhs() == null) {
            spct.setSanPhamChiTietThuocTinhs(new LinkedHashSet<>());
        }
        
        SanPhamChiTietThuocTinh existing = spct.getSanPhamChiTietThuocTinhs().stream()
                .filter(val -> val.getThuocTinh() != null && tenThuocTinh.equalsIgnoreCase(val.getThuocTinh().getTenThuocTinh()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setGiaTri(giaTri.trim());
        } else {
            SanPhamChiTietThuocTinh val = SanPhamChiTietThuocTinh.builder()
                    .sanPhamChiTiet(spct)
                    .thuocTinh(tt)
                    .giaTri(giaTri.trim())
                    .build();
            spct.getSanPhamChiTietThuocTinhs().add(val);
        }
    }

    private ThuongHieu detectBrand(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("yonex")) return findOrCreateBrand("Yonex");
        if (lower.contains("lining") || lower.contains("li-ning")) return findOrCreateBrand("Li-Ning");
        if (lower.contains("victor")) return findOrCreateBrand("Victor");
        if (lower.contains("gosen")) return findOrCreateBrand("GOSEN");
        if (lower.contains("kizuna")) return findOrCreateBrand("Kizuna");
        return findOrCreateBrand("Khác");
    }

    private String extractColor(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("đen trắng") || lower.contains("trắng đen")) return "Đen / Trắng";
        if (lower.contains("xanh ngọc")) return "Xanh Ngọc";
        if (lower.contains("xanh dương")) return "Xanh Dương";
        if (lower.contains("xanh chuối") || lower.contains("chuối")) return "Xanh Chuối";
        if (lower.contains("xanh lá")) return "Xanh Lá";
        if (lower.contains("trắng đỏ") || lower.contains("đỏ trắng")) return "Trắng / Đỏ";
        if (lower.contains("xám ngọc trai")) return "Xám Ngọc Trai";
        if (lower.contains("tím đêm")) return "Tím Đêm";
        if (lower.contains("vàng đen")) return "Vàng Đen";
        if (lower.contains("đen")) return "Màu Đen";
        if (lower.contains("trắng")) return "Màu Trắng";
        if (lower.contains("đỏ")) return "Màu Đỏ";
        if (lower.contains("xanh")) return "Màu Xanh";
        if (lower.contains("xám")) return "Màu Xám";
        if (lower.contains("tím")) return "Màu Tím";
        if (lower.contains("vàng")) return "Màu Vàng";
        if (lower.contains("cam")) return "Màu Cam";
        return "Mặc định";
    }

    @Test
    public void seedDatabase() {
        System.out.println("=== STARTING COMPLETE DATABASE SEEDING & ATTRIBUTE ENHANCEMENT FOR ALL CATEGORIES ===");

        // 1. Ensure Admin Accounts exist
        NhanVien employee = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("system_admin");
            tk.setMatKhau(passwordEncoder.encode("123456"));
            tk.setVaiTro("QL");
            tk.setTrangThai("hoat_dong");
            tk.setNgayTao(LocalDateTime.now());
            tk = taiKhoanRepository.save(tk);

            NhanVien nv = new NhanVien();
            nv.setTaiKhoan(tk);
            nv.setHoTenNv("Hệ thống Smash-VN");
            nv.setChucVu("Quản trị viên");
            nv.setSoDienThoaiNv("0987654321");
            nv.setNgayTao(LocalDateTime.now());
            return nhanVienRepository.save(nv);
        });

        TaiKhoan adminAcc = taiKhoanRepository.findByUsername("admin");
        if (adminAcc == null) {
            adminAcc = new TaiKhoan();
            adminAcc.setUsername("admin");
            adminAcc.setMatKhau(passwordEncoder.encode("123456"));
            adminAcc.setVaiTro("QL");
            adminAcc.setTrangThai("hoat_dong");
            adminAcc.setNgayTao(LocalDateTime.now());
            adminAcc = taiKhoanRepository.save(adminAcc);

            NhanVien nvAdmin = new NhanVien();
            nvAdmin.setTaiKhoan(adminAcc);
            nvAdmin.setHoTenNv("Quản trị viên Admin");
            nvAdmin.setChucVu("Quản lý");
            nvAdmin.setSoDienThoaiNv("0912345678");
            nvAdmin.setNgayTao(LocalDateTime.now());
            nhanVienRepository.save(nvAdmin);
        } else {
            adminAcc.setMatKhau(passwordEncoder.encode("123456"));
            adminAcc.setVaiTro("QL");
            adminAcc.setTrangThai("hoat_dong");
            taiKhoanRepository.save(adminAcc);
        }

        // Pre-populate brand, category, and attribute caches
        thuongHieuRepository.findAll().forEach(b -> brandCache.put(b.getTenThuongHieu().trim().toLowerCase(), b));
        danhMucRepository.findAll().forEach(c -> categoryCache.put(c.getTenDanhMuc().trim().toLowerCase(), c));
        thuocTinhRepository.findAll().forEach(a -> attributeCache.put(a.getTenThuocTinh().trim().toLowerCase(), a));

        // 2. Map of category folder names to formal category names
        Map<String, String> categoryFolderMap = new LinkedHashMap<>();
        categoryFolderMap.put("Áo", "Áo cầu lông");
        categoryFolderMap.put("Quần", "Quần cầu lông");
        categoryFolderMap.put("Balo", "Balo cầu lông");
        categoryFolderMap.put("Túi Xách", "Túi xách cầu lông");
        categoryFolderMap.put("Giày", "Giày cầu lông");
        categoryFolderMap.put("Cước", "Cước cầu lông");

        Map<String, BigDecimal> categoryPriceMap = new HashMap<>();
        categoryPriceMap.put("Áo", BigDecimal.valueOf(350000));
        categoryPriceMap.put("Quần", BigDecimal.valueOf(280000));
        categoryPriceMap.put("Balo", BigDecimal.valueOf(850000));
        categoryPriceMap.put("Túi Xách", BigDecimal.valueOf(950000));
        categoryPriceMap.put("Giày", BigDecimal.valueOf(1650000));
        categoryPriceMap.put("Cước", BigDecimal.valueOf(160000));

        File baseDir = new File("uploads/product");

        // 3. Process Subdirectory-based categories (Áo, Quần, Balo, Túi Xách, Giày, Cước)
        for (Map.Entry<String, String> entry : categoryFolderMap.entrySet()) {
            String folderName = entry.getKey();
            String formalCatName = entry.getValue();

            File catDir = new File(baseDir, folderName);
            if (!catDir.exists() || !catDir.isDirectory()) {
                System.out.println("Category directory not found: " + catDir.getAbsolutePath());
                continue;
            }

            DanhMuc category = findOrCreateCategory(formalCatName, "Sản phẩm " + formalCatName.toLowerCase() + " chính hãng");
            BigDecimal basePrice = categoryPriceMap.getOrDefault(folderName, BigDecimal.valueOf(500000));

            File[] productFolders = catDir.listFiles(File::isDirectory);
            if (productFolders == null) continue;

            for (File pFolder : productFolders) {
                String productName = pFolder.getName();
                ThuongHieu brand = detectBrand(productName);
                String extractedColor = extractColor(productName);

                SanPham existingProduct = sanPhamRepository.findAll().stream()
                        .filter(sp -> sp.getTenSanPham().equalsIgnoreCase(productName))
                        .findFirst()
                        .orElse(null);

                SanPham product;
                if (existingProduct == null) {
                    product = new SanPham();
                    product.setTenSanPham(productName);
                    product.setMoTa(productName + " chính hãng phân phối tại Smash-VN. Thiết kế thời trang, chất liệu cao cấp thoáng khí, mang lại sự thoải mái tối đa.");
                    product.setDanhMuc(category);
                    product.setThuongHieu(brand);
                    product.setNhanVien(employee);
                    product.setTrangThai("dang_ban");
                    product.setDiemTrungBinh(0.0);
                    product.setSoDanhGia(0);
                    product.setNgayTao(LocalDateTime.now());
                    product.setNgayCapNhat(LocalDateTime.now());
                } else {
                    product = existingProduct;
                    product.setDanhMuc(category);
                    product.setThuongHieu(brand);
                    product.setTrangThai("dang_ban");
                    product.setNgayCapNhat(LocalDateTime.now());
                }
                final SanPham savedProduct = sanPhamRepository.save(product);

                File[] imgFiles = pFolder.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
                });

                if (imgFiles != null && imgFiles.length > 0) {
                    List<String> sizeList;
                    String defaultMaterial;

                    if (folderName.equals("Áo") || folderName.equals("Quần")) {
                        sizeList = List.of("S", "M", "L", "XL");
                        defaultMaterial = "Thun lạnh thể thao cao cấp, co giãn 4 chiều, thấm hút mồ hôi tuyệt đối";
                    } else if (folderName.equals("Giày")) {
                        sizeList = List.of("39", "40", "41", "42", "43");
                        defaultMaterial = "Da PU cao cấp, đế cao su chống trượt bám sân, đệm phylon giảm chấn";
                    } else if (folderName.equals("Balo") || folderName.equals("Túi Xách")) {
                        sizeList = List.of("Standard");
                        defaultMaterial = "Vải Polyester dệt sợi chống thấm nước cao cấp, trang bị ngăn cách nhiệt";
                    } else { // Cước
                        sizeList = List.of("Standard");
                        defaultMaterial = "High-Polymer Nylon Braided Multifilament siêu bền";
                    }

                    final String finalExtractedColor = extractedColor;
                    for (String size : sizeList) {
                        final String finalSize = size;

                        List<SanPhamChiTiet> existingList = sanPhamChiTietRepository.findAll().stream()
                                .filter(ct -> ct.getSanPham() != null && ct.getSanPham().getId().equals(savedProduct.getId())
                                        && (finalExtractedColor.equalsIgnoreCase(ct.getMauSac()) || "Mặc định".equalsIgnoreCase(ct.getMauSac()))
                                        && finalSize.equalsIgnoreCase(ct.getKichThuoc()))
                                .toList();

                        SanPhamChiTiet spct;
                        if (existingList.isEmpty()) {
                            spct = new SanPhamChiTiet();
                            spct.setSanPham(savedProduct);
                            spct.setGiaBan(basePrice);
                            spct.setGiaNhap(basePrice.multiply(BigDecimal.valueOf(0.7)));
                            spct.setSoLuongTon(50);
                            spct.setTrangThai("dang_ban");
                            spct.setNgayTao(LocalDateTime.now());
                            spct.setNgayCapNhat(LocalDateTime.now());
                        } else {
                            spct = existingList.get(0);
                            spct.setSanPham(savedProduct);
                        }

                        // Set attributes safely using helper
                        setChiTietThuocTinh(spct, "Màu sắc", extractedColor);
                        setChiTietThuocTinh(spct, "Kích thước", size);
                        setChiTietThuocTinh(spct, "Chất liệu", defaultMaterial);

                        if (folderName.equals("Balo")) {
                            setChiTietThuocTinh(spct, "Trọng lượng", "0.95 kg");
                            setChiTietThuocTinh(spct, "Sức căng", "N/A");
                        } else if (folderName.equals("Túi Xách")) {
                            setChiTietThuocTinh(spct, "Trọng lượng", "1.20 kg");
                            setChiTietThuocTinh(spct, "Sức căng", "N/A");
                        } else if (folderName.equals("Cước")) {
                            setChiTietThuocTinh(spct, "Trọng lượng", "10m");
                            setChiTietThuocTinh(spct, "Sức căng", "0.65 mm - 0.68 mm");
                        } else {
                            setChiTietThuocTinh(spct, "Trọng lượng", "Tiêu chuẩn");
                            setChiTietThuocTinh(spct, "Sức căng", "N/A");
                        }

                        spct = sanPhamChiTietRepository.save(spct);

                        // Attach images
                        List<HinhAnhSanPham> imageEntities = new ArrayList<>();
                        int imgIdx = 0;
                        for (File imgFile : imgFiles) {
                            String relPath = folderName + "/" + productName + "/" + imgFile.getName();
                            HinhAnhSanPham hasp = new HinhAnhSanPham();
                            hasp.setSanPhamChiTiet(spct);
                            hasp.setUrlHinhAnh(relPath);
                            hasp.setMauSac(extractedColor);
                            hasp.setLaAnhChinh(imgIdx == 0);
                            imageEntities.add(hasp);
                            imgIdx++;
                        }

                        if (spct.getHinhAnhSanPhams() == null) {
                            spct.setHinhAnhSanPhams(new ArrayList<>());
                        } else {
                            spct.getHinhAnhSanPhams().clear();
                        }
                        spct.getHinhAnhSanPhams().addAll(imageEntities);

                        sanPhamChiTietRepository.save(spct);
                    }
                }
            }
        }

        // 4. Process Rackets in Li-ning and Yonex folders
        DanhMuc racketCat = findOrCreateCategory("Vợt cầu lông", "Các loại vợt cầu lông chính hãng");
        ThuongHieu yonexBrand = findOrCreateBrand("Yonex");
        ThuongHieu liningBrand = findOrCreateBrand("Li-Ning");

        List<RacketSeed> racketList = new ArrayList<>();
        racketList.add(new RacketSeed("Li-ning/AERONAUT 6000_Đỏ_xanh.png", "Li-Ning", "Li-Ning Aeronaut 6000", "Đỏ xanh", 2300000));
        racketList.add(new RacketSeed("Li-ning/AXFORCE 9 AYPT317-2_Đen.png", "Li-Ning", "Li-Ning Axforce 9 AYPT317-2", "Đen", 1850000));
        racketList.add(new RacketSeed("Li-ning/AXFORCE BIGBANG_Đen.png", "Li-Ning", "Li-Ning Axforce Bigbang Đen", "Đen", 1950000));
        racketList.add(new RacketSeed("Li-ning/Axforce Bigbang_Trắng.png", "Li-Ning", "Li-Ning Axforce Bigbang Trắng", "Trắng", 1950000));
        racketList.add(new RacketSeed("Li-ning/AXFORCE JR (5U) Trắng P-AYPT301-5.png", "Li-Ning", "Li-Ning Axforce JR Trắng", "Trắng", 1100000, "5U"));
        racketList.add(new RacketSeed("Li-ning/AXFORCE JR (5U) Đen P-AYPT299-5.png", "Li-Ning", "Li-Ning Axforce JR Đen", "Đen", 1100000, "5U"));
        racketList.add(new RacketSeed("Li-ning/Axforce 10_Xanh dương.png", "Li-Ning", "Li-Ning Axforce 10", "Xanh dương", 1250000));
        racketList.add(new RacketSeed("Li-ning/Axforce 30_Màu đen.png", "Li-Ning", "Li-Ning Axforce 30", "Đen", 1600000));
        racketList.add(new RacketSeed("Li-ning/Axforce 90 Tiger Max_Đỏ.png", "Li-Ning", "Li-Ning Axforce 90 Tiger Max", "Đỏ", 4200000));
        racketList.add(new RacketSeed("Li-ning/Axforce Thunder Cannon_Xanh Đậm.png", "Li-Ning", "Li-Ning Axforce Thunder Cannon", "Xanh Đậm", 2100000));
        racketList.add(new RacketSeed("Li-ning/CALIBAR 300C_Xam_Xanh.png", "Li-Ning", "Li-Ning Calibar 300C", "Xám Xanh", 1750000));
        racketList.add(new RacketSeed("Li-ning/CALIBAR 600I_Xám_Đen.png", "Li-Ning", "Li-Ning Calibar 600I", "Xám Đen", 2400000));
        racketList.add(new RacketSeed("Li-ning/CALIBAR 900_Vàng_Đen.png", "Li-Ning", "Li-Ning Calibar 900", "Vàng Đen", 3800000));
        racketList.add(new RacketSeed("Li-ning/Hỏa P-AYPT063-4_Đỏ.png", "Li-Ning", "Li-Ning Hỏa P-AYPT063-4", "Đỏ", 2600000));
        racketList.add(new RacketSeed("Li-ning/Phong P-AYPT059-4_Xanh.png", "Li-Ning", "Li-Ning Phong P-AYPT059-4", "Xanh", 2600000));
        racketList.add(new RacketSeed("Li-ning/WindStorm 72S Neon_Xanh nước biển nhạt.png", "Li-Ning", "Li-Ning Windstorm 72S Neon", "Xanh nước biển nhạt", 2150000));

        racketList.add(new RacketSeed("Yonex/ ARCSABER11_ Pro_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Pro", "Xám ngọc trai Đỏ", 4100000));
        racketList.add(new RacketSeed("Yonex/ARCSABER11_Play_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Play", "Xám ngọc trai Đỏ", 1350000));
        racketList.add(new RacketSeed("Yonex/ARCSABER11_Tour_Xám ngọc trai _Đỏ.png", "Yonex", "Yonex Arcsaber 11 Tour", "Xám ngọc trai Đỏ", 2650000));
        racketList.add(new RacketSeed("Yonex/ARCSABER7_Pro_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Pro", "Xám Chuối", 3900000));
        racketList.add(new RacketSeed("Yonex/ARCSABER7_Tour_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Tour", "Xám Chuối", 2550000));
        racketList.add(new RacketSeed("Yonex/ARCSABER_7_PLAY_Xam_Chuoi.png", "Yonex", "Yonex Arcsaber 7 Play", "Xám Chuối", 1300000));
        racketList.add(new RacketSeed("Yonex/ASTROX NEXTAGE_Đen_Xanh lá cây1.png", "Yonex", "Yonex Astrox Nextage", "Đen Xanh lá cây", 2700000));
        racketList.add(new RacketSeed("Yonex/ASTROX01f_lime_xanh chuối.png", "Yonex", "Yonex Astrox 01F", "Xanh chuối", 1150000));
        racketList.add(new RacketSeed("Yonex/ASTROX77-Pro_Cam Đậm.png", "Yonex", "Yonex Astrox 77 Pro", "Cam Đậm", 4050000));
        racketList.add(new RacketSeed("Yonex/ASTROX88D_Pro_Bạc đen.png", "Yonex", "Yonex Astrox 88D Pro", "Bạc đen", 4350000));
        racketList.add(new RacketSeed("Yonex/ASTROX88D_Tour_Bạc đen.png", "Yonex", "Yonex Astrox 88D Tour", "Bạc đen", 2750000));
        racketList.add(new RacketSeed("Yonex/ASTROX88S-Tour_Bạc đen.png", "Yonex", "Yonex Astrox 88S Tour", "Bạc đen", 2750000));
        racketList.add(new RacketSeed("Yonex/ASTROX99-Gen3-Pro_Đen_xanh lá cây(1).png", "Yonex", "Yonex Astrox 99 Gen 3 Pro", "Đen Xanh lá cây", 4500000));
        racketList.add(new RacketSeed("Yonex/ASTROX99-Gen3-Pro_Đen_xanh lá cây.png", "Yonex", "Yonex Astrox 99 Gen 3 Pro Special", "Đen Xanh lá cây", 4600000));
        racketList.add(new RacketSeed("Yonex/ASTROX_100_Play_Đỏ thẫm.png", "Yonex", "Yonex Astrox 100 Play", "Đỏ thẫm", 1400000));
        racketList.add(new RacketSeed("Yonex/Astrox_100_Tour_Đỏ thẫm.png", "Yonex", "Yonex Astrox 100 Tour", "Đỏ thẫm", 2800000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE NEXTAGE_Xám đậm.png", "Yonex", "Yonex Nanoflare Nextage", "Xám đậm", 2750000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE-800_Play_Xanh lá cây đậm.png", "Yonex", "Yonex Nanoflare 800 Play", "Xanh lá cây đậm", 1350000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE-800_Pro_Xanh lá cây đậm.png", "Yonex", "Yonex Nanoflare 800 Pro", "Xanh lá cây đậm", 4200000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_1000_z_Phay_Vàng_đen.png", "Yonex", "Yonex Nanoflare 1000 Z Play", "Vàng đen", 1450000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700G_Games_tím đêm.png", "Yonex", "Yonex Nanoflare 700G Games", "Tím đêm", 2200000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700_Play_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Play", "Tím đêm", 1350000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700_Pro_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Pro", "Tím đêm", 4250000));
        racketList.add(new RacketSeed("Yonex/NANOFLARE_700_Tour_tím đêm.png", "Yonex", "Yonex Nanoflare 700 Tour", "Tím đêm", 2700000));

        for (RacketSeed seed : racketList) {
            ThuongHieu brand = "Yonex".equalsIgnoreCase(seed.brandName) ? yonexBrand : liningBrand;

            SanPham existingProduct = sanPhamRepository.findAll().stream()
                    .filter(sp -> sp.getTenSanPham().equalsIgnoreCase(seed.productName))
                    .findFirst()
                    .orElse(null);

            SanPham product;
            if (existingProduct == null) {
                product = new SanPham();
                product.setTenSanPham(seed.productName);
                product.setMoTa("Vợt cầu lông " + seed.productName + " chính hãng phân phối tại Smash-VN. Thiết kế hiện đại, công nghệ tiên tiến.");
                product.setDanhMuc(racketCat);
                product.setThuongHieu(brand);
                product.setNhanVien(employee);
                product.setTrangThai("dang_ban");
                product.setDiemTrungBinh(0.0);
                product.setSoDanhGia(0);
                product.setNgayTao(LocalDateTime.now());
                product.setNgayCapNhat(LocalDateTime.now());
            } else {
                product = existingProduct;
                product.setDanhMuc(racketCat);
                product.setThuongHieu(brand);
                product.setTrangThai("dang_ban");
                product.setNgayCapNhat(LocalDateTime.now());
            }
            final SanPham savedProduct = sanPhamRepository.save(product);

            List<String> weights = (seed.customWeight != null) ? List.of(seed.customWeight) : List.of("3U", "4U");
            for (String weight : weights) {
                final String finalWeight = weight;
                List<SanPhamChiTiet> existingList = sanPhamChiTietRepository.findAll().stream()
                        .filter(ct -> ct.getSanPham() != null && ct.getSanPham().getId().equals(savedProduct.getId())
                                && seed.color.equalsIgnoreCase(ct.getMauSac()) && finalWeight.equalsIgnoreCase(ct.getTrongLuong()))
                        .toList();

                SanPhamChiTiet spct;
                if (existingList.isEmpty()) {
                    spct = new SanPhamChiTiet();
                    spct.setSanPham(savedProduct);
                    spct.setGiaBan(seed.price);
                    spct.setSoLuongTon(50);
                    spct.setGiaNhap(seed.price.multiply(BigDecimal.valueOf(0.7)));
                    spct.setTrangThai("dang_ban");
                    spct.setNgayTao(LocalDateTime.now());
                    spct.setNgayCapNhat(LocalDateTime.now());
                } else {
                    spct = existingList.get(0);
                    spct.setSanPham(savedProduct);
                }

                setChiTietThuocTinh(spct, "Màu sắc", seed.color);
                setChiTietThuocTinh(spct, "Trọng lượng", weight);
                setChiTietThuocTinh(spct, "Sức căng", "24 - 28 lbs");
                setChiTietThuocTinh(spct, "Kích thước", "G5");
                setChiTietThuocTinh(spct, "Chất liệu", "High Modulus Graphite / Carbon Fiber cao cấp");
                spct = sanPhamChiTietRepository.save(spct);

                HinhAnhSanPham hasp = new HinhAnhSanPham();
                hasp.setSanPhamChiTiet(spct);
                hasp.setUrlHinhAnh(seed.fileName);
                hasp.setMauSac(seed.color);
                hasp.setLaAnhChinh(true);

                if (spct.getHinhAnhSanPhams() == null) {
                    spct.setHinhAnhSanPhams(new ArrayList<>());
                } else {
                    spct.getHinhAnhSanPhams().clear();
                }
                spct.getHinhAnhSanPhams().add(hasp);

                sanPhamChiTietRepository.save(spct);
            }
        }

        // 5. Clean up any broken/dummy products whose images do NOT exist in uploads/product/
        auditAndCleanBrokenProductsInternal();

        // 6. Recalculate ratings for all remaining products
        List<SanPham> allProducts = sanPhamRepository.findAll();
        for (SanPham sp : allProducts) {
            danhGiaService.updateProductRatingStats(sp.getId());
        }
        System.out.println("Recalculated rating stats for all " + allProducts.size() + " products.");

        // 7. Ensure ONLY 2 Shipping Carriers exist in CSDL: GHN (Giao Hàng Nhanh) and TAIQUAY (Mua tại quầy)
        List<DonViVanChuyen> existingCarriers = donViVanChuyenDAO.findAll();

        List<DonViVanChuyen> carriersToSeed = List.of(
            createCarrier("GHN", "Giao Hàng Nhanh (GHN)", "1900 636677", "https://ghn.vn", 25000, 38000),
            createCarrier("TAIQUAY", "Mua tại quầy", "0987654321", "https://smashvn.com", 0, 0)
        );

        List<DonViVanChuyen> activeList = new ArrayList<>();
        for (DonViVanChuyen target : carriersToSeed) {
            DonViVanChuyen found = existingCarriers.stream()
                    .filter(c -> (c.getMaDonVi() != null && c.getMaDonVi().equalsIgnoreCase(target.getMaDonVi()))
                              || (c.getTenDonVi() != null && (c.getTenDonVi().toLowerCase().contains(target.getMaDonVi().toLowerCase()) || c.getTenDonVi().toLowerCase().contains("giao hàng nhanh") || c.getTenDonVi().toLowerCase().contains("quầy"))))
                    .findFirst()
                    .orElse(null);

            if (found == null) {
                found = donViVanChuyenDAO.save(target);
                System.out.println("Seeded new carrier: " + target.getTenDonVi());
            } else {
                found.setMaDonVi(target.getMaDonVi());
                found.setTenDonVi(target.getTenDonVi());
                found.setHotline(target.getHotline());
                found.setWebsite(target.getWebsite());
                found.setPhiLocal(target.getPhiLocal());
                found.setPhiNationwide(target.getPhiNationwide());
                found = donViVanChuyenDAO.save(found);
                System.out.println("Updated carrier info: " + found.getTenDonVi());
            }
            activeList.add(found);
        }

        // Clean up any obsolete carriers that are not GHN or TAIQUAY
        List<Integer> activeIds = activeList.stream().map(DonViVanChuyen::getId).toList();
        DonViVanChuyen defaultGhn = activeList.get(0);

        List<DonViVanChuyen> obsoleteCarriers = donViVanChuyenDAO.findAll().stream()
                .filter(c -> !activeIds.contains(c.getId()))
                .toList();

        for (DonViVanChuyen obsolete : obsoleteCarriers) {
            hoaDonRepository.findAll().stream()
                    .filter(hd -> hd.getDonViVanChuyen() != null && hd.getDonViVanChuyen().getId().equals(obsolete.getId()))
                    .forEach(hd -> {
                        hd.setDonViVanChuyen(defaultGhn);
                        hoaDonRepository.save(hd);
                    });
            donViVanChuyenDAO.delete(obsolete);
            System.out.println("Removed obsolete carrier: " + obsolete.getTenDonVi());
        }

        System.out.println("=== FULL DATABASE SEEDING AND ATTRIBUTE ENHANCEMENT COMPLETED SUCCESSFULLY ===");
    }

    private DonViVanChuyen createCarrier(String ma, String ten, String hotline, String web, double phiLocal, double phiNationwide) {
        DonViVanChuyen dv = new DonViVanChuyen();
        dv.setMaDonVi(ma);
        dv.setTenDonVi(ten);
        dv.setHotline(hotline);
        dv.setWebsite(web);
        dv.setPhiLocal(BigDecimal.valueOf(phiLocal));
        dv.setPhiNationwide(BigDecimal.valueOf(phiNationwide));
        return dv;
    }

    private void auditAndCleanBrokenProductsInternal() {
        System.out.println("--- AUDITING FOR PRODUCTS WITH BROKEN/MISSING IMAGES ON DISK ---");
        File baseDir = new File("uploads/product");

        List<SanPham> products = sanPhamRepository.findAll();
        List<SanPhamChiTiet> allSpcts = sanPhamChiTietRepository.findAll();
        Map<Integer, List<SanPhamChiTiet>> spctMap = allSpcts.stream()
                .filter(ct -> ct.getSanPham() != null)
                .collect(Collectors.groupingBy(ct -> ct.getSanPham().getId()));

        List<SanPhamYeuThich> allYeuThich = sanPhamYeuThichRepository.findAll();
        List<GioHangChiTiet> allGioHang = gioHangChiTietRepository.findAll();

        int deletedCount = 0;
        int fixedCount = 0;

        for (SanPham sp : new ArrayList<>(products)) {
            boolean hasValidImageOnDisk = false;
            List<SanPhamChiTiet> spcts = spctMap.getOrDefault(sp.getId(), List.of());

            if (!spcts.isEmpty()) {
                for (SanPhamChiTiet spct : spcts) {
                    if (spct.getHinhAnhSanPhams() != null && !spct.getHinhAnhSanPhams().isEmpty()) {
                        for (HinhAnhSanPham hasp : spct.getHinhAnhSanPhams()) {
                            String url = hasp.getUrlHinhAnh();
                            if (url != null && !url.isBlank()) {
                                String cleanUrl = url.replace("/uploads/product/", "").replace("uploads/product/", "");
                                File fileOnDisk = new File(baseDir, cleanUrl);
                                if (fileOnDisk.exists() && fileOnDisk.isFile()) {
                                    hasValidImageOnDisk = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (hasValidImageOnDisk) break;
                }
            }

            if (!hasValidImageOnDisk) {
                System.out.println("BROKEN IMAGE PRODUCT DETECTED: ID " + sp.getId() + " - Name: '" + sp.getTenSanPham() + "'");

                String cleanName = sp.getTenSanPham().replaceAll("(?i)cầu lông|chính hãng|nam|nữ|vợt|áo|quần|giày|balo|túi|t-shirt", "").trim();
                File matchFile = (cleanName.length() >= 3) ? findMatchingImageFile(baseDir, cleanName) : null;

                if (matchFile != null) {
                    String relPath = baseDir.toPath().relativize(matchFile.toPath()).toString().replace("\\", "/");
                    System.out.println(" -> FIXED image path to: " + relPath);
                    for (SanPhamChiTiet spct : spcts) {
                        if (spct.getHinhAnhSanPhams() == null) {
                            spct.setHinhAnhSanPhams(new ArrayList<>());
                        } else {
                            spct.getHinhAnhSanPhams().clear();
                        }
                        HinhAnhSanPham hasp = new HinhAnhSanPham();
                        hasp.setSanPhamChiTiet(spct);
                        hasp.setUrlHinhAnh(relPath);
                        hasp.setMauSac(spct.getMauSac());
                        hasp.setLaAnhChinh(true);
                        spct.getHinhAnhSanPhams().add(hasp);
                        sanPhamChiTietRepository.save(spct);
                    }
                    fixedCount++;
                } else {
                    System.out.println(" -> NO MATCHING IMAGE ON DISK. Deleting product ID " + sp.getId() + " ('" + sp.getTenSanPham() + "')");
                    
                    allYeuThich.stream()
                            .filter(yt -> yt.getSanPham() != null && yt.getSanPham().getId().equals(sp.getId()))
                            .forEach(sanPhamYeuThichRepository::delete);

                    for (SanPhamChiTiet spct : spcts) {
                        allGioHang.stream()
                                .filter(ghct -> ghct.getSanPhamChiTiet() != null && ghct.getSanPhamChiTiet().getId().equals(spct.getId()))
                                .forEach(gioHangChiTietRepository::delete);

                        List<HoaDonChiTiet> hdcts = hoaDonChiTietRepository.findAll().stream()
                                .filter(hdct -> hdct.getSanPhamChiTiet() != null && hdct.getSanPhamChiTiet().getId().equals(spct.getId()))
                                .collect(Collectors.toList());
                        hoaDonChiTietRepository.deleteAll(hdcts);

                        sanPhamChiTietRepository.delete(spct);
                    }
                    sanPhamRepository.delete(sp);
                    deletedCount++;
                }
            }
        }
        System.out.println("--- AUDIT SUMMARY: Fixed " + fixedCount + " products, Deleted " + deletedCount + " broken products without image files ---");
    }

    private File findMatchingImageFile(File dir, String keyword) {
        if (keyword == null || keyword.length() < 3) return null;
        String lowerKw = keyword.toLowerCase();
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                File subMatch = findMatchingImageFile(f, keyword);
                if (subMatch != null) return subMatch;
            } else {
                String name = f.getName().toLowerCase();
                if ((name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp"))
                        && (name.contains(lowerKw) || f.getParentFile().getName().toLowerCase().contains(lowerKw))) {
                    return f;
                }
            }
        }
        return null;
    }
}
