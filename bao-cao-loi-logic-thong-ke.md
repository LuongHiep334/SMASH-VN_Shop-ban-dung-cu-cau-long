# BÁO CÁO PHÂN TÍCH TOÀN DIỆN LOGIC XỬ LÝ PHÂN HỆ THỐNG KÊ (SMASH-VN)

> **Mục đích**: Báo cáo chi tiết các lỗi sai logic, sự không nhất quán dữ liệu, thiếu sót công thức tài chính và các điểm cần khắc phục trong module Thống kê (`AdminThongKeService`, `HoaDonRepository`, `HoaDonChiTietRepository`, `thongke.html`).  
> **Ngày lập báo cáo**: 25/08/2026  
> **Người lập**: Antigravity Assistant  
> **Đối tượng sử dụng**: Đội ngũ phát triển / Lập trình viên đang chỉnh sửa module Thống kê.

---

## TỔNG QUAN HIỆN TRẠNG
Phân hệ Thống kê hiện tại của hệ thống SMASH-VN bao gồm:
1. **Controller**: `AdminThongKeController.java` (`/admin/thong-ke`, `/admin/thong-ke/api`, `/admin/thong-ke/export`).
2. **Service**: `AdminThongKeService.java` (xử lý phân loại doanh thu `OrderClassifier`, tính KPIs, tăng trưởng, gợi ý vận hành, xuất Excel).
3. **Repository**: `HoaDonRepository.java`, `HoaDonChiTietRepository.java`, `SanPhamRepository.java`.
4. **Giao diện**: `templates/admin/thongke.html`.

Qua rà soát chuyên sâu từng dòng mã nguồn, phát hiện **6 nhóm lỗi và bất cập logic trọng yếu** sau:

---

## 1. NHÓM LỖI TRUY VẤN CSDL (QUERY MAPPING BUG) — [MỨC ĐỘ: RẤT NGHIÊM TRỌNG]

