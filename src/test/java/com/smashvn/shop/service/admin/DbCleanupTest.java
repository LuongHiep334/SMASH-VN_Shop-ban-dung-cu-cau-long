package com.smashvn.shop.service.admin;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DbCleanupTest {
    @Test
    public void cleanDatabase() throws Exception {
        Map<String, String> env = new HashMap<>();
        
        // Try reading .env file first
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("../.env");
        }
        
        if (envFile.exists()) {
            System.out.println("Loading configuration from " + envFile.getAbsolutePath());
            try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        env.put(key, value);
                    }
                }
            }
        }

        String dbUrl = env.get("DB_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = System.getenv("DB_URL");
        }
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:sqlserver://localhost:1433;databaseName=SMDB_FINAL;encrypt=true;trustServerCertificate=true;";
        }
        
        String username = env.get("DB_USERNAME");
        if (username == null || username.isEmpty()) {
            username = System.getenv("DB_USERNAME");
        }
        
        String password = env.get("DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            password = System.getenv("DB_PASSWORD");
        }

        System.out.println("Connecting to database: " + dbUrl);
        System.out.println("Using username: " + username);

        try (Connection conn = DriverManager.getConnection(dbUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // 1. Audit DotGiamGia
            System.out.println("--- Auditing DotGiamGia ---");
            String query1 = "SELECT id, ten_chien_dich, phan_tram_giam FROM DotGiamGia WHERE phan_tram_giam < 1 OR phan_tram_giam > 40";
            try (ResultSet rs = stmt.executeQuery(query1)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.printf("Invalid DotGiamGia: ID=%d, Name=%s, Percent=%d\n",
                            rs.getInt("id"), rs.getString("ten_chien_dich"), rs.getInt("phan_tram_giam"));
                }
                System.out.println("Total invalid DotGiamGia: " + count);
            }

            // 2. Audit PhieuGiamGia
            System.out.println("--- Auditing PhieuGiamGia ---");
            String query2 = "SELECT id, ma_phieu, gia_tri, don_vi FROM PhieuGiamGia " +
                            "WHERE so_luong_con_lai < 0 OR so_luong_con_lai > 1000000 " +
                            "OR gia_tri <= 0 " +
                            "OR (don_vi = '%' AND (gia_tri < 1 OR gia_tri > 100)) " +
                            "OR (don_vi = 'VND' AND (gia_tri < 1 OR gia_tri > 100000000)) " +
                            "OR gia_tri_don_hang_toi_thieu < 0 OR gia_tri_don_hang_toi_thieu > 100000000 " +
                            "OR (don_vi = '%' AND (gia_tri_giam_toi_da IS NULL OR gia_tri_giam_toi_da < 1 OR gia_tri_giam_toi_da > 100000000)) " +
                            "OR (don_vi = 'VND' AND gia_tri_giam_toi_da IS NOT NULL)";
            try (ResultSet rs = stmt.executeQuery(query2)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.printf("Invalid PhieuGiamGia: ID=%d, Code=%s, Value=%f, Unit=%s\n",
                            rs.getInt("id"), rs.getString("ma_phieu"), rs.getDouble("gia_tri"), rs.getString("don_vi"));
                }
                System.out.println("Total invalid PhieuGiamGia: " + count);
            }

            // 3. Clean up invalid records
            System.out.println("--- Cleaning up invalid records ---");
            int deletedMappings = stmt.executeUpdate(
                "DELETE FROM SanPham_DotGiamGia WHERE id_dot_giam_gia IN (SELECT id FROM DotGiamGia WHERE phan_tram_giam < 1 OR phan_tram_giam > 40)"
            );
            System.out.println("Deleted SanPham_DotGiamGia mappings: " + deletedMappings);

            int deletedDGG = stmt.executeUpdate("DELETE FROM DotGiamGia WHERE phan_tram_giam < 1 OR phan_tram_giam > 40");
            System.out.println("Deleted invalid DotGiamGia: " + deletedDGG);

            int deletedPGG = stmt.executeUpdate(
                "DELETE FROM PhieuGiamGia " +
                "WHERE so_luong_con_lai < 0 OR so_luong_con_lai > 1000000 " +
                "OR gia_tri <= 0 " +
                "OR (don_vi = '%' AND (gia_tri < 1 OR gia_tri > 100)) " +
                "OR (don_vi = 'VND' AND (gia_tri < 1 OR gia_tri > 100000000)) " +
                "OR gia_tri_don_hang_toi_thieu < 0 OR gia_tri_don_hang_toi_thieu > 100000000 " +
                "OR (don_vi = '%' AND (gia_tri_giam_toi_da IS NULL OR gia_tri_giam_toi_da < 1 OR gia_tri_giam_toi_da > 100000000)) " +
                "OR (don_vi = 'VND' AND gia_tri_giam_toi_da IS NOT NULL)"
            );
            System.out.println("Deleted invalid PhieuGiamGia: " + deletedPGG);
        }
    }
}
