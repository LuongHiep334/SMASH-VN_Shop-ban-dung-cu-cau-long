-- SMASH-VN: schema baseline, without customer data or credentials.
-- Run in SSMS against a NEW database. Existing tables are never dropped.
USE [master];
GO
IF DB_ID(N'BadmintonShopDB1') IS NULL
    CREATE DATABASE [BadmintonShopDB1];
GO
USE [BadmintonShopDB1];
GO
IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
BEGIN
    PRINT N'Database is not empty. Use a new database for initial setup.';
    SET NOEXEC ON;
END;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

/* ============================================================================
   PHẦN 1 - SCHEMA MỚI
   ============================================================================ */

/****** Object:  Table [dbo].[Blog]    Script Date: 10/08/2026 15:02:12 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Blog](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NULL,
	[danh_muc] [nvarchar](255) NULL,
	[noi_dung] [nvarchar](max) NULL,
	[hinh_anh] [nvarchar](255) NULL,
	[ngay_dang] [date] NULL,
	[duong_dan] [varchar](255) NOT NULL,
	[tom_tat] [nvarchar](1000) NULL,
	[the] [nvarchar](255) NULL,
	[tieu_de] [nvarchar](255) NOT NULL,
	[trang_thai] [varchar](20) NOT NULL,
	[da_xoa] [bit] NOT NULL,
	[ngay_xoa] [datetime] NULL,
	[ngay_tao] [datetime] NOT NULL,
	[updated_by] [varchar](255) NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[duong_dan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BlogComment]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BlogComment](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_blog] [int] NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[noi_dung] [nvarchar](1000) NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[da_xoa] [bit] NOT NULL,
	[ngay_xoa] [datetime] NULL,
	[ly_do_xoa] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatConversation]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatConversation](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NULL,
	[session_id] [nvarchar](100) NULL,
	[tieu_de] [nvarchar](255) NULL,
	[trang_thai] [nvarchar](20) NOT NULL,
	[ngay_tao] [datetime2](0) NOT NULL,
	[ngay_cap_nhat] [datetime2](0) NULL,
 CONSTRAINT [PK_ChatConversation] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatFeedback]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatFeedback](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_tin_nhan] [bigint] NOT NULL,
	[id_tai_khoan] [int] NULL,
	[session_id] [nvarchar](100) NULL,
	[danh_gia] [smallint] NOT NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[ngay_tao] [datetime2](0) NOT NULL,
	[ngay_cap_nhat] [datetime2](0) NULL,
 CONSTRAINT [PK_ChatFeedback] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatMessage]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatMessage](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_cuoc_tro_chuyen] [bigint] NOT NULL,
	[vai_tro] [nvarchar](20) NOT NULL,
	[noi_dung] [nvarchar](max) NOT NULL,
	[ten_model] [nvarchar](100) NULL,
	[trang_thai] [nvarchar](20) NOT NULL,
	[so_token_dau_vao] [int] NULL,
	[so_token_dau_ra] [int] NULL,
	[thoi_gian_xu_ly_ms] [bigint] NULL,
	[ma_loi] [nvarchar](100) NULL,
	[noi_dung_loi] [nvarchar](1000) NULL,
	[ngay_tao] [datetime2](0) NOT NULL,
 CONSTRAINT [PK_ChatMessage] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CommentModerationKeyword]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CommentModerationKeyword](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[tu_khoa] [nvarchar](255) NOT NULL,
	[kich_hoat] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[tu_khoa] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CommentViolationLog]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CommentViolationLog](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[id_danh_gia] [int] NULL,
	[id_san_pham] [int] NOT NULL,
	[noi_dung_goc] [nvarchar](max) NOT NULL,
	[noi_dung_da_loc] [nvarchar](max) NOT NULL,
	[muc_do_vi_pham] [nvarchar](50) NOT NULL,
	[so_lan_vi_pham] [int] NOT NULL,
	[thoi_han_khoa] [nvarchar](100) NULL,
	[ngay_vi_pham] [datetime] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DanhGia]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DanhGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[id_san_pham] [int] NOT NULL,
	[so_sao] [float] NOT NULL,
	[noi_dung] [nvarchar](max) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[binh_luan_an] [bit] NOT NULL,
	[hinh_anh_an] [bit] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
	[id_nhan_vien] [int] NULL,
	[ngay_an_binh_luan] [datetime] NULL,
	[ngay_hien_binh_luan] [datetime] NULL,
	[ngay_an_hinh_anh] [datetime] NULL,
	[ngay_hien_hinh_anh] [datetime] NULL,
	[da_xoa] [bit] NOT NULL,
	[ngay_xoa] [datetime] NULL,
	[id_nguoi_xoa] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UQ_KhachHang_SanPham] UNIQUE NONCLUSTERED 
(
	[id_khach_hang] ASC,
	[id_san_pham] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DanhMuc]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DanhMuc](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_danh_muc] [nvarchar](255) NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DanhMucThuocTinh]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DanhMucThuocTinh](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_danh_muc] [int] NOT NULL,
	[id_thuoc_tinh] [int] NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UQ_DanhMucThuocTinh] UNIQUE NONCLUSTERED 
(
	[id_danh_muc] ASC,
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DonViVanChuyen]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DonViVanChuyen](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_don_vi] [nvarchar](50) NULL,
	[ten_don_vi] [nvarchar](100) NULL,
	[so_hotline] [nvarchar](20) NULL,
	[web_url] [nvarchar](100) NULL,
	[ma_token] [nvarchar](255) NULL,
	[ma_client] [nvarchar](100) NULL,
	[dia_chi_kho] [nvarchar](500) NULL,
	[phi_noi_dia] [decimal](18, 2) NULL,
	[phi_toan_quoc] [decimal](18, 2) NULL,
	[phien_ban] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DotGiamGia]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DotGiamGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_chien_dich] [nvarchar](255) NOT NULL,
	[ngay_bat_dau] [datetime] NOT NULL,
	[ngay_ket_thuc] [datetime] NOT NULL,
	[phan_tram_giam] [int] NULL,
	[gia_tri_giam] [decimal](18, 2) NULL,
	[gia_tu] [decimal](18, 2) NULL,
	[gia_den] [decimal](18, 2) NULL,
	[loai_giam_gia] [nvarchar](100) NOT NULL,
	[id_nhan_vien] [int] NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[EditLog]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[EditLog](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NULL,
	[ten_bang] [nvarchar](100) NOT NULL,
	[id_ban_ghi] [int] NOT NULL,
	[hanh_dong] [nvarchar](20) NOT NULL,
	[gia_tri_cu] [nvarchar](max) NULL,
	[gia_tri_moi] [nvarchar](max) NULL,
	[thoi_gian] [datetime] NOT NULL,
	[dia_chi_ip] [varchar](50) NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[vai_tro_thuc_hien] [nvarchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[flyway_schema_history]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[flyway_schema_history](
	[installed_rank] [int] NOT NULL,
	[version] [nvarchar](50) NULL,
	[description] [nvarchar](200) NULL,
	[type] [nvarchar](20) NOT NULL,
	[script] [nvarchar](1000) NOT NULL,
	[checksum] [int] NULL,
	[installed_by] [nvarchar](100) NOT NULL,
	[installed_on] [datetime] NOT NULL,
	[execution_time] [int] NOT NULL,
	[success] [bit] NOT NULL,
 CONSTRAINT [flyway_schema_history_pk] PRIMARY KEY CLUSTERED 
(
	[installed_rank] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[GiaoDichThanhToan]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GiaoDichThanhToan](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_giao_dich] [nvarchar](100) NOT NULL,
	[id_hoa_don] [int] NULL,
	[so_tien] [decimal](18, 2) NOT NULL,
	[cong_thanh_toan] [nvarchar](50) NOT NULL,
	[trang_thai] [nvarchar](50) NOT NULL,
	[du_lieu_tho] [nvarchar](max) NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[ma_giao_dich] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[GioHang]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GioHang](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[GioHangChiTiet]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GioHangChiTiet](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_gio_hang] [int] NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[so_luong] [int] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HinhAnhDanhGia]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HinhAnhDanhGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_danh_gia] [int] NOT NULL,
	[url_hinh_anh] [nvarchar](max) NOT NULL,
	[thu_tu] [int] NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HinhAnhSanPham]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HinhAnhSanPham](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[url_hinh_anh] [nvarchar](max) NOT NULL,
	[mau_sac] [nvarchar](50) NULL,
	[la_anh_chinh] [bit] NOT NULL,
	[thu_tu] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HoaDon]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HoaDon](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[id_nhan_vien] [int] NULL,
	[id_phuong_thuc_thanh_toan] [int] NOT NULL,
	[id_phieu_giam_gia] [int] NULL,
	[id_don_vi_van_chuyen] [int] NULL,
	[id_dia_chi] [int] NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_thanh_toan] [datetime] NULL,
	[tien_hang] [decimal](18, 2) NOT NULL,
	[phi_van_chuyen] [decimal](18, 2) NOT NULL,
	[so_tien_giam_gia] [decimal](18, 2) NOT NULL,
	[tong_tien] [decimal](18, 2) NOT NULL,
	[trang_thai_don_hang] [nvarchar](50) NOT NULL,
	[trang_thai_thanh_toan] [nvarchar](50) NOT NULL,
	[ten_nguoi_nhan] [nvarchar](100) NULL,
	[sdt_nhan] [nvarchar](15) NOT NULL,
	[email_nguoi_nhan] [nvarchar](255) NULL,
	[dia_chi_nhan] [nvarchar](500) NOT NULL,
	[ly_do_huy] [nvarchar](500) NULL,
	[ly_do_hoan_tra] [nvarchar](500) NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[ma_giao_dich] [nvarchar](100) NULL,
	[nguoi_xac_nhan_thanh_toan] [nvarchar](100) NULL,
	[thoi_gian_xac_nhan] [datetime] NULL,
	[trang_thai_hoan_hang] [nvarchar](50) NULL,
	[loai_yeu_cau_doi_tra] [nvarchar](20) NULL,
	[bang_chung_hoan_tra] [nvarchar](max) NULL,
	[trang_thai_xu_ly_hang_hoan] [nvarchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[HoaDonChiTiet]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[HoaDonChiTiet](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_hoa_don] [int] NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[so_luong] [int] NOT NULL,
	[don_gia] [decimal](18, 2) NOT NULL,
	[gia_goc] [decimal](18, 2) NULL,
	[gia_sau_giam] [decimal](18, 2) NULL,
	[ten_san_pham_snapshot] [nvarchar](255) NULL,
	[sku_snapshot] [nvarchar](100) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ten_dot_giam_gia_snapshot] [nvarchar](255) NULL,
	[thuoc_tinh_snapshot] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[KhachHang]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[KhachHang](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[ho_ten_kh] [nvarchar](100) NULL,
	[so_dien_thoai_kh] [varchar](15) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[LichSuTrangThaiDonHang]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LichSuTrangThaiDonHang](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[id_hoa_don] [int] NOT NULL,
	[id_nhan_vien] [int] NULL,
	[hanh_dong] [nvarchar](100) NOT NULL,
	[trang_thai_cu] [nvarchar](50) NULL,
	[trang_thai_moi] [nvarchar](50) NULL,
	[ghi_chu] [nvarchar](500) NULL,
	[thoi_gian] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MaKhoiPhuc]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MaKhoiPhuc](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[ma_xac_nhan] [nvarchar](100) NOT NULL,
	[loai_xac_nhan] [nvarchar](20) NOT NULL,
	[thoi_gian_het_han] [datetime] NOT NULL,
	[da_su_dung] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NewsletterSubscriber]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NewsletterSubscriber](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[email] [nvarchar](255) NOT NULL,
	[trang_thai] [nvarchar](30) NOT NULL,
	[token_huy_dang_ky] [nvarchar](255) NULL,
	[ngay_dang_ky] [datetime] NOT NULL,
	[ngay_huy] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NhanVien]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NhanVien](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[ho_ten] [nvarchar](100) NOT NULL,
	[chuc_vu] [nvarchar](100) NOT NULL,
	[so_dien_thoai_nv] [nvarchar](15) NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PhieuGiamGia]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PhieuGiamGia](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_phieu] [nvarchar](50) NOT NULL,
	[gia_tri] [decimal](18, 2) NOT NULL,
	[gia_tri_giam_toi_da] [decimal](18, 2) NULL,
	[don_vi] [nvarchar](10) NOT NULL,
	[ngay_bat_dau] [datetime] NOT NULL,
	[ngay_ket_thuc] [datetime] NOT NULL,
	[so_luong_con_lai] [int] NOT NULL,
	[loai_giam_gia] [nvarchar](100) NOT NULL,
	[id_nhan_vien] [int] NOT NULL,
	[gia_tri_don_hang_toi_thieu] [decimal](18, 2) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[ma_phieu] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PhuongThucThanhToan]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PhuongThucThanhToan](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ma_phuong_thuc] [nvarchar](50) NULL,
	[ten_phuong_thuc] [nvarchar](100) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPham]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPham](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_danh_muc] [int] NOT NULL,
	[id_thuong_hieu] [int] NOT NULL,
	[id_nhan_vien] [int] NOT NULL,
	[ten_san_pham] [nvarchar](255) NOT NULL,
	[mo_ta] [nvarchar](max) NULL,
	[trang_thai] [bit] NOT NULL,
	[so_luot_danh_gia] [int] NOT NULL,
	[diem_trung_binh] [float] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
	[ma_san_pham] [nvarchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPham_DotGiamGia]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPham_DotGiamGia](
	[id_san_pham] [int] NOT NULL,
	[id_dot_giam_gia] [int] NOT NULL,
 CONSTRAINT [PK_SanPham_DotGiamGia] PRIMARY KEY CLUSTERED 
(
	[id_san_pham] ASC,
	[id_dot_giam_gia] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPhamChiTiet]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPhamChiTiet](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_san_pham] [int] NOT NULL,
	[gia_nhap] [decimal](18, 2) NULL,
	[gia_ban] [decimal](18, 2) NOT NULL,
	[so_luong_ton] [int] NOT NULL,
	[trang_thai] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
	[so_luong_sp_loi] [int] NOT NULL,
	[sku] [nvarchar](40) NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPhamChiTietThuocTinh]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPhamChiTietThuocTinh](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_san_pham_chi_tiet] [int] NOT NULL,
	[id_thuoc_tinh] [int] NOT NULL,
	[gia_tri] [nvarchar](500) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UQ_SPCT_ThuocTinh] UNIQUE NONCLUSTERED 
(
	[id_san_pham_chi_tiet] ASC,
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SanPhamYeuThich]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SanPhamYeuThich](
	[id_khach_hang] [int] NOT NULL,
	[id_san_pham] [int] NOT NULL,
	[ngay_them] [datetime] NOT NULL,
 CONSTRAINT [PK_SanPhamYeuThich] PRIMARY KEY CLUSTERED 
(
	[id_khach_hang] ASC,
	[id_san_pham] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SoDiaChi]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SoDiaChi](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_khach_hang] [int] NOT NULL,
	[ho_va_ten_nguoi_nhan] [nvarchar](100) NULL,
	[sdt_nguoi_nhan] [nvarchar](15) NOT NULL,
	[dia_chi_cu_the] [nvarchar](255) NOT NULL,
	[tinh_thanh] [nvarchar](100) NULL,
	[quan_huyen] [nvarchar](100) NULL,
	[phuong_xa] [nvarchar](100) NULL,
	[ghn_province_id] [int] NULL,
	[ghn_district_id] [int] NULL,
	[ghn_ward_code] [nvarchar](50) NULL,
	[dia_chi_mac_dinh] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TaiKhoan]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TaiKhoan](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[username] [nvarchar](255) NOT NULL,
	[mat_khau] [nvarchar](255) NULL,
	[vai_tro] [nvarchar](10) NOT NULL,
	[trang_thai_tai_khoan] [varchar](30) NOT NULL,
	[so_lan_mua_thanh_cong] [int] NOT NULL,
	[so_lan_nhac_nho_vi_pham] [int] NOT NULL,
	[ngay_vi_pham_gan_nhat] [datetime] NULL,
	[thoi_han_mo_khoa] [datetime] NULL,
	[ma_xac_thuc_khoa] [nvarchar](100) NULL,
	[ngay_tao] [datetime] NOT NULL,
	[ngay_cap_nhat] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ThongBao]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ThongBao](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_tai_khoan] [int] NOT NULL,
	[tieu_de] [nvarchar](255) NOT NULL,
	[noi_dung] [nvarchar](max) NOT NULL,
	[loai_thong_bao] [nvarchar](50) NULL,
	[da_doc] [bit] NOT NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ThuocTinh]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ThuocTinh](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_thuoc_tinh] [nvarchar](100) NOT NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[ten_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ThuongHieu]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ThuongHieu](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[ten_thuong_hieu] [nvarchar](255) NOT NULL,
	[logo] [nvarchar](255) NULL,
	[trang_thai] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TichHopVanChuyen]    Script Date: 10/08/2026 15:02:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TichHopVanChuyen](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_hoa_don] [int] NOT NULL,
	[nha_cung_cap] [nvarchar](50) NOT NULL,
	[ma_don_hang_ngoai] [nvarchar](100) NULL,
	[ma_van_don] [nvarchar](100) NULL,
	[trang_thai] [nvarchar](100) NULL,
	[du_lieu_yeu_cau] [nvarchar](max) NULL,
	[du_lieu_phan_hoi] [nvarchar](max) NULL,
	[ngay_tao] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_PADDING ON
GO
/****** Object:  Index [IDX_ChatConversation_Session]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatConversation_Session] ON [dbo].[ChatConversation]
(
	[session_id] ASC,
	[ngay_tao] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_ChatConversation_TaiKhoan]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatConversation_TaiKhoan] ON [dbo].[ChatConversation]
(
	[id_tai_khoan] ASC,
	[ngay_tao] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_ChatFeedback_TinNhan]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatFeedback_TinNhan] ON [dbo].[ChatFeedback]
(
	[id_tin_nhan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_ChatFeedback_TinNhan_Session]    Script Date: 10/08/2026 15:02:13 ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_ChatFeedback_TinNhan_Session] ON [dbo].[ChatFeedback]
(
	[id_tin_nhan] ASC,
	[session_id] ASC
)
WHERE ([session_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [UQ_ChatFeedback_TinNhan_TaiKhoan]    Script Date: 10/08/2026 15:02:13 ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_ChatFeedback_TinNhan_TaiKhoan] ON [dbo].[ChatFeedback]
(
	[id_tin_nhan] ASC,
	[id_tai_khoan] ASC
)
WHERE ([id_tai_khoan] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_ChatMessage_CuocTroChuyen]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_ChatMessage_CuocTroChuyen] ON [dbo].[ChatMessage]
(
	[id_cuoc_tro_chuyen] ASC,
	[ngay_tao] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_DANHGIA_SANPHAM]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_DANHGIA_SANPHAM] ON [dbo].[DanhGia]
(
	[id_san_pham] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_DanhMucThuocTinh_DanhMuc]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_DanhMucThuocTinh_DanhMuc] ON [dbo].[DanhMucThuocTinh]
(
	[id_danh_muc] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_DanhMucThuocTinh_ThuocTinh]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_DanhMucThuocTinh_ThuocTinh] ON [dbo].[DanhMucThuocTinh]
(
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_EDITLOG_TIME]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_EDITLOG_TIME] ON [dbo].[EditLog]
(
	[thoi_gian] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [flyway_schema_history_s_idx]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [flyway_schema_history_s_idx] ON [dbo].[flyway_schema_history]
(
	[success] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_GIAODICH_HOADON]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_GIAODICH_HOADON] ON [dbo].[GiaoDichThanhToan]
(
	[id_hoa_don] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_GIOHANG_KHACHHANG]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_GIOHANG_KHACHHANG] ON [dbo].[GioHang]
(
	[id_khach_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_GIOHANGCHITIET_GIOHANG]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_GIOHANGCHITIET_GIOHANG] ON [dbo].[GioHangChiTiet]
(
	[id_gio_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_HOADON_KHACHHANG]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_HOADON_KHACHHANG] ON [dbo].[HoaDon]
(
	[id_khach_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_HOADON_NGAYTAO]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_HOADON_NGAYTAO] ON [dbo].[HoaDon]
(
	[ngay_tao] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_HOADONCHITIET_HOADON]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_HOADONCHITIET_HOADON] ON [dbo].[HoaDonChiTiet]
(
	[id_hoa_don] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_KHACHHANG_TAIKHOAN]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_KHACHHANG_TAIKHOAN] ON [dbo].[KhachHang]
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_NHANVIEN_TAIKHOAN]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_NHANVIEN_TAIKHOAN] ON [dbo].[NhanVien]
(
	[id_tai_khoan] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_SPCTTT_SPCT]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_SPCTTT_SPCT] ON [dbo].[SanPhamChiTietThuocTinh]
(
	[id_san_pham_chi_tiet] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_SPCTTT_ThuocTinh]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_SPCTTT_ThuocTinh] ON [dbo].[SanPhamChiTietThuocTinh]
(
	[id_thuoc_tinh] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_SODIACHI_KHACHHANG]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_SODIACHI_KHACHHANG] ON [dbo].[SoDiaChi]
(
	[id_khach_hang] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IDX_TICHHOPVC_HOADON]    Script Date: 10/08/2026 15:02:13 ******/
