-- Product catalog contract fixture extracted from the original demo catalog.
-- Parsed by ProductAttributeDataContractTest; never executed against a database.
-- Contains only product/category/attribute/image data, without accounts or orders.

INSERT INTO [dbo].[DanhMuc] ([ten_danh_muc],[trang_thai]) VALUES
(N'Vợt cầu lông', 1),
(N'Giày cầu lông', 1),
(N'Áo cầu lông', 1),
(N'Quần cầu lông', 1),
(N'Balo cầu lông', 1),
(N'Túi cầu lông', 1),
(N'Dây cước', 1),
(N'Quấn cán', 1);
GO

INSERT INTO [dbo].[ThuocTinh] ([ten_thuoc_tinh],[trang_thai]) VALUES
(N'Màu sắc', 1),
(N'Độ cứng', 1),
(N'Trọng lượng', 1),
(N'Điểm cân bằng', 1),
(N'Loại người chơi', 1),
(N'Kích thước', 1),
(N'Sức căng', 1);
GO

INSERT INTO [dbo].[DanhMucThuocTinh] ([id_danh_muc],[id_thuoc_tinh],[trang_thai]) VALUES
(1,1,1), (1,2,1), (1,3,1), (1,4,1), (1,5,1), (1,7,1),
(2,1,1), (2,6,1),
(3,1,1), (3,6,1),
(4,1,1), (4,6,1),
(5,1,1),
(6,1,1),
(7,1,1),
(8,1,1);
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Lining Bladex 800 Speed 2026', N'Sản phẩm Lining Bladex 800 Speed 2026 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Axforce 100 Gen 2', N'Sản phẩm Vợt cầu lông Lining Axforce 100 Gen 2 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025', N'Sản phẩm Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Axforce BigBang new', N'Sản phẩm Vợt cầu lông Lining Axforce BigBang new chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Bladex Assassin', N'Sản phẩm Vợt cầu lông Lining Bladex Assassin chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt cầu lông Lining Halbertec 1000 chính hãng', N'Sản phẩm Vợt cầu lông Lining Halbertec 1000 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 2, 1, N'Vợt Cầu Lông Lining Halbertec Motor', N'Sản phẩm Vợt Cầu Lông Lining Halbertec Motor chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt Cầu Lông Mizuno Altair T327', N'Sản phẩm Vợt Cầu Lông Mizuno Altair T327 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt cầu lông Mizuno BDSS Altius Sonic', N'Sản phẩm Vợt cầu lông Mizuno BDSS Altius Sonic chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt cầu lông Mizuno Fortius 55 Strive', N'Sản phẩm Vợt cầu lông Mizuno Fortius 55 Strive chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 4, 1, N'Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng', N'Sản phẩm Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Auraspeed 99 J 2026', N'Sản phẩm Vợt cầu lông Victor Auraspeed 99 J 2026 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor AuraSpeed A', N'Sản phẩm Vợt cầu lông Victor AuraSpeed A chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Auraspeed FANTOME F HYQ', N'Sản phẩm Vợt cầu lông Victor Auraspeed FANTOME F HYQ chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt Cầu Lông Victor AuraSpeed LYC', N'Sản phẩm Vợt Cầu Lông Victor AuraSpeed LYC chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor DriveX 12 WT25', N'Sản phẩm Vợt cầu lông Victor DriveX 12 WT25 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Jetspeed S12 II R', N'Sản phẩm Vợt cầu lông Victor Jetspeed S12 II R chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 3, 1, N'Vợt cầu lông Victor Thruster Hammer Light', N'Sản phẩm Vợt cầu lông Victor Thruster Hammer Light chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 10', N'Sản phẩm Vợt cầu lông Yonex Astrox 10 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 100 Tour VA', N'Sản phẩm Vợt cầu lông Yonex Astrox 100 Tour VA chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng', N'Sản phẩm Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng', N'Sản phẩm Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 99 Pro 2025', N'Sản phẩm Vợt cầu lông Yonex Astrox 99 Pro 2025 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox 99 Tour 2025', N'Sản phẩm Vợt cầu lông Yonex Astrox 99 Tour 2025 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Astrox Lite 43i', N'Sản phẩm Vợt cầu lông Yonex Astrox Lite 43i chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Nanoflare 1000Z', N'Sản phẩm Vợt cầu lông Yonex Nanoflare 1000Z chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (1, 1, 1, N'Vợt cầu lông Yonex Nanoflare 700 Pro 2024', N'Sản phẩm Vợt cầu lông Yonex Nanoflare 700 Pro 2024 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 2, 1, N'Giày cầu lông Lining AYZW007-3 chính hãng', N'Sản phẩm Giày cầu lông Lining AYZW007-3 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 3, 1, N'Giày cầu lông Victor A531 WAG chính hãng', N'Sản phẩm Giày cầu lông Victor A531 WAG chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 3, 1, N'Giày cầu lông Victor A970 cADVAM - Trắng chính hãng', N'Sản phẩm Giày cầu lông Victor A970 cADVAM - Trắng chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 1, 1, N'Giày cầu lông Yonex Eclipsion X3', N'Sản phẩm Giày cầu lông Yonex Eclipsion X3 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 1, 1, N'Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng', N'Sản phẩm Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (2, 1, 1, N'Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng', N'Sản phẩm Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 2, 1, N'Áo cầu lông Lining P-APLUA47-1 nam chính hãng', N'Sản phẩm Áo cầu lông Lining P-APLUA47-1 nam chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 1, 1, N'Áo cầu lông Yonex RM3216 - Poinciana chính hãng', N'Sản phẩm Áo cầu lông Yonex RM3216 - Poinciana chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 1, 1, N'Áo cầu lông Yonex RM3232 - White chính hãng', N'Sản phẩm Áo cầu lông Yonex RM3232 - White chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (3, 3, 1, N'Áo hoodie lót bông Victor Vic07 - Đỏ', N'Sản phẩm Áo hoodie lót bông Victor Vic07 - Đỏ chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần cầu lông Lining 92001 - Đen trắng', N'Sản phẩm Quần cầu lông Lining 92001 - Đen trắng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần cầu lông Lining 9682 - Đen xanh ngọc', N'Sản phẩm Quần cầu lông Lining 9682 - Đen xanh ngọc chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần cầu lông lining nữ đen - mã 081', N'Sản phẩm Quần cầu lông lining nữ đen - mã 081 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 2, 1, N'Quần Cầu Lông Lining training trắng', N'Sản phẩm Quần Cầu Lông Lining training trắng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 1, 1, N'Quần cầu lông Yonex Q3 nữ - Đen trắng', N'Sản phẩm Quần cầu lông Yonex Q3 nữ - Đen trắng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (4, 1, 1, N'Quần cầu lông Yonex TSM3117 - Lion chính hãng', N'Sản phẩm Quần cầu lông Yonex TSM3117 - Lion chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 2, 1, N'Balo cầu lông Lining P-ABSV133-3 chính hãng', N'Sản phẩm Balo cầu lông Lining P-ABSV133-3 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 3, 1, N'Balo cầu lông Victor BR5042 EXA - Trắng đỏ chính hãng', N'Sản phẩm Balo cầu lông Victor BR5042 EXA - Trắng đỏ chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 3, 1, N'Balo cầu lông Victor BR5051 CNY - Trắng đỏ chính hãng', N'Sản phẩm Balo cầu lông Victor BR5051 CNY - Trắng đỏ chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (5, 1, 1, N'Balo cầu lông Yonex BAG525B1212Z', N'Sản phẩm Balo cầu lông Yonex BAG525B1212Z chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 2, 1, N'Túi cầu lông Lining ABJU013-2 chính hãng', N'Sản phẩm Túi cầu lông Lining ABJU013-2 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 2, 1, N'Túi cầu lông Lining P-ABLV029-3 chính hãng', N'Sản phẩm Túi cầu lông Lining P-ABLV029-3 chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 3, 1, N'Túi cầu lông Victor BR5651CNY - Trắng đỏ chính hãng', N'Sản phẩm Túi cầu lông Victor BR5651CNY - Trắng đỏ chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 1, 1, N'Túi cầu lông Yonex BA92026EX xám chính hãng', N'Sản phẩm Túi cầu lông Yonex BA92026EX xám chính hãng chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (6, 1, 1, N'Túi Xách Cầu Lông Yonex 3D 2241R (BKDBL)', N'Sản phẩm Túi Xách Cầu Lông Yonex 3D 2241R (BKDBL) chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 6, 1, N'Dây cước căng vợt Kizuna Z65X', N'Sản phẩm Dây cước căng vợt Kizuna Z65X chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 1, 1, N'Dây cước căng vợt Yonex BG 66 Ultimax', N'Sản phẩm Dây cước căng vợt Yonex BG 66 Ultimax chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 1, 1, N'Dây cước căng vợt Yonex BG SKY', N'Sản phẩm Dây cước căng vợt Yonex BG SKY chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (7, 1, 1, N'Dây cước căng vợt Yonex Nanogy BG 95', N'Sản phẩm Dây cước căng vợt Yonex Nanogy BG 95 chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPham] ([id_danh_muc],[id_thuong_hieu],[id_nhan_vien],[ten_san_pham],[mo_ta],[trang_thai],[so_luot_danh_gia],[diem_trung_binh],[ngay_tao]) VALUES (8, 1, 1, N'Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)', N'Sản phẩm Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn) chính hãng chất lượng cao, nhập khẩu phân phối trực tiếp bởi SMASH-VN.', 1, 0, 0.0, GETDATE());
GO

INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (1, 2500000, 4490000, 30, 1, GETDATE()); -- spct_1
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (2, 2500000, 5650000, 30, 1, GETDATE()); -- spct_2
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (3, 2500000, 5100000, 30, 1, GETDATE()); -- spct_3
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (4, 2500000, 1690000, 30, 1, GETDATE()); -- spct_4
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (5, 2500000, 1300000, 30, 1, GETDATE()); -- spct_5
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (6, 2500000, 880000, 30, 1, GETDATE()); -- spct_6
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (7, 2500000, 1100000, 30, 1, GETDATE()); -- spct_7
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (8, 2500000, 1400000, 30, 1, GETDATE()); -- spct_8
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (9, 2500000, 3756000, 30, 1, GETDATE()); -- spct_9
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (10, 2500000, 3842000, 30, 1, GETDATE()); -- spct_10
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (11, 2500000, 3200000, 30, 1, GETDATE()); -- spct_11
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (12, 2500000, 4190000, 30, 1, GETDATE()); -- spct_12
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (13, 2500000, 1190000, 30, 1, GETDATE()); -- spct_13
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (14, 2500000, 4490000, 30, 1, GETDATE()); -- spct_14
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (15, 2500000, 3900000, 30, 1, GETDATE()); -- spct_15
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (16, 2500000, 4449000, 30, 1, GETDATE()); -- spct_16
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (17, 2500000, 3900000, 30, 1, GETDATE()); -- spct_17
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (18, 2500000, 1250000, 30, 1, GETDATE()); -- spct_18
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (19, 2500000, 939000, 30, 1, GETDATE()); -- spct_19
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (20, 2500000, 4469000, 30, 1, GETDATE()); -- spct_20
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (21, 2500000, 2349000, 30, 1, GETDATE()); -- spct_21
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (22, 2500000, 3200000, 30, 1, GETDATE()); -- spct_22
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (23, 2500000, 5209000, 30, 1, GETDATE()); -- spct_23
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (24, 2500000, 4890000, 30, 1, GETDATE()); -- spct_24
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (25, 2500000, 709000, 20, 1, GETDATE()); -- spct_25
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (25, 2500000, 709000, 20, 1, GETDATE()); -- spct_26
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (26, 2500000, 5099000, 30, 1, GETDATE()); -- spct_27
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (27, 2500000, 4709000, 30, 1, GETDATE()); -- spct_28
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2290000, 20, 1, GETDATE()); -- spct_29
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2290000, 20, 1, GETDATE()); -- spct_30
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2290000, 20, 1, GETDATE()); -- spct_31
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (28, 1500000, 2290000, 20, 1, GETDATE()); -- spct_32
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 1589000, 20, 1, GETDATE()); -- spct_33
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 1589000, 20, 1, GETDATE()); -- spct_34
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 1589000, 20, 1, GETDATE()); -- spct_35
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (29, 1500000, 1589000, 20, 1, GETDATE()); -- spct_36
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_37
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_38
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_39
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (30, 1500000, 2100000, 20, 1, GETDATE()); -- spct_40
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_41
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_42
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_43
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_44
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_45
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_46
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_47
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_48
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_49
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_50
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_51
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (31, 1500000, 2100000, 15, 1, GETDATE()); -- spct_52
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_53
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_54
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_55
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (32, 1500000, 2100000, 20, 1, GETDATE()); -- spct_56
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_57
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_58
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_59
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (33, 1500000, 2100000, 20, 1, GETDATE()); -- spct_60
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 599000, 25, 1, GETDATE()); -- spct_61
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 599000, 25, 1, GETDATE()); -- spct_62
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 599000, 25, 1, GETDATE()); -- spct_63
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (34, 200000, 599000, 25, 1, GETDATE()); -- spct_64
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_65
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_66
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_67
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (35, 200000, 320000, 25, 1, GETDATE()); -- spct_68
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_69
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_70
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_71
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (36, 200000, 320000, 25, 1, GETDATE()); -- spct_72
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_73
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_74
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_75
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (37, 200000, 320000, 25, 1, GETDATE()); -- spct_76
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_77
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_78
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_79
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (38, 200000, 320000, 25, 1, GETDATE()); -- spct_80
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_81
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_82
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_83
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (39, 200000, 320000, 25, 1, GETDATE()); -- spct_84
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_85
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_86
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_87
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (40, 200000, 320000, 25, 1, GETDATE()); -- spct_88
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_89
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_90
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_91
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (41, 200000, 320000, 25, 1, GETDATE()); -- spct_92
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_93
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_94
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_95
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (42, 200000, 320000, 25, 1, GETDATE()); -- spct_96
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 509000, 25, 1, GETDATE()); -- spct_97
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 509000, 25, 1, GETDATE()); -- spct_98
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 509000, 25, 1, GETDATE()); -- spct_99
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (43, 200000, 509000, 25, 1, GETDATE()); -- spct_100
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (44, 500000, 790000, 30, 1, GETDATE()); -- spct_101
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (45, 500000, 790000, 30, 1, GETDATE()); -- spct_102
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (46, 500000, 790000, 30, 1, GETDATE()); -- spct_103
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (47, 200000, 350000, 20, 1, GETDATE()); -- spct_104
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (47, 200000, 350000, 20, 1, GETDATE()); -- spct_105
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (47, 200000, 350000, 20, 1, GETDATE()); -- spct_106
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (48, 500000, 790000, 30, 1, GETDATE()); -- spct_107
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (49, 500000, 790000, 30, 1, GETDATE()); -- spct_108
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (50, 500000, 790000, 30, 1, GETDATE()); -- spct_109
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (51, 500000, 790000, 30, 1, GETDATE()); -- spct_110
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (52, 500000, 790000, 30, 1, GETDATE()); -- spct_111
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (53, 80000, 150000, 30, 1, GETDATE()); -- spct_112
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (54, 80000, 150000, 30, 1, GETDATE()); -- spct_113
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (55, 80000, 150000, 30, 1, GETDATE()); -- spct_114
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (56, 80000, 150000, 30, 1, GETDATE()); -- spct_115
INSERT INTO [dbo].[SanPhamChiTiet] ([id_san_pham],[gia_nhap],[gia_ban],[so_luong_ton],[trang_thai],[ngay_tao]) VALUES (57, 80000, 150000, 30, 1, GETDATE()); -- spct_116

