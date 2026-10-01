/*
 * Migration V5: Drop legacy unused table LichSuTrangThaiDonHang.
 *
 * In initial database designs, LichSuTrangThaiDonHang was planned for order status history.
 * However, the entire application has migrated to using EditLog (ten_bang = 'HoaDon')
 * for order timeline and audit logging. LichSuTrangThaiDonHang is an orphaned table
 * with no JPA entity, no repository usage, and zero rows.
 */

IF OBJECT_ID(N'dbo.LichSuTrangThaiDonHang', N'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.LichSuTrangThaiDonHang;
END;
