/*
 * Migration V3: Thêm mã sản phẩm (ma_san_pham) và SKU biến thể (sku) tự sinh.
 * - Cho phép NULL tạm thời ở schema level để tương thích luồng INSERT -> DB cấp ID -> sinh mã -> UPDATE.
 * - Backfill toàn bộ dữ liệu hiện có bằng thuật toán padding có điều kiện (chống truncate khi ID >= 1000000).
 * - Kiểm tra tính toàn vẹn (validate NULL và duplicate).
 * - Tạo Filtered Unique Index trên SQL Server để đảm bảo tính duy nhất tuyệt đối cho các giá trị NOT NULL.
 */

-- 1. Thêm cột ma_san_pham vào bảng SanPham nếu chưa tồn tại
IF OBJECT_ID(N'dbo.SanPham', N'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns 
        WHERE object_id = OBJECT_ID(N'dbo.SanPham') 
          AND name = N'ma_san_pham'
    )
    BEGIN
        ALTER TABLE dbo.SanPham ADD ma_san_pham NVARCHAR(20) NULL;
    END;
END;

-- 2. Thêm cột sku vào bảng SanPhamChiTiet nếu chưa tồn tại
IF OBJECT_ID(N'dbo.SanPhamChiTiet', N'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns 
        WHERE object_id = OBJECT_ID(N'dbo.SanPhamChiTiet') 
          AND name = N'sku'
    )
    BEGIN
        ALTER TABLE dbo.SanPhamChiTiet ADD sku NVARCHAR(40) NULL;
    END;
END;

-- 3. Backfill ma_san_pham cho SanPham (chống truncate ID >= 1000000)
IF OBJECT_ID(N'dbo.SanPham', N'U') IS NOT NULL
BEGIN
    EXEC sys.sp_executesql N'
        UPDATE dbo.SanPham
        SET ma_san_pham = CASE 
            WHEN id < 1000000 THEN ''SP'' + RIGHT(''000000'' + CAST(id AS VARCHAR(20)), 6)
            ELSE ''SP'' + CAST(id AS VARCHAR(20))
        END
        WHERE ma_san_pham IS NULL OR LTRIM(RTRIM(ma_san_pham)) = '''';
    ';
END;

-- 4. Backfill sku cho SanPhamChiTiet (chống truncate ID >= 1000000)
IF OBJECT_ID(N'dbo.SanPhamChiTiet', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.SanPham', N'U') IS NOT NULL
BEGIN
    EXEC sys.sp_executesql N'
        UPDATE spct
        SET spct.sku = CASE 
            WHEN spct.id < 1000000 THEN sp.ma_san_pham + ''-V'' + RIGHT(''000000'' + CAST(spct.id AS VARCHAR(20)), 6)
            ELSE sp.ma_san_pham + ''-V'' + CAST(spct.id AS VARCHAR(20))
        END
        FROM dbo.SanPhamChiTiet spct
        INNER JOIN dbo.SanPham sp ON spct.id_san_pham = sp.id
        WHERE spct.sku IS NULL OR LTRIM(RTRIM(spct.sku)) = '''';
    ';
END;

-- 5. Validation kiểm tra tính hợp lệ dữ liệu
IF OBJECT_ID(N'dbo.SanPham', N'U') IS NOT NULL
BEGIN
    EXEC sys.sp_executesql N'
        IF EXISTS (SELECT 1 FROM dbo.SanPham WHERE ma_san_pham IS NULL OR LTRIM(RTRIM(ma_san_pham)) = '''')
        BEGIN
            THROW 50001, ''Migration V3 failed: Phát hiện bản ghi SanPham có ma_san_pham NULL sau khi backfill.'', 1;
        END;

        IF EXISTS (
            SELECT ma_san_pham 
            FROM dbo.SanPham 
            WHERE ma_san_pham IS NOT NULL 
            GROUP BY ma_san_pham 
            HAVING COUNT(*) > 1
        )
        BEGIN
            THROW 50002, ''Migration V3 failed: Phát hiện trùng lặp ma_san_pham trong bảng SanPham.'', 1;
        END;
    ';
END;

IF OBJECT_ID(N'dbo.SanPhamChiTiet', N'U') IS NOT NULL
BEGIN
    EXEC sys.sp_executesql N'
        IF EXISTS (SELECT 1 FROM dbo.SanPhamChiTiet WHERE sku IS NULL OR LTRIM(RTRIM(sku)) = '''')
        BEGIN
            THROW 50003, ''Migration V3 failed: Phát hiện bản ghi SanPhamChiTiet có sku NULL sau khi backfill.'', 1;
        END;

        IF EXISTS (
            SELECT sku 
            FROM dbo.SanPhamChiTiet 
            WHERE sku IS NOT NULL 
            GROUP BY sku 
            HAVING COUNT(*) > 1
        )
        BEGIN
            THROW 50004, ''Migration V3 failed: Phát hiện trùng lặp sku trong bảng SanPhamChiTiet.'', 1;
        END;
    ';
END;

-- 6. Tạo Filtered Unique Index cho ma_san_pham và sku
IF OBJECT_ID(N'dbo.SanPham', N'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes 
        WHERE name = N'UX_SanPham_MaSanPham' 
          AND object_id = OBJECT_ID(N'dbo.SanPham')
    )
    BEGIN
        EXEC sys.sp_executesql N'
            CREATE UNIQUE INDEX UX_SanPham_MaSanPham 
            ON dbo.SanPham(ma_san_pham) 
            WHERE ma_san_pham IS NOT NULL;
        ';
    END;
END;

IF OBJECT_ID(N'dbo.SanPhamChiTiet', N'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes 
        WHERE name = N'UX_SanPhamChiTiet_Sku' 
          AND object_id = OBJECT_ID(N'dbo.SanPhamChiTiet')
    )
    BEGIN
        EXEC sys.sp_executesql N'
            CREATE UNIQUE INDEX UX_SanPhamChiTiet_Sku 
            ON dbo.SanPhamChiTiet(sku) 
            WHERE sku IS NOT NULL;
        ';
    END;
END;