### 🔴 Lỗi 1.1: Truy vấn `findAllOrdersInPeriod` bị hardcode `null` làm tê liệt thống kê Online Gateway
* **Vị trí**: [`HoaDonRepository.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/repository/HoaDonRepository.java#L72-L91) & [`AdminThongKeService.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java#L510-L620).
* **Mô tả chi tiết**:
  * Trong JPQL `findAllOrdersInPeriod`:
    ```java
    SELECT hd.id,                          // row[0]
           hd.khachHang.hoTenKh,           // row[1]
           null,                           // row[2]
           hd.ngayTao,                     // row[3]
           null,                           // row[4] <-- Hardcode NULL (Đáng lẽ là hd.phuongThucThanhToan.maPhuongThuc)
           hd.phuongThucThanhToan.tenPhuongThuc, // row[5]
           null,                           // row[6] <-- Hardcode NULL (Đáng lẽ là hd.paymentStatus hoặc tương đương)
           hd.trangThaiThanhToan,          // row[7]
           hd.trangThaiDonHang,            // row[8]
           null, null, null,               // row[9, 10, 11]
           hd.tongTien,                    // row[12]
           hd.refundStatus                 // row[13]
    FROM HoaDon hd ...
    ```
  * Trong Service `AdminThongKeService.java`:
    ```java
    String paymentMethod = (String) row[4]; // LUÔN LUÔN LÀ NULL!
    String paymentStatus = (String) row[6]; // LUÔN LUÔN LÀ NULL!
    ```
  * Đoạn code tính KPIs cổng thanh toán Online (dòng 592-620):
    ```java
    if (paymentMethod != null) {
        String pm = paymentMethod.toLowerCase();
        if (pm.contains("zalopay") || pm.contains("sepay")) {
            onlineTotal++; // KHÔNG BAO GIỜ CHẠY VÀO ĐÂY!
        }
    }
    ```
* **Hậu quả**: Các chỉ số `onlineTotal`, `onlineSuccess`, `onlineFailed`, `onlinePending`, `onlineRevenue` **luôn luôn bằng 0** dù có phát sinh thanh toán qua ZaloPay/SePay thực tế.

---

### 🔴 Lỗi 1.2: Truy vấn Lịch sử giao dịch `findRawTransactionsInPeriod` bị `null` mã giao dịch
* **Vị trí**: [`HoaDonRepository.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/repository/HoaDonRepository.java#L98-L117) & [`AdminThongKeService.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java#L456-L463).
* **Mô tả chi tiết**:
  * JPQL `findRawTransactionsInPeriod` đặt các cột 9, 10, 11 thành `null, null, null` thay vì lấy trường `hd.maGiaoDich`.
  * Service đọc `tid = row[9]`, `appTransId = row[10]`, `maGd = row[11]` $\rightarrow$ Tất cả đều là `null` $\rightarrow$ gán mặc định `transactionId = "-"`.
* **Hậu quả**: Cột **Mã Giao Dịch** trên bảng Lịch sử giao dịch ở giao diện Thống kê **hiển thị dấu gạch ngang `-` cho 100% các đơn hàng**, mất hoàn toàn khả năng tra cứu đối soát.

---

## 2. NHÓM LỖI THỜI ĐIỂM GHI NHẬN DOANH THU & HOÀN TIỀN — [MỨC ĐỘ: NGHIÊM TRỌNG]

### 🔴 Lỗi 2.1: Ghi nhận doanh thu theo `ngayTao` thay vì ngày hoàn thành/thanh toán thực tế
* **Vị trí**: Toàn bộ các câu query trong `HoaDonRepository` và `HoaDonChiTietRepository` đều dùng:
  `WHERE hd.ngayTao BETWEEN :start AND :end`
* **Vấn đề nghiệp vụ**:
  * **Ví dụ thực tế**: Đơn hàng Online tạo ngày **28/07** (hình thức COD), giao hàng và thu tiền thành công vào ngày **03/08**.
  * Khi kế toán/admin lọc báo cáo "Tháng này" (Tháng 8: 01/08 - 31/08): Đơn hàng này **bị biến mất hoàn toàn** khỏi doanh thu tháng 8 vì `ngayTao` nằm ở tháng 7.
  * Trong nguyên lý kế toán tài chính (Cash-basis & Accrual-basis): **Doanh thu thực tế (Actual Revenue)** phải được ghi nhận tại thời điểm giao hàng thành công (`thoiGianXacNhan` / `paidAt` / ngày giao `da_giao`), không được ghi nhận theo ngày đặt hàng sơ khai.

---

### 🔴 Lỗi 2.2: Đơn hoàn tiền (Refund Reversal) trừ ngược về ngày tạo đơn cũ
* **Vị trí**: [`AdminThongKeService.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java#L535-L556).
* **Vấn đề nghiệp vụ**:
  * Khi đơn hàng bị hoàn tiền (`ACTUAL_REVENUE_REVERSAL`), hệ thống lấy `ngayTao` của đơn hàng đó để trừ vào biểu đồ doanh thu theo ngày (`groupedRevenue.put(key, ... - tongTien)`).
  * **Hậu quả 1**: Nếu đơn hàng tạo ngày 01/08, đến ngày 10/08 mới phát sinh Trả hàng hoàn tiền: Hệ thống lại trừ tiền vào cột ngày **01/08** (khiến doanh thu ngày 01/08 bị giảm hoặc âm trên biểu đồ), trong khi ngày **10/08** là ngày tiền thực tế bị rút ra thì không có biến động.
  * **Hậu quả 2**: Nếu đơn tạo tháng trước (tháng 7) và hoàn tiền vào tháng này (tháng 8): Lọc tháng 8 sẽ **không tìm thấy đơn hàng tháng 7** $\rightarrow$ Dòng tiền hoàn tháng 8 không bị trừ $\rightarrow$ **Doanh thu tháng 8 bị phóng đại (thổi phồng) sai lệch so với thực tế**.

---

## 3. NHÓM LỖI LỆCH TỔNG TIỀN DO VOUCHER & PHÍ SHIP — [MỨC ĐỘ: TRUNG BÌNH - CAO]

### ⚠️ Lỗi 3.1: Vênh số liệu giữa Doanh thu Sản phẩm vs Doanh thu Đơn hàng
* **Vị trí**: [`HoaDonChiTietRepository.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/repository/HoaDonChiTietRepository.java#L45-L69) & [`AdminThongKeService.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java#L666-L690).
* **Bản chất công thức**:
  1. `actualRevenue` (Doanh thu hóa đơn) = $\sum (\text{tienHang} + \text{phiVanChuyen} - \text{soTienGiamGia})$.
  2. `baseProductRevenue` (Doanh thu dòng sản phẩm) = $\sum (\text{soLuong} \times \text{donGia})$.
* **Hậu quả**:
  * `soTienGiamGia` từ Phiếu giảm giá (Voucher) áp dụng trên toàn đơn hàng không được phân bổ (allocate) theo tỷ lệ về từng dòng sản phẩm chi tiết.
  * Khi tính tỷ trọng % của Top sản phẩm hoặc Thương hiệu: `pShare = (pRev / baseProductRevenue) * 100`, mẫu số không đồng nhất với `actualRevenue` của cửa hàng, dẫn đến tổng doanh thu từng thương hiệu cộng lại khác với Doanh thu thực tế trên KPI Card.

---

## 4. NHÓM LỖI CẢNH BÁO TỒN KHO BÁN CHẬM (SLOW-MOVING) — [MỨC ĐỘ: TRUNG BÌNH]

### ⚠️ Lỗi 4.1: Cảnh báo "Hàng tồn kho bán chậm" bị sai khi chọn khoảng thời gian ngắn
* **Vị trí**: [`AdminThongKeService.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/service/admin/AdminThongKeService.java#L693-L745).
* **Mô tả chi tiết**:
  * Service lấy danh sách sản phẩm còn tồn kho (`stockVal > 0`) và so sánh với số lượng bán trong kỳ lọc `[start, end]`.
  * Nếu người dùng chọn bộ lọc **"Hôm nay" (Today)**: Rất nhiều sản phẩm không có phát sinh đơn bán trong 1 ngày đó $\rightarrow$ Hệ thống lập tức đánh cờ đỏ `DANGER: "Không phát sinh bán trong kỳ"`.
* **Giải pháp chuẩn**:
  * Chỉ số Hàng bán chậm / Tồn kho chết (Dead Stock / Slow-moving) phải dựa trên **tuổi của hàng tồn kho (Aging)** hoặc **thời gian kể từ đơn bán gần nhất (> 30 ngày / 60 ngày không bán được)**, độc lập hoàn toàn với bộ lọc doanh thu ngắn hạn.

---

## 5. NHÓM THIẾU SÓT VỀ CHỈ SỐ TÀI CHÍNH (MISSING FINANCIAL KPIS)

### ⚠️ Thiếu sót 5.1: Hoàn toàn chưa có tính Lợi Nhuận Gộp (Gross Profit) & Giá Vốn (COGS)
* **Thực trạng**:
  * Bảng `SanPhamChiTiet` đã có trường `giaNhap` (Giá vốn nhập hàng bình quân gia quyền).
  * Tuy nhiên, hệ thống chỉ đang tính:
    * Doanh thu thực tế (`actualRevenue`).
    * Doanh thu dự kiến (`expectedRevenue`).
    * Giá trị trung bình đơn (`avgOrderValue`).
  * **Chưa có**:
    * **Tổng giá vốn hàng bán (COGS)** = $\sum (\text{soLuong} \times \text{giaNhap})$.
    * **Lợi nhuận gộp (Gross Profit)** = $\text{Doanh thu thực tế} - \text{COGS}$.
    * **Tỷ suất lợi nhuận (Profit Margin %)** = $(\text{Lợi nhuận gộp} / \text{Doanh thu}) \times 100$.

---

### ⚠️ Thiếu sót 5.2: Chưa phân tách rõ ràng kênh Bán Tại Quầy (POS) và Online
* **Thực trạng**:
  * Doanh thu đang cộng gộp chung giữa POS (`HDSVN*`) và Online (`DHSVN*`).
  * Thiếu biểu đồ hoặc KPI card so sánh doanh thu theo kênh:
    * 🏪 **Doanh thu tại quầy (POS Store)**.
    * 🌐 **Doanh thu trực tuyến (Online Delivery)**.

---

## 6. NHÓM BẤT CẬP ĐẾM KHÁCH HÀNG MỚI (NEW CUSTOMERS METRIC)

### ⚠️ Bất cập 6.1: Khách hàng vãng lai tại quầy làm sai lệch số lượng Khách Mới
* **Vị trí**: [`HoaDonRepository.java`](file:///h:/SMASH_VN/SMASH-VN_Shop-ban-vot-cau-long/src/main/java/com/smashvn/shop/repository/HoaDonRepository.java#L93-L96).
* **Mô tả**:
  * Query: `WHERE (SELECT MIN(hd.ngayTao) FROM HoaDon hd WHERE hd.khachHang.id = kh.id AND hd.trangThaiDonHang IN ('da_giao', 'hoan_thanh')) BETWEEN :start AND :end`.
  * Nếu nhiều đơn bán tại quầy cùng gán cho 1 tài khoản `KhachHang` mặc định (ví dụ ID = 1 "Khách lẻ"), khách này chỉ được tính là khách mới 1 lần duy nhất lúc tạo hệ thống. Sau đó không bao giờ được tính nữa dù có nhiều khách lẻ mới mua tại quầy.

---

## BẢNG TỔNG HỢP & MA TRẬN ĐỘ ƯU TIÊN SỬA LỖI

| STT | Tên Lỗi / Bất Cập | Mức độ | File Ảnh Hưởng | Hướng Khắc Phục Khuyến Nghị |
| :---: | :--- | :---: | :--- | :--- |
| **1** | Lỗi hardcode `null` cột `paymentMethod` và `paymentStatus` trong JPQL `findAllOrdersInPeriod` | 🔴 **Rất cao** | `HoaDonRepository.java`, `AdminThongKeService.java` | Sửa SELECT JPQL trả đúng `hd.phuongThucThanhToan.tenPhuongThuc`, `hd.trangThaiThanhToan`. |
| **2** | Lỗi hardcode `null` cột `maGiaoDich` trong JPQL `findRawTransactionsInPeriod` | 🔴 **Rất cao** | `HoaDonRepository.java`, `AdminThongKeService.java` | Sửa SELECT JPQL trả về `hd.maGiaoDich` để bảng lịch sử hiển thị đúng mã giao dịch. |
| **3** | Ghi nhận doanh thu theo `ngayTao` thay vì ngày giao hàng/thanh toán thành công | 🔴 **Cao** | `HoaDonRepository.java`, `HoaDonChiTietRepository.java` | Lọc doanh thu thực tế theo `COALESCE(hd.thoiGianXacNhan, hd.ngayThanhToan, hd.ngayTao)`. |
| **4** | Refund Reversal trừ vào ngày tạo đơn cũ thay vì ngày hoàn tiền | 🔴 **Cao** | `AdminThongKeService.java` | Đơn hoàn tiền phải ghi nhận trừ doanh thu tại ngày phát sinh hoàn (`hd.ngayThanhToan` / `thoiGianHoanTien`). |
| **5** | Cảnh báo hàng bán chậm bị gán cờ `DANGER` khi xem theo ngày (Today) | 🟡 **Trung bình** | `AdminThongKeService.java` | Tách riêng hàm tính hàng tồn kho chậm luân chuyển dựa trên mốc cố định 30/60 ngày không có đơn. |
| **6** | Bổ sung Giá vốn (COGS) & Lợi nhuận gộp (Gross Profit) | 🟢 **Nâng cấp** | `AdminThongKeService.java`, `thongke.html` | Tính thêm `cogs = SUM(soLuong * giaNhap)` và `profit = revenue - cogs`. |

---
*Tài liệu này được lưu tại đường dẫn gốc dự án: `bao-cao-loi-logic-thong-ke.md`.*