GO

-- 13. SanPhamChiTietThuocTinh
INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (1, 2, N'Cứng'), (1, 3, N'4U'), (1, 4, N'Cân bằng'), (1, 5, N'Phản tạt, phòng thủ'), (1, 7, N'30 - 31 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (2, 1, N'Đen vàng đồng xanh lam'), (2, 2, N'Cứng'), (2, 3, N'4U'), (2, 4, N'Nặng đầu'), (2, 5, N'Tấn công'), (2, 7, N'30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (3, 2, N'Cứng'), (3, 3, N'4U'), (3, 4, N'Nặng đầu'), (3, 5, N'Tấn công'), (3, 7, N'30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (4, 2, N'Dẻo'), (4, 3, N'4U'), (4, 4, N'Nặng đầu'), (4, 5, N'Tấn công'), (4, 7, N'28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (5, 3, N'4U'), (5, 4, N'Nặng đầu'), (5, 5, N'Tấn công');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (6, 2, N'Trung bình'), (6, 3, N'4U'), (6, 4, N'Hơi nặng đầu'), (6, 5, N'Công thủ toàn diện'), (6, 7, N'25 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (7, 2, N'Trung bình'), (7, 3, N'4U'), (7, 5, N'Công thủ toàn diện');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (8, 2, N'Dẻo'), (8, 3, N'5U'), (8, 4, N'Cân bằng'), (8, 5, N'Công thủ toàn diện'), (8, 7, N'30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (9, 1, N'Đen'), (9, 2, N'Trung bình'), (9, 3, N'4U'), (9, 4, N'Cân bằng'), (9, 5, N'Công thủ toàn diện'), (9, 7, N'22 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (10, 1, N'Xanh/Nâu nhạt'), (10, 2, N'Cứng'), (10, 3, N'4U'), (10, 4, N'Nặng đầu'), (10, 5, N'Tấn công'), (10, 7, N'30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (11, 1, N'Đen xanh cam'), (11, 2, N'Cứng'), (11, 3, N'4U'), (11, 4, N'Nặng đầu'), (11, 5, N'Tấn công'), (11, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (12, 2, N'Cứng'), (12, 3, N'4U'), (12, 4, N'Hơi nặng đầu'), (12, 7, N'31 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (13, 2, N'Trung bình'), (13, 3, N'4U'), (13, 4, N'Cân bằng'), (13, 5, N'Công thủ toàn diện');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (14, 1, N'Hồng xanh dương'), (14, 2, N'Trung bình'), (14, 3, N'4U'), (14, 4, N'Nặng đầu'), (14, 7, N'28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (15, 1, N'Xanh cobalt'), (15, 2, N'Cứng'), (15, 3, N'4U'), (15, 4, N'Hơi nặng đầu'), (15, 5, N'Phản tạt, phòng thủ'), (15, 7, N'28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (16, 1, N'Trắng hồng'), (16, 2, N'Cứng'), (16, 3, N'4U'), (16, 4, N'Hơi nặng đầu'), (16, 5, N'Công thủ toàn diện'), (16, 7, N'32 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (17, 2, N'Cứng'), (17, 3, N'3U'), (17, 4, N'Cân bằng'), (17, 7, N'29 - 30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (18, 2, N'Dẻo'), (18, 3, N'5U'), (18, 7, N'28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (19, 2, N'Trung bình'), (19, 3, N'4U');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (20, 1, N'Dark Olive'), (20, 2, N'Cứng'), (20, 3, N'4U'), (20, 4, N'Nặng đầu'), (20, 5, N'Tấn công'), (20, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (21, 1, N'Đen đỏ'), (21, 2, N'Trung bình'), (21, 3, N'3F'), (21, 4, N'Hơi nặng đầu'), (21, 5, N'Phản tạt, phòng thủ'), (21, 7, N'26 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (22, 1, N'Light Beige'), (22, 2, N'Cứng'), (22, 3, N'4U'), (22, 4, N'Nặng đầu'), (22, 5, N'Tấn công'), (22, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (23, 1, N'Black/Green'), (23, 2, N'Cứng'), (23, 3, N'4U'), (23, 4, N'Siêu nặng đầu'), (23, 5, N'Tấn công'), (23, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (24, 1, N'Black/Green'), (24, 2, N'Cứng'), (24, 3, N'4U'), (24, 4, N'Nặng đầu'), (24, 5, N'Tấn công'), (24, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (25, 1, N'Aqua blue'), (25, 2, N'Dẻo'), (25, 3, N'4U'), (25, 4, N'Hơi nặng đầu'), (25, 5, N'Công thủ toàn diện'), (25, 7, N'20 - 30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (26, 1, N'Green'), (26, 2, N'Dẻo'), (26, 3, N'4U'), (26, 4, N'Hơi nặng đầu'), (26, 5, N'Công thủ toàn diện'), (26, 7, N'20 - 30 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (27, 1, N'Đen - Vàng (Lightning Yellow)'), (27, 2, N'Siêu cứng'), (27, 3, N'4U'), (27, 4, N'Nhẹ đầu'), (27, 5, N'Phản tạt, phòng thủ'), (27, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (28, 1, N'Midnight Purple'), (28, 2, N'Trung bình'), (28, 3, N'4U'), (28, 4, N'Cân bằng'), (28, 5, N'Công thủ toàn diện'), (28, 7, N'20 - 28 lbs');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (29, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (30, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (31, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (32, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (33, 1, N'Trắng'), (33, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (34, 1, N'Trắng'), (34, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (35, 1, N'Trắng'), (35, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (36, 1, N'Trắng'), (36, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (37, 1, N'Trắng'), (37, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (38, 1, N'Trắng'), (38, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (39, 1, N'Trắng'), (39, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (40, 1, N'Trắng'), (40, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (41, 1, N'Trắng'), (41, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (42, 1, N'Trắng'), (42, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (43, 1, N'Trắng'), (43, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (44, 1, N'Trắng'), (44, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (45, 1, N'Trắng đen'), (45, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (46, 1, N'Trắng đen'), (46, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (47, 1, N'Trắng đen'), (47, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (48, 1, N'Trắng đen'), (48, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (49, 1, N'Xanh Navy'), (49, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (50, 1, N'Xanh Navy'), (50, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (51, 1, N'Xanh Navy'), (51, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (52, 1, N'Xanh Navy'), (52, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (53, 1, N'Grayish Beige'), (53, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (54, 1, N'Grayish Beige'), (54, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (55, 1, N'Grayish Beige'), (55, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (56, 1, N'Grayish Beige'), (56, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (57, 1, N'Crystal Teal'), (57, 6, N'39');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (58, 1, N'Crystal Teal'), (58, 6, N'40');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (59, 1, N'Crystal Teal'), (59, 6, N'41');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (60, 1, N'Crystal Teal'), (60, 6, N'42');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (61, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (62, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (63, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (64, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (65, 1, N'Poinciana'), (65, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (66, 1, N'Poinciana'), (66, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (67, 1, N'Poinciana'), (67, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (68, 1, N'Poinciana'), (68, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (69, 1, N'White'), (69, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (70, 1, N'White'), (70, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (71, 1, N'White'), (71, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (72, 1, N'White'), (72, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (73, 1, N'Đỏ'), (73, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (74, 1, N'Đỏ'), (74, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (75, 1, N'Đỏ'), (75, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (76, 1, N'Đỏ'), (76, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (77, 1, N'Đen trắng'), (77, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (78, 1, N'Đen trắng'), (78, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (79, 1, N'Đen trắng'), (79, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (80, 1, N'Đen trắng'), (80, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (81, 1, N'Đen xanh ngọc'), (81, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (82, 1, N'Đen xanh ngọc'), (82, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (83, 1, N'Đen xanh ngọc'), (83, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (84, 1, N'Đen xanh ngọc'), (84, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (85, 1, N'Đen'), (85, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (86, 1, N'Đen'), (86, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (87, 1, N'Đen'), (87, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (88, 1, N'Đen'), (88, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (89, 1, N'Trắng'), (89, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (90, 1, N'Trắng'), (90, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (91, 1, N'Trắng'), (91, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (92, 1, N'Trắng'), (92, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (93, 1, N'Đen trắng'), (93, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (94, 1, N'Đen trắng'), (94, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (95, 1, N'Đen trắng'), (95, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (96, 1, N'Đen trắng'), (96, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (97, 1, N'Lion'), (97, 6, N'S');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (98, 1, N'Lion'), (98, 6, N'M');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (99, 1, N'Lion'), (99, 6, N'L');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (100, 1, N'Lion'), (100, 6, N'XL');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (104, 1, N'Bright White');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (105, 1, N'Jet Black');
GO

INSERT INTO [dbo].[SanPhamChiTietThuocTinh] ([id_san_pham_chi_tiet],[id_thuoc_tinh],[gia_tri]) VALUES (106, 1, N'Riviera');
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (1, N'Vợt cầu lông/Li-Ning/Lining Bladex 800 Speed 2026/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (1, N'Vợt cầu lông/Li-Ning/Lining Bladex 800 Speed 2026/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (1, N'Vợt cầu lông/Li-Ning/Lining Bladex 800 Speed 2026/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (2, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 100 Gen 2/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (2, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 100 Gen 2/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (2, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 100 Gen 2/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (3, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025/anh1.png', N'Loh Kean Yew 2025', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (3, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025/anh2.png', N'Loh Kean Yew 2025', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (3, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce 90 New - Loh Kean Yew 2025/anh3.png', N'Loh Kean Yew 2025', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (4, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce BigBang new/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (4, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce BigBang new/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (4, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Axforce BigBang new/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (5, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Bladex Assassin/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (5, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Bladex Assassin/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (5, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Bladex Assassin/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (6, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Halbertec 1000 chính hãng/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (6, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Halbertec 1000 chính hãng/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (6, N'Vợt cầu lông/Li-Ning/Vợt cầu lông Lining Halbertec 1000 chính hãng/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (7, N'Vợt cầu lông/Li-Ning/Vợt Cầu Lông Lining Halbertec Motor/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (7, N'Vợt cầu lông/Li-Ning/Vợt Cầu Lông Lining Halbertec Motor/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (7, N'Vợt cầu lông/Li-Ning/Vợt Cầu Lông Lining Halbertec Motor/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (8, N'Vợt cầu lông/Mizuno/Vợt Cầu Lông Mizuno Altair T327/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (8, N'Vợt cầu lông/Mizuno/Vợt Cầu Lông Mizuno Altair T327/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (9, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno BDSS Altius Sonic/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (9, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno BDSS Altius Sonic/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (9, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno BDSS Altius Sonic/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (10, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno Fortius 55 Strive/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (10, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno Fortius 55 Strive/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (10, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno Fortius 55 Strive/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh.png', N'Đen xanh cam chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh1.png', N'Đen xanh cam chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh2.png', N'Đen xanh cam chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (11, N'Vợt cầu lông/Mizuno/Vợt cầu lông Mizuno JPX 8.1 Pro - Đen xanh cam chính hãng/anh3.png', N'Đen xanh cam chính hãng', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (12, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed 99 J 2026/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (12, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed 99 J 2026/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (12, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed 99 J 2026/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (13, N'Vợt cầu lông/Victor/Vợt cầu lông Victor AuraSpeed A/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (13, N'Vợt cầu lông/Victor/Vợt cầu lông Victor AuraSpeed A/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (13, N'Vợt cầu lông/Victor/Vợt cầu lông Victor AuraSpeed A/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (14, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed FANTOME F HYQ/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (14, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed FANTOME F HYQ/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (14, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Auraspeed FANTOME F HYQ/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (15, N'Vợt cầu lông/Victor/Vợt Cầu Lông Victor AuraSpeed LYC/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (15, N'Vợt cầu lông/Victor/Vợt Cầu Lông Victor AuraSpeed LYC/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (15, N'Vợt cầu lông/Victor/Vợt Cầu Lông Victor AuraSpeed LYC/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (16, N'Vợt cầu lông/Victor/Vợt cầu lông Victor DriveX 12 WT25/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (16, N'Vợt cầu lông/Victor/Vợt cầu lông Victor DriveX 12 WT25/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (16, N'Vợt cầu lông/Victor/Vợt cầu lông Victor DriveX 12 WT25/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (17, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Jetspeed S12 II R/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (17, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Jetspeed S12 II R/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (17, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Jetspeed S12 II R/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (18, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Thruster Hammer Light/anh1.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (18, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Thruster Hammer Light/anh2.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (18, N'Vợt cầu lông/Victor/Vợt cầu lông Victor Thruster Hammer Light/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (19, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 10/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (19, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 10/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (20, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 100 Tour VA/anh3.png', N'Màu mặc định', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (21, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (21, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (21, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 22 Lite (BKRD) chính hãng/anh3.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh.png', N'Light Beige chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh1.png', N'Light Beige chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh2.png', N'Light Beige chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (22, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 77 Play Limited - Light Beige chính hãng/anh3.png', N'Light Beige chính hãng', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (23, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Pro 2025/anh3.png', N'Màu mặc định', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (24, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox 99 Tour 2025/anh3.png', N'Màu mặc định', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh.png', N'Aqua blue', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh1.png', N'Aqua blue', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh2.png', N'Aqua blue', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (25, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Aqua blue/anh3.png', N'Aqua blue', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (26, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Green/anh.png', N'Green', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (26, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Green/anh1.png', N'Green', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (26, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Astrox Lite 43i/Green/anh2.png', N'Green', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (27, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 1000Z/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (27, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 1000Z/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (27, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 1000Z/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (28, N'Vợt cầu lông/Yonex/Vợt cầu lông Yonex Nanoflare 700 Pro 2024/anh3.png', N'Màu mặc định', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh.png', N'3 chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh1.png', N'3 chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh2.png', N'3 chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh3.png', N'3 chính hãng', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (29, N'Giày/Giày cầu lông Lining AYZW007-3 chính hãng/anh4.png', N'3 chính hãng', 0, 5);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (33, N'Giày/Giày cầu lông Victor A531 WAG chính hãng/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (33, N'Giày/Giày cầu lông Victor A531 WAG chính hãng/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (33, N'Giày/Giày cầu lông Victor A531 WAG chính hãng/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (37, N'Giày/Giày cầu lông Victor A970 cADVAM - Trắng chính hãng/anh.png', N'Trắng chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (37, N'Giày/Giày cầu lông Victor A970 cADVAM - Trắng chính hãng/anh1.png', N'Trắng chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (37, N'Giày/Giày cầu lông Victor A970 cADVAM - Trắng chính hãng/anh2.png', N'Trắng chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (41, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng/anh.png', N'Trắng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (41, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng/anh1.png', N'Trắng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (41, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng/anh2.png', N'Trắng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (45, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng đen/anh.png', N'Trắng đen', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (45, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng đen/anh1.png', N'Trắng đen', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (45, N'Giày/Giày cầu lông Yonex Eclipsion X3/Trắng đen/anh2.png', N'Trắng đen', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (49, N'Giày/Giày cầu lông Yonex Eclipsion X3/Xanh NaVy/anh.png', N'Xanh NaVy', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (49, N'Giày/Giày cầu lông Yonex Eclipsion X3/Xanh NaVy/anh1.png', N'Xanh NaVy', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh.png', N'Grayish Beige chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh1.png', N'Grayish Beige chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh2.png', N'Grayish Beige chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh3.png', N'Grayish Beige chính hãng', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (53, N'Giày/Giày cầu lông Yonex SHB 65Z VA Men - Grayish Beige chính hãng/anh4.png', N'Grayish Beige chính hãng', 0, 5);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (57, N'Giày/Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng/anh.png', N'Crystal teal chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (57, N'Giày/Giày cầu lông Yonex Tokyo 4 - Crystal teal chính hãng/anh1.png', N'Crystal teal chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (61, N'Áo/Áo cầu lông Lining P-APLUA47-1 nam chính hãng/anh.png', N'1 nam chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (61, N'Áo/Áo cầu lông Lining P-APLUA47-1 nam chính hãng/anh1.png', N'1 nam chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (61, N'Áo/Áo cầu lông Lining P-APLUA47-1 nam chính hãng/anh2.png', N'1 nam chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (65, N'Áo/Áo cầu lông Yonex RM3216 - Poinciana chính hãng/anh.png', N'Poinciana chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (69, N'Áo/Áo cầu lông Yonex RM3232 - White chính hãng/anh.png', N'White chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (73, N'Áo/Áo hoodie lót bông Victor Vic07 - Đỏ/anh.png', N'Đỏ', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (77, N'Quần/Quần cầu lông Lining 92001 - Đen trắng/anh.png', N'Đen trắng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (81, N'Quần/Quần cầu lông Lining 9682 - Đen xanh ngọc/anh.png', N'Đen xanh ngọc', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (85, N'Quần/Quần cầu lông lining nữ đen - mã 081/anh.png', N'mã 081', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (89, N'Quần/Quần Cầu Lông Lining training trắng/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (93, N'Quần/Quần cầu lông Yonex Q3 nữ - Đen trắng/anh.png', N'Đen trắng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (97, N'Quần/Quần cầu lông Yonex TSM3117 - Lion chính hãng/anh.png', N'Lion chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (97, N'Quần/Quần cầu lông Yonex TSM3117 - Lion chính hãng/anh1.png', N'Lion chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (97, N'Quần/Quần cầu lông Yonex TSM3117 - Lion chính hãng/anh3.png', N'Lion chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (101, N'Balo/Balo cầu lông Lining P-ABSV133-3 chính hãng/anh.png', N'3 chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (102, N'Balo/Balo cầu lông Victor BR5042 EXA - Trắng đỏ chính hãng/anh.png', N'Trắng đỏ chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (103, N'Balo/Balo cầu lông Victor BR5051 CNY - Trắng đỏ chính hãng/anh.png', N'Trắng đỏ chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (104, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Bright White/anh.png', N'Bright White', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (104, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Bright White/anh1.png', N'Bright White', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (105, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Jet Black/anh.png', N'Jet Black', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (105, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Jet Black/anh1.png', N'Jet Black', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (106, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Riviera/anh.png', N'Riviera', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (106, N'Balo/Balo cầu lông Yonex BAG525B1212Z/Riviera/anh1.png', N'Riviera', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (107, N'Túi Xách/Túi cầu lông Lining ABJU013-2 chính hãng/anh.png', N'2 chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (107, N'Túi Xách/Túi cầu lông Lining ABJU013-2 chính hãng/anh1.png', N'2 chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (108, N'Túi Xách/Túi cầu lông Lining P-ABLV029-3 chính hãng/anh.png', N'3 chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (108, N'Túi Xách/Túi cầu lông Lining P-ABLV029-3 chính hãng/anh1.png', N'3 chính hãng', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (108, N'Túi Xách/Túi cầu lông Lining P-ABLV029-3 chính hãng/anh2.png', N'3 chính hãng', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (109, N'Túi Xách/Túi cầu lông Victor BR5651CNY - Trắng đỏ chính hãng/anh.png', N'Trắng đỏ chính hãng', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (110, N'Túi Xách/Túi cầu lông Yonex BA92026EX xám chính hãng/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (111, N'Túi Xách/Túi Xách Cầu Lông Yonex 3D 2241R (BKDBL)/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt Kizuna Z65X/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt Kizuna Z65X/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt Kizuna Z65X/anh2.png', N'Màu mặc định', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt Kizuna Z65X/anh3.png', N'Màu mặc định', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt Kizuna Z65X/anh4.png', N'Màu mặc định', 0, 5);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (112, N'Cước/Dây cước căng vợt Kizuna Z65X/anh5.png', N'Màu mặc định', 0, 6);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Yonex BG 66 Ultimax/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (113, N'Cước/Dây cước căng vợt Yonex BG 66 Ultimax/anh1.png', N'Màu mặc định', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (114, N'Cước/Dây cước căng vợt Yonex BG SKY/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (115, N'Cước/Dây cước căng vợt Yonex Nanogy BG 95/anh.png', N'Màu mặc định', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh.png', N'30 EX (Túi 2 cuộn)', 1, 1);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh1.png', N'30 EX (Túi 2 cuộn)', 0, 2);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh2.png', N'30 EX (Túi 2 cuộn)', 0, 3);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh3.png', N'30 EX (Túi 2 cuộn)', 0, 4);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh4.png', N'30 EX (Túi 2 cuộn)', 0, 5);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh5.png', N'30 EX (Túi 2 cuộn)', 0, 6);
GO

INSERT INTO [dbo].[HinhAnhSanPham] ([id_san_pham_chi_tiet],[url_hinh_anh],[mau_sac],[la_anh_chinh],[thu_tu]) VALUES (116, N'Quấn cán/Quấn cán Yonex xịn AC102-30 EX (Túi 2 cuộn)/anh6.png', N'30 EX (Túi 2 cuộn)', 0, 7);
GO
