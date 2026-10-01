/*
 * Migration V4: Drop legacy id_trang_thai column and constraints from GioHangChiTiet.
 *
 * In earlier database revisions, GioHangChiTiet had a NOT NULL column id_trang_thai
 * referencing TrangThaiGioHang. In the modern domain model, cart items do not maintain
 * status (order status is handled on HoaDon / HoaDonChiTiet), and GioHangChiTiet entity
 * does not map id_trang_thai. Leaving NOT NULL on id_trang_thai causes INSERT failures
 * when adding items to the cart.
 */

IF OBJECT_ID(N'dbo.GioHangChiTiet', N'U') IS NOT NULL
BEGIN
    -- 1. Drop foreign key constraints on dbo.GioHangChiTiet referencing id_trang_thai
    DECLARE @fkName sysname;
    DECLARE @dropFkSql nvarchar(1000);

    DECLARE fk_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT fk.name
        FROM sys.foreign_keys fk
        INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
        INNER JOIN sys.columns c ON fkc.parent_object_id = c.object_id AND fkc.parent_column_id = c.column_id
        WHERE fk.parent_object_id = OBJECT_ID(N'dbo.GioHangChiTiet')
          AND c.name = N'id_trang_thai';

    OPEN fk_cursor;
    FETCH NEXT FROM fk_cursor INTO @fkName;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @dropFkSql = N'ALTER TABLE dbo.GioHangChiTiet DROP CONSTRAINT ' + QUOTENAME(@fkName) + N';';
        EXEC sys.sp_executesql @dropFkSql;
        FETCH NEXT FROM fk_cursor INTO @fkName;
    END;

    CLOSE fk_cursor;
    DEALLOCATE fk_cursor;

    -- 2. Drop default constraints on dbo.GioHangChiTiet for id_trang_thai
    DECLARE @dfName sysname;
    DECLARE @dropDfSql nvarchar(1000);

    DECLARE df_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT dc.name
        FROM sys.default_constraints dc
        INNER JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.GioHangChiTiet')
          AND c.name = N'id_trang_thai';

    OPEN df_cursor;
    FETCH NEXT FROM df_cursor INTO @dfName;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @dropDfSql = N'ALTER TABLE dbo.GioHangChiTiet DROP CONSTRAINT ' + QUOTENAME(@dfName) + N';';
        EXEC sys.sp_executesql @dropDfSql;
        FETCH NEXT FROM df_cursor INTO @dfName;
    END;

    CLOSE df_cursor;
    DEALLOCATE df_cursor;

    -- 3. Drop check constraints on dbo.GioHangChiTiet for id_trang_thai if any
    DECLARE @ckName sysname;
    DECLARE @dropCkSql nvarchar(1000);

    DECLARE ck_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT cc.name
        FROM sys.check_constraints cc
        WHERE cc.parent_object_id = OBJECT_ID(N'dbo.GioHangChiTiet')
          AND cc.definition LIKE N'%id_trang_thai%';

    OPEN ck_cursor;
    FETCH NEXT FROM ck_cursor INTO @ckName;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @dropCkSql = N'ALTER TABLE dbo.GioHangChiTiet DROP CONSTRAINT ' + QUOTENAME(@ckName) + N';';
        EXEC sys.sp_executesql @dropCkSql;
        FETCH NEXT FROM ck_cursor INTO @ckName;
    END;

    CLOSE ck_cursor;
    DEALLOCATE ck_cursor;

    -- 4. Drop indexes on dbo.GioHangChiTiet for id_trang_thai if any
    DECLARE @ixName sysname;
    DECLARE @dropIxSql nvarchar(1000);

    DECLARE ix_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT i.name
        FROM sys.indexes i
        INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
        INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
        WHERE i.object_id = OBJECT_ID(N'dbo.GioHangChiTiet')
          AND c.name = N'id_trang_thai'
          AND i.is_primary_key = 0;

    OPEN ix_cursor;
    FETCH NEXT FROM ix_cursor INTO @ixName;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @dropIxSql = N'DROP INDEX ' + QUOTENAME(@ixName) + N' ON dbo.GioHangChiTiet;';
        EXEC sys.sp_executesql @dropIxSql;
        FETCH NEXT FROM ix_cursor INTO @ixName;
    END;

    CLOSE ix_cursor;
    DEALLOCATE ix_cursor;

    -- 5. Drop the column id_trang_thai if it exists
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.GioHangChiTiet')
          AND name = N'id_trang_thai'
    )
    BEGIN
        ALTER TABLE dbo.GioHangChiTiet DROP COLUMN id_trang_thai;
    END;
END;

-- 6. Clean up obsolete table TrangThaiGioHang if present and unused
IF OBJECT_ID(N'dbo.TrangThaiGioHang', N'U') IS NOT NULL
BEGIN
    -- Check if any other FK references dbo.TrangThaiGioHang
    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE referenced_object_id = OBJECT_ID(N'dbo.TrangThaiGioHang')
    )
    BEGIN
        DROP TABLE dbo.TrangThaiGioHang;
    END;
END;
