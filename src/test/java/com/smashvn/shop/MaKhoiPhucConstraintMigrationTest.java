package com.smashvn.shop;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GuestCheckoutService;
import com.smashvn.shop.service.user.UserQuenMatKhauService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MaKhoiPhucConstraintMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenKhoiPhucRepository tokenRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private GuestCheckoutService guestCheckoutService;

    @Autowired
    private UserQuenMatKhauService quenMatKhauService;

    @Test
    @Order(1)
    public void step1_inspectBeforeAlter() {
        System.out.println("================================================================================");
        System.out.println("STEP 1: INSPECT CURRENT CHECK CONSTRAINTS & METADATA BEFORE ALTER");
        System.out.println("================================================================================");

        // 1. Current Database Name
        String currentDb = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
        System.out.println("[DATABASE NAME]: " + currentDb);

        // 2. Check column data type and length
        List<Map<String, Object>> colMeta = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'MaKhoiPhuc' AND COLUMN_NAME = 'loai_xac_nhan'");
        System.out.println("[COLUMN METADATA for loai_xac_nhan]: " + colMeta);

        // 3. Check existing CHECK constraints on dbo.MaKhoiPhuc
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT cc.name, cc.definition " +
                "FROM sys.check_constraints cc " +
                "WHERE cc.parent_object_id = OBJECT_ID('dbo.MaKhoiPhuc')");
        System.out.println("[EXISTING CHECK CONSTRAINTS]: " + constraints.size());
        for (Map<String, Object> c : constraints) {
            System.out.println("  - Constraint Name: " + c.get("name") + " | Definition: " + c.get("definition"));
        }

        // 4. Check existing distinct loai_xac_nhan
        List<Map<String, Object>> distinctTypes = jdbcTemplate.queryForList(
                "SELECT loai_xac_nhan, COUNT(*) AS count_val " +
                "FROM dbo.MaKhoiPhuc " +
                "GROUP BY loai_xac_nhan");
        System.out.println("[DISTINCT loai_xac_nhan in DB]: " + distinctTypes);
    }

    @Test
    @Order(2)
    public void step2_alterCheckConstraint() {
        System.out.println("================================================================================");
        System.out.println("STEP 2: ENSURE CK_MaKhoiPhuc_LoaiXacNhan IS PRESENT (IDEMPOTENT GUARD)");
        System.out.println("================================================================================");

        List<Map<String, Object>> existingConstraints = jdbcTemplate.queryForList(
                "SELECT cc.name, cc.definition " +
                "FROM sys.check_constraints cc " +
                "WHERE cc.parent_object_id = OBJECT_ID('dbo.MaKhoiPhuc') " +
                "  AND cc.name = 'CK_MaKhoiPhuc_LoaiXacNhan'");

        if (!existingConstraints.isEmpty()) {
            System.out.println("[IDEMPOTENT]: CK_MaKhoiPhuc_LoaiXacNhan already exists on dbo.MaKhoiPhuc. Skipping DDL execution.");
            return;
        }

        // 1. Find and drop any existing check constraint on dbo.MaKhoiPhuc referencing loai_xac_nhan
        List<String> constraintNames = jdbcTemplate.queryForList(
                "SELECT cc.name " +
                "FROM sys.check_constraints cc " +
                "WHERE cc.parent_object_id = OBJECT_ID('dbo.MaKhoiPhuc') " +
                "  AND cc.definition LIKE '%loai_xac_nhan%'",
                String.class);

        for (String cName : constraintNames) {
            System.out.println("[DROPPING CONSTRAINT]: " + cName);
            jdbcTemplate.execute("ALTER TABLE dbo.MaKhoiPhuc DROP CONSTRAINT [" + cName + "]");
        }

        // 2. Add the new named constraint CK_MaKhoiPhuc_LoaiXacNhan
        System.out.println("[ADDING NEW CONSTRAINT]: CK_MaKhoiPhuc_LoaiXacNhan");
        jdbcTemplate.execute(
                "ALTER TABLE dbo.MaKhoiPhuc WITH CHECK ADD CONSTRAINT [CK_MaKhoiPhuc_LoaiXacNhan] " +
                "CHECK (loai_xac_nhan IN ('EMAIL', 'SMS', 'GUEST_ACTIVATION', 'FORGOT_PASSWORD'))");

        System.out.println("[MIGRATION COMPLETED SUCCESSFULLY]");
    }

    @Test
    @Order(3)
    public void step3_verifyAfterAlter() {
        System.out.println("================================================================================");
        System.out.println("STEP 3: VERIFY NEW CHECK CONSTRAINT AFTER ALTER");
        System.out.println("================================================================================");

        List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT cc.name, cc.definition " +
                "FROM sys.check_constraints cc " +
                "WHERE cc.parent_object_id = OBJECT_ID('dbo.MaKhoiPhuc')");

        System.out.println("[CHECK CONSTRAINTS AFTER ALTER]: " + constraints.size());
        boolean foundNewConstraint = false;
        for (Map<String, Object> c : constraints) {
            System.out.println("  - Constraint Name: " + c.get("name") + " | Definition: " + c.get("definition"));
            if ("CK_MaKhoiPhuc_LoaiXacNhan".equalsIgnoreCase((String) c.get("name"))) {
                foundNewConstraint = true;
            }
        }
        assertTrue(foundNewConstraint, "Constraint CK_MaKhoiPhuc_LoaiXacNhan should exist");
    }

    @Test
    @Order(4)
    public void step4_testBusinessLevelInserts() {
        System.out.println("================================================================================");
        System.out.println("STEP 4: TEST BUSINESS-LEVEL INSERTS FOR GUEST_ACTIVATION AND FORGOT_PASSWORD");
        System.out.println("================================================================================");

        // Find or create test account
        TaiKhoan tk = taiKhoanRepository.findByUsername("test_token_user@smashvn.com");
        if (tk == null) {
            tk = new TaiKhoan();
            tk.setUsername("test_token_user@smashvn.com");
            tk.setMatKhau(null);
            tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
            tk.setVaiTro("KH");
            tk.setSoLanMuaThanhCong(0);
            tk = taiKhoanRepository.save(tk);
        }

        // Test A: Insert GUEST_ACTIVATION token
        System.out.println("[TEST A]: Inserting GUEST_ACTIVATION token...");
        TokenKhoiPhuc guestToken = new TokenKhoiPhuc();
        guestToken.setTaiKhoan(tk);
        guestToken.setMaXacNhan(UUID.randomUUID().toString());
        guestToken.setLoaiXacNhan("GUEST_ACTIVATION");
        guestToken.setThoiGianHetHan(LocalDateTime.now().plusDays(30));
        guestToken.setDaSuDung(false);

        TokenKhoiPhuc savedGuestToken = tokenRepository.save(guestToken);
        assertNotNull(savedGuestToken.getId(), "GUEST_ACTIVATION token should have generated ID");
        assertEquals("GUEST_ACTIVATION", savedGuestToken.getLoaiXacNhan());
        System.out.println("[TEST A - RESULT]: GUEST_ACTIVATION token inserted successfully with ID: " + savedGuestToken.getId());

        // Test B: Insert FORGOT_PASSWORD token
        System.out.println("[TEST B]: Inserting FORGOT_PASSWORD token...");
        TokenKhoiPhuc forgotToken = new TokenKhoiPhuc();
        forgotToken.setTaiKhoan(tk);
        forgotToken.setMaXacNhan(UUID.randomUUID().toString());
        forgotToken.setLoaiXacNhan("FORGOT_PASSWORD");
        forgotToken.setThoiGianHetHan(LocalDateTime.now().plusMinutes(15));
        forgotToken.setDaSuDung(false);

        TokenKhoiPhuc savedForgotToken = tokenRepository.save(forgotToken);
        assertNotNull(savedForgotToken.getId(), "FORGOT_PASSWORD token should have generated ID");
        assertEquals("FORGOT_PASSWORD", savedForgotToken.getLoaiXacNhan());
        System.out.println("[TEST B - RESULT]: FORGOT_PASSWORD token inserted successfully with ID: " + savedForgotToken.getId());
    }

    @Test
    @Order(5)
    public void step5_inspectAndCleanupTestData() {
        System.out.println("================================================================================");
        System.out.println("STEP 5: INSPECT COUNTS & CLEANUP TEST DATA");
        System.out.println("================================================================================");

        List<Map<String, Object>> distinctCounts = jdbcTemplate.queryForList(
                "SELECT loai_xac_nhan, COUNT(*) AS total_count " +
                "FROM dbo.MaKhoiPhuc " +
                "GROUP BY loai_xac_nhan");
        System.out.println("[SUMMARY COUNTS BY loai_xac_nhan BEFORE CLEANUP]:");
        for (Map<String, Object> row : distinctCounts) {
            System.out.println("  - " + row.get("loai_xac_nhan") + ": " + row.get("total_count"));
        }

        // Cleanup test data to prevent database pollution
        TaiKhoan tk = taiKhoanRepository.findByUsername("test_token_user@smashvn.com");
        if (tk != null) {
            int deletedTokens = jdbcTemplate.update("DELETE FROM dbo.MaKhoiPhuc WHERE id_tai_khoan = ?", tk.getId());
            int deletedAccounts = jdbcTemplate.update("DELETE FROM dbo.TaiKhoan WHERE id = ?", tk.getId());
            System.out.println("[CLEANUP COMPLETED]: Deleted " + deletedTokens + " test tokens and " + deletedAccounts + " test account.");
        }
    }
}