CREATE NONCLUSTERED INDEX [IDX_TICHHOPVC_HOADON] ON [dbo].[TichHopVanChuyen]
(
	[id_hoa_don] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Blog] ADD  DEFAULT ((0)) FOR [da_xoa]
GO
ALTER TABLE [dbo].[Blog] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[BlogComment] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[BlogComment] ADD  DEFAULT ((0)) FOR [da_xoa]
GO
ALTER TABLE [dbo].[ChatConversation] ADD  CONSTRAINT [DF_ChatConversation_TrangThai]  DEFAULT (N'ACTIVE') FOR [trang_thai]
GO
ALTER TABLE [dbo].[ChatConversation] ADD  CONSTRAINT [DF_ChatConversation_NgayTao]  DEFAULT (sysdatetime()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ChatFeedback] ADD  CONSTRAINT [DF_ChatFeedback_NgayTao]  DEFAULT (sysdatetime()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ChatMessage] ADD  CONSTRAINT [DF_ChatMessage_TrangThai]  DEFAULT (N'SUCCESS') FOR [trang_thai]
GO
ALTER TABLE [dbo].[ChatMessage] ADD  CONSTRAINT [DF_ChatMessage_NgayTao]  DEFAULT (sysdatetime()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[CommentModerationKeyword] ADD  DEFAULT ((1)) FOR [kich_hoat]
GO
ALTER TABLE [dbo].[CommentModerationKeyword] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[CommentViolationLog] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT ((0)) FOR [binh_luan_an]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT ((0)) FOR [hinh_anh_an]
GO
ALTER TABLE [dbo].[DanhGia] ADD  DEFAULT ((0)) FOR [da_xoa]
GO
ALTER TABLE [dbo].[DanhMuc] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[DanhMucThuocTinh] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[DotGiamGia] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[EditLog] ADD  DEFAULT (getdate()) FOR [thoi_gian]
GO
ALTER TABLE [dbo].[flyway_schema_history] ADD  DEFAULT (getdate()) FOR [installed_on]
GO
ALTER TABLE [dbo].[GiaoDichThanhToan] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[GioHang] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[GioHangChiTiet] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[HinhAnhDanhGia] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[HinhAnhSanPham] ADD  DEFAULT ((0)) FOR [la_anh_chinh]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT ((0)) FOR [tien_hang]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT ((0)) FOR [phi_van_chuyen]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT ((0)) FOR [so_tien_giam_gia]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT (N'CHO_XAC_NHAN') FOR [trang_thai_don_hang]
GO
ALTER TABLE [dbo].[HoaDon] ADD  DEFAULT (N'CHO_THANH_TOAN') FOR [trang_thai_thanh_toan]
GO
ALTER TABLE [dbo].[HoaDonChiTiet] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[KhachHang] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] ADD  DEFAULT (getdate()) FOR [thoi_gian]
GO
ALTER TABLE [dbo].[MaKhoiPhuc] ADD  DEFAULT ((0)) FOR [da_su_dung]
GO
ALTER TABLE [dbo].[MaKhoiPhuc] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[NewsletterSubscriber] ADD  DEFAULT ('ACTIVE') FOR [trang_thai]
GO
ALTER TABLE [dbo].[NewsletterSubscriber] ADD  DEFAULT (getdate()) FOR [ngay_dang_ky]
GO
ALTER TABLE [dbo].[NhanVien] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT ((0)) FOR [so_luot_danh_gia]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT ((0.0)) FOR [diem_trung_binh]
GO
ALTER TABLE [dbo].[SanPham] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT ((0)) FOR [so_luong_ton]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[SanPhamChiTiet] ADD  DEFAULT ((0)) FOR [so_luong_sp_loi]
GO
ALTER TABLE [dbo].[SanPhamYeuThich] ADD  DEFAULT (getdate()) FOR [ngay_them]
GO
ALTER TABLE [dbo].[SoDiaChi] ADD  DEFAULT ((0)) FOR [dia_chi_mac_dinh]
GO
ALTER TABLE [dbo].[SoDiaChi] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT ('ACTIVE') FOR [trang_thai_tai_khoan]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT ((0)) FOR [so_lan_mua_thanh_cong]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT ((0)) FOR [so_lan_nhac_nho_vi_pham]
GO
ALTER TABLE [dbo].[TaiKhoan] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ThongBao] ADD  DEFAULT ((0)) FOR [da_doc]
GO
ALTER TABLE [dbo].[ThongBao] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[ThuocTinh] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[ThuongHieu] ADD  DEFAULT ((1)) FOR [trang_thai]
GO
ALTER TABLE [dbo].[TichHopVanChuyen] ADD  DEFAULT ('GHN') FOR [nha_cung_cap]
GO
ALTER TABLE [dbo].[TichHopVanChuyen] ADD  DEFAULT (getdate()) FOR [ngay_tao]
GO
ALTER TABLE [dbo].[Blog]  WITH CHECK ADD  CONSTRAINT [FK_Blog_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[Blog] CHECK CONSTRAINT [FK_Blog_TaiKhoan]
GO
ALTER TABLE [dbo].[BlogComment]  WITH CHECK ADD  CONSTRAINT [FK_BlogComment_Blog] FOREIGN KEY([id_blog])
REFERENCES [dbo].[Blog] ([id])
GO
ALTER TABLE [dbo].[BlogComment] CHECK CONSTRAINT [FK_BlogComment_Blog]
GO
ALTER TABLE [dbo].[BlogComment]  WITH CHECK ADD  CONSTRAINT [FK_BlogComment_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[BlogComment] CHECK CONSTRAINT [FK_BlogComment_TaiKhoan]
GO
ALTER TABLE [dbo].[ChatConversation]  WITH CHECK ADD  CONSTRAINT [FK_ChatConversation_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[ChatConversation] CHECK CONSTRAINT [FK_ChatConversation_TaiKhoan]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [FK_ChatFeedback_ChatMessage] FOREIGN KEY([id_tin_nhan])
REFERENCES [dbo].[ChatMessage] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [FK_ChatFeedback_ChatMessage]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [FK_ChatFeedback_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [FK_ChatFeedback_TaiKhoan]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [FK_ChatMessage_ChatConversation] FOREIGN KEY([id_cuoc_tro_chuyen])
REFERENCES [dbo].[ChatConversation] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [FK_ChatMessage_ChatConversation]
GO
ALTER TABLE [dbo].[CommentViolationLog]  WITH CHECK ADD  CONSTRAINT [FK_CommentViolationLog_DanhGia] FOREIGN KEY([id_danh_gia])
REFERENCES [dbo].[DanhGia] ([id])
GO
ALTER TABLE [dbo].[CommentViolationLog] CHECK CONSTRAINT [FK_CommentViolationLog_DanhGia]
GO
ALTER TABLE [dbo].[CommentViolationLog]  WITH CHECK ADD  CONSTRAINT [FK_CommentViolationLog_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[CommentViolationLog] CHECK CONSTRAINT [FK_CommentViolationLog_SanPham]
GO
ALTER TABLE [dbo].[CommentViolationLog]  WITH CHECK ADD  CONSTRAINT [FK_CommentViolationLog_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[CommentViolationLog] CHECK CONSTRAINT [FK_CommentViolationLog_TaiKhoan]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_KhachHang]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_NguoiHienHA] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_NguoiHienHA]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_NguoiXoa] FOREIGN KEY([id_nguoi_xoa])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_NguoiXoa]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD  CONSTRAINT [FK_DanhGia_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[DanhGia] CHECK CONSTRAINT [FK_DanhGia_SanPham]
GO
ALTER TABLE [dbo].[DanhMucThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_DanhMucThuocTinh_DanhMuc] FOREIGN KEY([id_danh_muc])
REFERENCES [dbo].[DanhMuc] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[DanhMucThuocTinh] CHECK CONSTRAINT [FK_DanhMucThuocTinh_DanhMuc]
GO
ALTER TABLE [dbo].[DanhMucThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_DanhMucThuocTinh_ThuocTinh] FOREIGN KEY([id_thuoc_tinh])
REFERENCES [dbo].[ThuocTinh] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[DanhMucThuocTinh] CHECK CONSTRAINT [FK_DanhMucThuocTinh_ThuocTinh]
GO
ALTER TABLE [dbo].[DotGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_DotGiamGia_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[DotGiamGia] CHECK CONSTRAINT [FK_DotGiamGia_NhanVien]
GO
ALTER TABLE [dbo].[EditLog]  WITH CHECK ADD  CONSTRAINT [FK_EditLog_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[EditLog] CHECK CONSTRAINT [FK_EditLog_TaiKhoan]
GO
ALTER TABLE [dbo].[GiaoDichThanhToan]  WITH CHECK ADD  CONSTRAINT [FK_GiaoDichThanhToan_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[GiaoDichThanhToan] CHECK CONSTRAINT [FK_GiaoDichThanhToan_HoaDon]
GO
ALTER TABLE [dbo].[GioHang]  WITH CHECK ADD  CONSTRAINT [FK_GioHang_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[GioHang] CHECK CONSTRAINT [FK_GioHang_KhachHang]
GO
ALTER TABLE [dbo].[GioHangChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_GioHangChiTiet_GioHang] FOREIGN KEY([id_gio_hang])
REFERENCES [dbo].[GioHang] ([id])
GO
ALTER TABLE [dbo].[GioHangChiTiet] CHECK CONSTRAINT [FK_GioHangChiTiet_GioHang]
GO
ALTER TABLE [dbo].[GioHangChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_GioHangChiTiet_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
GO
ALTER TABLE [dbo].[GioHangChiTiet] CHECK CONSTRAINT [FK_GioHangChiTiet_SPCT]
GO
ALTER TABLE [dbo].[HinhAnhDanhGia]  WITH CHECK ADD  CONSTRAINT [FK_HinhAnhDanhGia_DanhGia] FOREIGN KEY([id_danh_gia])
REFERENCES [dbo].[DanhGia] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[HinhAnhDanhGia] CHECK CONSTRAINT [FK_HinhAnhDanhGia_DanhGia]
GO
ALTER TABLE [dbo].[HinhAnhSanPham]  WITH CHECK ADD  CONSTRAINT [FK_HinhAnhSanPham_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
GO
ALTER TABLE [dbo].[HinhAnhSanPham] CHECK CONSTRAINT [FK_HinhAnhSanPham_SPCT]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_DiaChi] FOREIGN KEY([id_dia_chi])
REFERENCES [dbo].[SoDiaChi] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_DiaChi]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_DVVC] FOREIGN KEY([id_don_vi_van_chuyen])
REFERENCES [dbo].[DonViVanChuyen] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_DVVC]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_KhachHang]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_NhanVien]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_PhieuGiamGia] FOREIGN KEY([id_phieu_giam_gia])
REFERENCES [dbo].[PhieuGiamGia] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_PhieuGiamGia]
GO
ALTER TABLE [dbo].[HoaDon]  WITH CHECK ADD  CONSTRAINT [FK_HoaDon_PTTT] FOREIGN KEY([id_phuong_thuc_thanh_toan])
REFERENCES [dbo].[PhuongThucThanhToan] ([id])
GO
ALTER TABLE [dbo].[HoaDon] CHECK CONSTRAINT [FK_HoaDon_PTTT]
GO
ALTER TABLE [dbo].[HoaDonChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_HoaDonChiTiet_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[HoaDonChiTiet] CHECK CONSTRAINT [FK_HoaDonChiTiet_HoaDon]
GO
ALTER TABLE [dbo].[HoaDonChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_HoaDonChiTiet_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
GO
ALTER TABLE [dbo].[HoaDonChiTiet] CHECK CONSTRAINT [FK_HoaDonChiTiet_SPCT]
GO
ALTER TABLE [dbo].[KhachHang]  WITH CHECK ADD  CONSTRAINT [FK_KhachHang_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[KhachHang] CHECK CONSTRAINT [FK_KhachHang_TaiKhoan]
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang]  WITH CHECK ADD  CONSTRAINT [FK_LichSu_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] CHECK CONSTRAINT [FK_LichSu_HoaDon]
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang]  WITH CHECK ADD  CONSTRAINT [FK_LichSu_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] CHECK CONSTRAINT [FK_LichSu_NhanVien]
GO
ALTER TABLE [dbo].[MaKhoiPhuc]  WITH CHECK ADD  CONSTRAINT [FK_MaKhoiPhuc_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[MaKhoiPhuc] CHECK CONSTRAINT [FK_MaKhoiPhuc_TaiKhoan]
GO
ALTER TABLE [dbo].[NhanVien]  WITH CHECK ADD  CONSTRAINT [FK_NhanVien_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
GO
ALTER TABLE [dbo].[NhanVien] CHECK CONSTRAINT [FK_NhanVien_TaiKhoan]
GO
ALTER TABLE [dbo].[PhieuGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_PhieuGiamGia_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[PhieuGiamGia] CHECK CONSTRAINT [FK_PhieuGiamGia_NhanVien]
GO
ALTER TABLE [dbo].[SanPham]  WITH CHECK ADD  CONSTRAINT [FK_SanPham_DanhMuc] FOREIGN KEY([id_danh_muc])
REFERENCES [dbo].[DanhMuc] ([id])
GO
ALTER TABLE [dbo].[SanPham] CHECK CONSTRAINT [FK_SanPham_DanhMuc]
GO
ALTER TABLE [dbo].[SanPham]  WITH CHECK ADD  CONSTRAINT [FK_SanPham_NhanVien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[NhanVien] ([id])
GO
ALTER TABLE [dbo].[SanPham] CHECK CONSTRAINT [FK_SanPham_NhanVien]
GO
ALTER TABLE [dbo].[SanPham]  WITH CHECK ADD  CONSTRAINT [FK_SanPham_ThuongHieu] FOREIGN KEY([id_thuong_hieu])
REFERENCES [dbo].[ThuongHieu] ([id])
GO
ALTER TABLE [dbo].[SanPham] CHECK CONSTRAINT [FK_SanPham_ThuongHieu]
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_SPDGG_DotGiamGia] FOREIGN KEY([id_dot_giam_gia])
REFERENCES [dbo].[DotGiamGia] ([id])
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia] CHECK CONSTRAINT [FK_SPDGG_DotGiamGia]
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia]  WITH CHECK ADD  CONSTRAINT [FK_SPDGG_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[SanPham_DotGiamGia] CHECK CONSTRAINT [FK_SPDGG_SanPham]
GO
ALTER TABLE [dbo].[SanPhamChiTiet]  WITH CHECK ADD  CONSTRAINT [FK_SanPhamChiTiet_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[SanPhamChiTiet] CHECK CONSTRAINT [FK_SanPhamChiTiet_SanPham]
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_SPCTTT_SPCT] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[SanPhamChiTiet] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] CHECK CONSTRAINT [FK_SPCTTT_SPCT]
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh]  WITH CHECK ADD  CONSTRAINT [FK_SPCTTT_ThuocTinh] FOREIGN KEY([id_thuoc_tinh])
REFERENCES [dbo].[ThuocTinh] ([id])
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] CHECK CONSTRAINT [FK_SPCTTT_ThuocTinh]
GO
ALTER TABLE [dbo].[SanPhamYeuThich]  WITH CHECK ADD  CONSTRAINT [FK_SanPhamYeuThich_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[SanPhamYeuThich] CHECK CONSTRAINT [FK_SanPhamYeuThich_KhachHang]
GO
ALTER TABLE [dbo].[SanPhamYeuThich]  WITH CHECK ADD  CONSTRAINT [FK_SanPhamYeuThich_SanPham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[SanPham] ([id])
GO
ALTER TABLE [dbo].[SanPhamYeuThich] CHECK CONSTRAINT [FK_SanPhamYeuThich_SanPham]
GO
ALTER TABLE [dbo].[SoDiaChi]  WITH CHECK ADD  CONSTRAINT [FK_SoDiaChi_KhachHang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[KhachHang] ([id])
GO
ALTER TABLE [dbo].[SoDiaChi] CHECK CONSTRAINT [FK_SoDiaChi_KhachHang]
GO
ALTER TABLE [dbo].[ThongBao]  WITH CHECK ADD  CONSTRAINT [FK_ThongBao_TaiKhoan] FOREIGN KEY([id_tai_khoan])
REFERENCES [dbo].[TaiKhoan] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[ThongBao] CHECK CONSTRAINT [FK_ThongBao_TaiKhoan]
GO
ALTER TABLE [dbo].[TichHopVanChuyen]  WITH CHECK ADD  CONSTRAINT [FK_TichHopVanChuyen_HoaDon] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[HoaDon] ([id])
GO
ALTER TABLE [dbo].[TichHopVanChuyen] CHECK CONSTRAINT [FK_TichHopVanChuyen_HoaDon]
GO
ALTER TABLE [dbo].[Blog]  WITH CHECK ADD CHECK  (([trang_thai]='PUBLISHED' OR [trang_thai]='DRAFT'))
GO
ALTER TABLE [dbo].[BlogComment]  WITH CHECK ADD  CONSTRAINT [CK_BlogComment_Content_Length] CHECK  ((len(ltrim(rtrim([noi_dung])))>=(1) AND len(ltrim(rtrim([noi_dung])))<=(1000)))
GO
ALTER TABLE [dbo].[BlogComment] CHECK CONSTRAINT [CK_BlogComment_Content_Length]
GO
ALTER TABLE [dbo].[ChatConversation]  WITH CHECK ADD  CONSTRAINT [CK_ChatConversation_NguoiDung] CHECK  (([id_tai_khoan] IS NOT NULL OR nullif(ltrim(rtrim([session_id])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[ChatConversation] CHECK CONSTRAINT [CK_ChatConversation_NguoiDung]
GO
ALTER TABLE [dbo].[ChatConversation]  WITH CHECK ADD  CONSTRAINT [CK_ChatConversation_TrangThai] CHECK  (([trang_thai]=N'ARCHIVED' OR [trang_thai]=N'CLOSED' OR [trang_thai]=N'ACTIVE'))
GO
ALTER TABLE [dbo].[ChatConversation] CHECK CONSTRAINT [CK_ChatConversation_TrangThai]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [CK_ChatFeedback_DanhGia] CHECK  (([danh_gia]=(1) OR [danh_gia]=(-1)))
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [CK_ChatFeedback_DanhGia]
GO
ALTER TABLE [dbo].[ChatFeedback]  WITH CHECK ADD  CONSTRAINT [CK_ChatFeedback_NguoiDanhGia] CHECK  (([id_tai_khoan] IS NOT NULL OR nullif(ltrim(rtrim([session_id])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[ChatFeedback] CHECK CONSTRAINT [CK_ChatFeedback_NguoiDanhGia]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_NoiDung] CHECK  ((nullif(ltrim(rtrim([noi_dung])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_NoiDung]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_ThoiGianXuLy] CHECK  (([thoi_gian_xu_ly_ms] IS NULL OR [thoi_gian_xu_ly_ms]>=(0)))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_ThoiGianXuLy]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_TokenDauRa] CHECK  (([so_token_dau_ra] IS NULL OR [so_token_dau_ra]>=(0)))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_TokenDauRa]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_TokenDauVao] CHECK  (([so_token_dau_vao] IS NULL OR [so_token_dau_vao]>=(0)))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_TokenDauVao]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_TrangThai] CHECK  (([trang_thai]=N'BLOCKED' OR [trang_thai]=N'FAILED' OR [trang_thai]=N'SUCCESS' OR [trang_thai]=N'PENDING'))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_TrangThai]
GO
ALTER TABLE [dbo].[ChatMessage]  WITH CHECK ADD  CONSTRAINT [CK_ChatMessage_VaiTro] CHECK  (([vai_tro]=N'TOOL' OR [vai_tro]=N'SYSTEM' OR [vai_tro]=N'ASSISTANT' OR [vai_tro]=N'USER'))
GO
ALTER TABLE [dbo].[ChatMessage] CHECK CONSTRAINT [CK_ChatMessage_VaiTro]
GO
ALTER TABLE [dbo].[DanhGia]  WITH CHECK ADD CHECK  (([so_sao]>=(1.0) AND [so_sao]<=(5.0)))
GO
ALTER TABLE [dbo].[EditLog]  WITH CHECK ADD CHECK  (([hanh_dong]='DELETE' OR [hanh_dong]='UPDATE' OR [hanh_dong]='INSERT'))
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang]  WITH CHECK ADD  CONSTRAINT [CK_LichSuTrangThaiDonHang_HanhDong] CHECK  (([hanh_dong]=N'GHI_CHU' OR [hanh_dong]=N'CAP_NHAT_MA_VAN_DON' OR [hanh_dong]=N'CAP_NHAT_TRANG_THAI' OR [hanh_dong]=N'XAC_NHAN_HOAN_TIEN' OR [hanh_dong]=N'XAC_NHAN_THANH_TOAN' OR [hanh_dong]=N'GIAO_HANG' OR [hanh_dong]=N'DONG_GOI' OR [hanh_dong]=N'HUY_DON' OR [hanh_dong]=N'XAC_NHAN_DON' OR [hanh_dong]=N'TAO_DON'))
GO
ALTER TABLE [dbo].[LichSuTrangThaiDonHang] CHECK CONSTRAINT [CK_LichSuTrangThaiDonHang_HanhDong]
ALTER TABLE [dbo].[MaKhoiPhuc]  WITH CHECK ADD  CONSTRAINT [CK_MaKhoiPhuc_LoaiXacNhan] CHECK  (([loai_xac_nhan]='EMAIL' OR [loai_xac_nhan]='SMS' OR [loai_xac_nhan]='GUEST_ACTIVATION' OR [loai_xac_nhan]='FORGOT_PASSWORD'))
GO
ALTER TABLE [dbo].[MaKhoiPhuc] CHECK CONSTRAINT [CK_MaKhoiPhuc_LoaiXacNhan]
GO
ALTER TABLE [dbo].[PhieuGiamGia]  WITH CHECK ADD CHECK  (([don_vi]='%' OR [don_vi]='VND'))
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh]  WITH CHECK ADD  CONSTRAINT [CK_SPCTTT_GiaTri] CHECK  ((nullif(ltrim(rtrim([gia_tri])),N'') IS NOT NULL))
GO
ALTER TABLE [dbo].[SanPhamChiTietThuocTinh] CHECK CONSTRAINT [CK_SPCTTT_GiaTri]
GO
ALTER TABLE [dbo].[TaiKhoan]  WITH CHECK ADD CHECK  (([vai_tro]='QL' OR [vai_tro]='NV' OR [vai_tro]='KH'))
GO

/* ============================================================================
   TẠO BẢNG PHIẾU NHẬP VÀ PHIẾU NHẬP CHI TIẾT
   ============================================================================ */
CREATE TABLE [dbo].[PhieuNhap] (
    [id] INT IDENTITY(1,1) NOT NULL,

    [ma_phieu_nhap] NVARCHAR(50) NOT NULL,

    [id_nhan_vien] INT NOT NULL,

    [ngay_nhap] DATETIME NOT NULL
        CONSTRAINT DF_PhieuNhap_NgayNhap DEFAULT GETDATE(),

    [tong_tien] DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_PhieuNhap_TongTien DEFAULT 0,

    [ghi_chu] NVARCHAR(500) NULL,

    [ngay_tao] DATETIME NOT NULL
        CONSTRAINT DF_PhieuNhap_NgayTao DEFAULT GETDATE(),

    [ngay_cap_nhat] DATETIME NULL,

    CONSTRAINT PK_PhieuNhap
        PRIMARY KEY ([id]),

    CONSTRAINT UQ_PhieuNhap_MaPhieuNhap
        UNIQUE ([ma_phieu_nhap]),

    CONSTRAINT FK_PhieuNhap_NhanVien
        FOREIGN KEY ([id_nhan_vien])
        REFERENCES [dbo].[NhanVien]([id]),

    CONSTRAINT CK_PhieuNhap_TongTien
        CHECK ([tong_tien] >= 0)
);
GO

CREATE TABLE [dbo].[PhieuNhapChiTiet] (
    [id] INT IDENTITY(1,1) NOT NULL,

    [id_phieu_nhap] INT NOT NULL,

    [id_san_pham_chi_tiet] INT NOT NULL,

    [so_luong] INT NOT NULL,

    [gia_nhap] DECIMAL(18,2) NOT NULL,

    [thanh_tien] DECIMAL(18,2) NOT NULL,

    CONSTRAINT PK_PhieuNhapChiTiet
        PRIMARY KEY ([id]),

    CONSTRAINT FK_PNCT_PhieuNhap
        FOREIGN KEY ([id_phieu_nhap])
        REFERENCES [dbo].[PhieuNhap]([id]),

    CONSTRAINT FK_PNCT_SanPhamChiTiet
        FOREIGN KEY ([id_san_pham_chi_tiet])
        REFERENCES [dbo].[SanPhamChiTiet]([id]),

    CONSTRAINT CK_PNCT_SoLuong
        CHECK ([so_luong] > 0),

    CONSTRAINT CK_PNCT_GiaNhap
        CHECK ([gia_nhap] >= 0),

    CONSTRAINT CK_PNCT_ThanhTien
        CHECK ([thanh_tien] >= 0)
);
GO


SET NOEXEC OFF;
GO
