package com.smashvn.shop.service.product;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProductAttributeDataContractTest {

    static class DanhMuc {
        int id;
        String name;
        DanhMuc(int id, String name) { this.id = id; this.name = name; }
    }

    static class ThuocTinh {
        int id;
        String name;
        ThuocTinh(int id, String name) { this.id = id; this.name = name; }
    }

    static class DanhMucThuocTinh {
        int idDanhMuc;
        int idThuocTinh;
        DanhMucThuocTinh(int idDanhMuc, int idThuocTinh) {
            this.idDanhMuc = idDanhMuc;
            this.idThuocTinh = idThuocTinh;
        }
    }

    static class SanPham {
        int id;
        int idDanhMuc;
        int idThuongHieu;
        String tenSanPham;
        String moTa;
        SanPham(int id, int idDanhMuc, int idThuongHieu, String tenSanPham, String moTa) {
            this.id = id;
            this.idDanhMuc = idDanhMuc;
            this.idThuongHieu = idThuongHieu;
            this.tenSanPham = tenSanPham;
            this.moTa = moTa;
        }
    }

    static class SanPhamChiTiet {
        int id;
        int idSanPham;
        BigDecimal giaBan;
        SanPhamChiTiet(int id, int idSanPham, BigDecimal giaBan) {
            this.id = id;
            this.idSanPham = idSanPham;
            this.giaBan = giaBan;
        }
    }

    static class SanPhamChiTietThuocTinh {
        int idSpct;
        int idThuocTinh;
        String giaTri;
        SanPhamChiTietThuocTinh(int idSpct, int idThuocTinh, String giaTri) {
            this.idSpct = idSpct;
            this.idThuocTinh = idThuocTinh;
            this.giaTri = giaTri;
        }
    }

    static class HinhAnhSanPham {
        int id;
        int idSanPham;
        int idSpct;
        String urlHinhAnh;
        String mauSac;
        HinhAnhSanPham(int id, int idSanPham, int idSpct, String urlHinhAnh, String mauSac) {
            this.id = id;
            this.idSanPham = idSanPham;
            this.idSpct = idSpct;
            this.urlHinhAnh = urlHinhAnh;
            this.mauSac = mauSac;
        }
    }

    private static final Map<Integer, DanhMuc> danhMucMap = new LinkedHashMap<>();
    private static final Map<Integer, ThuocTinh> thuocTinhMap = new LinkedHashMap<>();
    private static final List<DanhMucThuocTinh> dmtmList = new ArrayList<>();
    private static final Map<Integer, SanPham> sanPhamMap = new LinkedHashMap<>();
    private static final Map<Integer, SanPhamChiTiet> spctMap = new LinkedHashMap<>();
    private static final List<SanPhamChiTietThuocTinh> spctttList = new ArrayList<>();
    private static final List<HinhAnhSanPham> hinhAnhList = new ArrayList<>();

    @BeforeAll
    static void parseSqlScript() throws Exception {
        File sqlFile = new File("scratch/BadmintonShopDB1_ban_moi_nhat_.sql");
        assertTrue(sqlFile.exists(), "SQL script scratch/BadmintonShopDB1_ban_moi_nhat_.sql must exist");

        List<String> lines = Files.readAllLines(sqlFile.toPath(), StandardCharsets.UTF_8);

        String mode = "";
        int spCounter = 0;
        int spctCounter = 0;
        int haspCounter = 0;

        Pattern dmPattern = Pattern.compile("\\(N'([^']+)',\\s*([0-1])\\)");
        Pattern ttPattern = Pattern.compile("\\(N'([^']+)',\\s*([0-1])\\)");
        Pattern dmtmPattern = Pattern.compile("\\((\\d+),\\s*(\\d+),\\s*([0-1])\\)");

        Pattern spPattern = Pattern.compile("INSERT INTO \\[\\s*dbo\\s*\\]\\.\\[\\s*SanPham\\s*\\]\\s*\\([^\\)]+\\)\\s*VALUES\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*\\d+\\s*,\\s*N'((?:''|[^'])*)'\\s*,\\s*N'((?:''|[^'])*)'");
        Pattern spctPattern = Pattern.compile("INSERT INTO \\[\\s*dbo\\s*\\]\\.\\[\\s*SanPhamChiTiet\\s*\\]\\s*\\([^\\)]+\\)\\s*VALUES\\s*\\(\\s*(\\d+)\\s*,\\s*([\\d\\.]+)\\s*,\\s*([\\d\\.]+)\\s*,\\s*(\\d+)");
        Pattern spctttTuplePattern = Pattern.compile("\\((\\d+),\\s*(\\d+),\\s*N'((?:''|[^'])*)'\\)");
        Pattern haspTuplePattern = Pattern.compile("\\((\\d+),\\s*(\\d+),\\s*N'((?:''|[^'])*)',\\s*(?:N'((?:''|[^'])*)'|NULL),\\s*([0-1])\\)");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.contains("INSERT INTO [dbo].[DanhMuc]") && !trimmed.contains("DanhMucThuocTinh")) {
                mode = "DanhMuc";
                continue;
            } else if (trimmed.contains("INSERT INTO [dbo].[ThuocTinh]")) {
                mode = "ThuocTinh";
                continue;
            } else if (trimmed.contains("INSERT INTO [dbo].[DanhMucThuocTinh]")) {
                mode = "DanhMucThuocTinh";
                continue;
            }

            if (trimmed.startsWith("GO")) {
                mode = "";
                continue;
            }

            if ("DanhMuc".equals(mode)) {
                Matcher m = dmPattern.matcher(trimmed);
                while (m.find()) {
                    int id = danhMucMap.size() + 1;
                    danhMucMap.put(id, new DanhMuc(id, m.group(1)));
                }
            } else if ("ThuocTinh".equals(mode)) {
                Matcher m = ttPattern.matcher(trimmed);
                while (m.find()) {
                    int id = thuocTinhMap.size() + 1;
                    thuocTinhMap.put(id, new ThuocTinh(id, m.group(1)));
                }
            } else if ("DanhMucThuocTinh".equals(mode)) {
                Matcher m = dmtmPattern.matcher(trimmed);
                while (m.find()) {
                    dmtmList.add(new DanhMucThuocTinh(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
                }
            }

            if (trimmed.startsWith("INSERT INTO [dbo].[SanPham]")) {
                Matcher m = spPattern.matcher(trimmed);
                if (m.find()) {
                    spCounter++;
                    sanPhamMap.put(spCounter, new SanPham(
                            spCounter,
                            Integer.parseInt(m.group(1)),
                            Integer.parseInt(m.group(2)),
                            m.group(3).replace("''", "'"),
                            m.group(4).replace("''", "'")
                    ));
                }
            } else if (trimmed.startsWith("INSERT INTO [dbo].[SanPhamChiTiet]")) {
                Matcher m = spctPattern.matcher(trimmed);
                if (m.find()) {
                    spctCounter++;
                    spctMap.put(spctCounter, new SanPhamChiTiet(
                            spctCounter,
                            Integer.parseInt(m.group(1)),
                            new BigDecimal(m.group(3))
                    ));
                }
            } else if (trimmed.startsWith("INSERT INTO [dbo].[SanPhamChiTietThuocTinh]")) {
                Matcher m = spctttTuplePattern.matcher(trimmed);
                while (m.find()) {
                    spctttList.add(new SanPhamChiTietThuocTinh(
                            Integer.parseInt(m.group(1)),
                            Integer.parseInt(m.group(2)),
                            m.group(3).replace("''", "'")
                    ));
                }
            } else if (trimmed.startsWith("INSERT INTO [dbo].[HinhAnhSanPham]")) {
                Matcher m = haspTuplePattern.matcher(trimmed);
                while (m.find()) {
                    haspCounter++;
                    hinhAnhList.add(new HinhAnhSanPham(
                            haspCounter,
                            Integer.parseInt(m.group(1)),
                            Integer.parseInt(m.group(2)),
                            m.group(3),
                            m.group(4)
                    ));
                }
            }
        }
    }

    @Test
    @DisplayName("Assert ThuocTinh count = 7 and exact contract whitelist")
    void testThuocTinhWhitelist() {
        assertEquals(7, thuocTinhMap.size(), "ThuocTinh count must be exactly 7");
        List<String> expectedNames = List.of(
                "Màu sắc",
                "Độ cứng",
                "Trọng lượng",
                "Điểm cân bằng",
                "Loại người chơi",
                "Kích thước",
                "Sức căng"
        );
        for (int i = 0; i < expectedNames.size(); i++) {
            int id = i + 1;
            assertTrue(thuocTinhMap.containsKey(id), "ThuocTinh ID " + id + " must exist");
            assertEquals(expectedNames.get(i), thuocTinhMap.get(id).name, "ThuocTinh ID " + id + " name mismatch");
        }
    }

    @Test
    @DisplayName("Assert DanhMucThuocTinh mappings count and exact category mapping whitelist")
    void testDanhMucThuocTinhMappings() {
        assertEquals(16, dmtmList.size(), "DanhMucThuocTinh mapping count must be exactly 16");

        Map<Integer, Set<Integer>> expectedCategoryAttributes = new HashMap<>();
        // 1: Vợt (1: Màu sắc, 2: Độ cứng, 3: Trọng lượng, 4: Điểm cân bằng, 5: Loại người chơi, 7: Sức căng)
        expectedCategoryAttributes.put(1, Set.of(1, 2, 3, 4, 5, 7));
        // 2: Giày (1: Màu sắc, 6: Kích thước)
        expectedCategoryAttributes.put(2, Set.of(1, 6));
        // 3: Áo (1: Màu sắc, 6: Kích thước)
        expectedCategoryAttributes.put(3, Set.of(1, 6));
        // 4: Quần (1: Màu sắc, 6: Kích thước)
        expectedCategoryAttributes.put(4, Set.of(1, 6));
        // 5: Balo (1: Màu sắc)
        expectedCategoryAttributes.put(5, Set.of(1));
        // 6: Túi (1: Màu sắc)
        expectedCategoryAttributes.put(6, Set.of(1));
        // 7: Dây cước (1: Màu sắc)
        expectedCategoryAttributes.put(7, Set.of(1));
        // 8: Quấn cán (1: Màu sắc)
        expectedCategoryAttributes.put(8, Set.of(1));

        Map<Integer, Set<Integer>> actualMap = new HashMap<>();
        for (DanhMucThuocTinh row : dmtmList) {
            actualMap.computeIfAbsent(row.idDanhMuc, k -> new HashSet<>()).add(row.idThuocTinh);
        }

        assertEquals(expectedCategoryAttributes, actualMap, "DanhMucThuocTinh category-attribute mappings must match expected contract");
    }

    @Test
    @DisplayName("Assert Controlled Vocabularies (Stiffness, Balance, Player, Tension unit)")
    void testControlledVocabularies() {
        Set<String> validStiffness = Set.of("Dẻo", "Trung bình", "Cứng", "Siêu cứng");
        Set<String> validBalance = Set.of("Nhẹ đầu", "Cân bằng", "Hơi nặng đầu", "Nặng đầu", "Siêu nặng đầu");
        Set<String> validPlayer = Set.of("Tấn công", "Công thủ toàn diện", "Phản tạt, phòng thủ");

        List<String> violations = new ArrayList<>();

        for (SanPhamChiTietThuocTinh row : spctttList) {
            ThuocTinh tt = thuocTinhMap.get(row.idThuocTinh);
            if (tt == null) continue;

            String val = row.giaTri;
            if ("Độ cứng".equals(tt.name) && !validStiffness.contains(val)) {
                violations.add("SPCT " + row.idSpct + " Độ cứng non-canonical: '" + val + "'");
            } else if ("Điểm cân bằng".equals(tt.name) && !validBalance.contains(val)) {
                violations.add("SPCT " + row.idSpct + " Điểm cân bằng non-canonical: '" + val + "'");
            } else if ("Loại người chơi".equals(tt.name) && !validPlayer.contains(val)) {
                violations.add("SPCT " + row.idSpct + " Loại người chơi non-canonical: '" + val + "'");
            } else if ("Sức căng".equals(tt.name) && (!val.contains("lbs") || val.contains("kg"))) {
                violations.add("SPCT " + row.idSpct + " Sức căng invalid unit/format: '" + val + "'");
            }
        }

        assertTrue(violations.isEmpty(), "Non-canonical vocabulary violations found:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("Assert Kích thước values belong strictly to whitelist {39,40,41,42,S,M,L,XL} and G5 count = 0")
    void testKichThuocWhitelist() {
        Set<String> allowedSizes = Set.of("39", "40", "41", "42", "S", "M", "L", "XL");
        List<String> violations = new ArrayList<>();
        int g5Count = 0;

        for (SanPhamChiTietThuocTinh row : spctttList) {
            ThuocTinh tt = thuocTinhMap.get(row.idThuocTinh);
            if (tt != null && "Kích thước".equals(tt.name)) {
                if ("G5".equalsIgnoreCase(row.giaTri)) {
                    g5Count++;
                }
                if (!allowedSizes.contains(row.giaTri)) {
                    violations.add("SPCT " + row.idSpct + " Kích thước invalid value: '" + row.giaTri + "'");
                }
            }
        }

        assertEquals(0, g5Count, "G5 count in EAV Kích thước must be 0");
        assertTrue(violations.isEmpty(), "Invalid Kích thước values found:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("Assert Category Attribute Rules (No size on rackets, no racket attrs on shoes/apparel, etc.)")
    void testCategoryAttributeRules() {
        List<String> violations = new ArrayList<>();

        for (SanPhamChiTietThuocTinh row : spctttList) {
            SanPhamChiTiet spct = spctMap.get(row.idSpct);
            if (spct == null) continue;
            SanPham sp = sanPhamMap.get(spct.idSanPham);
            if (sp == null) continue;

            int catId = sp.idDanhMuc;
            int ttId = row.idThuocTinh;

            // Check if (catId, ttId) exists in dmtmList
            boolean allowed = dmtmList.stream().anyMatch(rowDm -> rowDm.idDanhMuc == catId && rowDm.idThuocTinh == ttId);
            if (!allowed) {
                violations.add("Category ID " + catId + " (" + danhMucMap.get(catId).name + ") product '" + sp.tenSanPham
                        + "' SPCT " + spct.id + " has forbidden attribute '" + thuocTinhMap.get(ttId).name + "' = '" + row.giaTri + "'");
            }
        }

        assertTrue(violations.isEmpty(), "Category attribute rule violations found:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("Assert Duplicate SPCT + Attribute Rows Count = 0")
    void testDuplicateAttributesPerVariant() {
        Map<String, Integer> counts = new HashMap<>();
        for (SanPhamChiTietThuocTinh row : spctttList) {
            String key = row.idSpct + "_" + row.idThuocTinh;
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add("Key " + entry.getKey() + " has " + entry.getValue() + " rows");
            }
        }

        assertTrue(duplicates.isEmpty(), "Duplicate attribute rows per SPCT found:\n" + String.join("\n", duplicates));
    }

    @Test
    @DisplayName("Assert Placeholder Metadata Values Count = 0")
    void testNoPlaceholderValues() {
        Set<String> forbiddenExactPlaceholders = Set.of(
                "Màu mặc định", "N/A", "NA", "NULL", "Không xác định", "mặc định",
                "chính hãng", "3 chính hãng", "1 nam chính hãng", "mã 081", "Lion chính hãng",
                "Loh Kean Yew 2025", "30 EX (Túi 2 cuộn)", "2 ngăn", "3 ngăn"
        );

        List<String> violations = new ArrayList<>();
        for (SanPhamChiTietThuocTinh row : spctttList) {
            String val = row.giaTri;
            String ttName = thuocTinhMap.get(row.idThuocTinh).name;

            for (String kw : forbiddenExactPlaceholders) {
                if (val.equalsIgnoreCase(kw)) {
                    violations.add("SPCT " + row.idSpct + " attribute '" + ttName + "' has placeholder value: '" + val + "'");
                    break;
                }
                if ("Màu sắc".equals(ttName) && val.equalsIgnoreCase(kw)) {
                    violations.add("SPCT " + row.idSpct + " Color has placeholder metadata: '" + val + "'");
                    break;
                }
            }
        }

        assertTrue(violations.isEmpty(), "Placeholder attribute values found:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("Print Audit Summary Report & Full Dataset Inspection")
    void printAuditSummary() {
        System.out.println("=== FULL DATASET AUDIT SUMMARY ===");
        System.out.println("DanhMuc count: " + danhMucMap.size());
        System.out.println("ThuocTinh count: " + thuocTinhMap.size());
        System.out.println("DanhMucThuocTinh count: " + dmtmList.size());
        System.out.println("SanPham count: " + sanPhamMap.size());
        System.out.println("SanPhamChiTiet count: " + spctMap.size());
        System.out.println("SanPhamChiTietThuocTinh count: " + spctttList.size());
        System.out.println("HinhAnhSanPham count: " + hinhAnhList.size());

        System.out.println("\n--- DISTINCT FINAL VOCABULARIES ---");
        for (ThuocTinh tt : thuocTinhMap.values()) {
            Set<String> distinctVals = spctttList.stream()
                    .filter(r -> r.idThuocTinh == tt.id)
                    .map(r -> r.giaTri)
                    .collect(Collectors.toCollection(TreeSet::new));
            System.out.println("Attribute [" + tt.id + "] " + tt.name + " (" + distinctVals.size() + " distinct values):");
            System.out.println("  " + String.join(" | ", distinctVals));
        }

        System.out.println("\n--- REPEATING GENERIC PRICE CANDIDATES ---");
        Map<BigDecimal, List<SanPhamChiTiet>> priceMap = spctMap.values().stream()
                .collect(Collectors.groupingBy(s -> s.giaBan));
        for (Map.Entry<BigDecimal, List<SanPhamChiTiet>> entry : priceMap.entrySet()) {
            if (entry.getValue().size() >= 4) {
                System.out.println("Price: " + entry.getKey() + " appears on " + entry.getValue().size() + " variants:");
                for (SanPhamChiTiet spct : entry.getValue()) {
                    SanPham sp = sanPhamMap.get(spct.idSanPham);
                    DanhMuc cat = danhMucMap.get(sp.idDanhMuc);
                    System.out.println("  SPCT " + spct.id + " | SP " + sp.id + " | Cat: " + cat.name + " | Product: " + sp.tenSanPham);
                }
            }
        }
    }
}

