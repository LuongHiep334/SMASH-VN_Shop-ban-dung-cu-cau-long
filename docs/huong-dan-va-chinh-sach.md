# Hướng dẫn và chính sách SMASH VN

Triển khai trong workspace, chưa triển khai lên máy chủ. Ba trang công khai:

- `/huong-dan-mua-hang`
- `/huong-dan-thanh-toan`
- `/chinh-sach`

Nội dung được viết riêng theo mã nguồn. Trang VNB chỉ tham khảo cách chia phần, không lấy thông tin tài khoản ngân hàng hay cam kết dịch vụ của VNB.

## Căn cứ đã đối chiếu

- `CheckoutController`, `CheckoutContextService`: Mua ngay tạo lần thanh toán riêng; thanh toán giỏ dùng các món được chọn; kiểm tra tồn kho/giá/mã; kết quả đơn được lưu để kiểm tra lại khi mất phản hồi.
- `GuestCheckoutService`, `TemporaryPasswordService`, `UserDangNhapController`: email mới tạo GUEST; ACTIVE cần xác thực mật khẩu; email hồ sơ GUEST cũ không tự cấp quyền xem lịch sử; mật khẩu tạm phải chuyển sang mật khẩu chính thức. Email mật khẩu tạm được phát sau lần COD đủ điều kiện hoặc sau ghi nhận thanh toán SePay đủ điều kiện, không hứa gửi tại thời điểm chọn QR.
- `UserDashboardController`: tra cứu công khai bằng mã đơn + email hoặc điện thoại khớp; quyền xem đơn giới hạn theo phiên và đơn. Hủy/xác nhận nhận hàng/gửi đổi trả kiểm tra quyền trên đơn.
- `checkout.html`, `SepayIpnController`, `SepayGatewayService`: COD và SePay QR; nhận tiền qua đối soát; phiên chờ có thể hết hạn; nhận tiền muộn ở đơn hủy/hết hạn phải shop xử lý. Không hướng dẫn nút giả lập thành thao tác thanh toán thật.
- `ShippingFeeCalculator`: phí theo đơn vị, địa chỉ, cấu hình/dữ liệu và GHN khi khả dụng; không cố định phí trong nội dung.
- `OrderViewService`: khách hủy ở `cho_thanh_toan`, `cho_xac_nhan`, `da_xac_nhan`; đơn đã thu tiền chuyển chờ hoàn. Đổi/trả 7 ngày từ timestamp giao thành công; shop duyệt, thu hồi, kiểm hàng/xử lý kho trước xác nhận hoàn tiền; đổi phụ thuộc hàng thay thế.
- `dash-manage-order.html`, `FileStorageService`: biểu mẫu hiện tại yêu cầu video MP4/WEBM/MOV, tối đa 50 MB.
- `AccountStatus`, `TaiKhoan`: GUEST, ACTIVE, trạng thái khóa; không thấy chương trình hạng Đồng/Bạc/Vàng.

## Các điều khoản chủ shop cần chốt

Giữ mốc hiện có: 7 ngày từ khi giao thành công; thông báo lỗi do nhà sản xuất được thống nhất ở trang sản phẩm và chính sách. Chỉ mô tả hỗ trợ gửi yêu cầu, có xét duyệt; bỏ cam kết bảo hành hãng/hoàn 100% chưa đủ căn cứ.

1. Tiêu chí xét duyệt lỗi, tình trạng hàng/bao bì/phụ kiện; trường hợp đổi size/màu hoặc lý do khác có được chấp thuận hay không. Mã nguồn có trường lý do mở, chưa chứng minh được chính sách thương mại cho từng lý do.
2. Ai chịu phí gửi trả, giao hàng đổi, giao lại và giao không thành công; cách xử lý từng trường hợp.
3. Số tiền hoàn (gồm/không gồm phí vận chuyển), cách hoàn và thông tin nhận tiền; thời gian xét duyệt và hoàn tiền. Hệ thống ghi nhận xác nhận giao dịch, không tự chuyển tiền hoàn qua ngân hàng.
4. Thời gian giao dự kiến và quy trình xử lý giao không thành công; không suy ra cam kết giao từ trạng thái demo.
5. Thời hạn lưu, phạm vi chia sẻ dữ liệu và quy trình xử lý yêu cầu xóa dữ liệu; hiện hướng khách liên hệ, không tạo cam kết pháp lý mới.
6. Nút “Thanh toán ngay” trong chi tiết đơn hiện trỏ tới trang `/payment/sepay/simulate`, trang này phụ thuộc chế độ debug. Hướng dẫn không hứa có chức năng tạo lại QR hoặc tiếp tục trả tiền trên trang đó; khi đóng QR, hướng khách tra cứu và liên hệ. Đây là giới hạn luồng hiện có, chưa thay đổi trong phạm vi tác vụ này.

## Hình minh họa

`static/images/guides/chon-phan-loai.jpg` được cắt từ ảnh giao diện thật `docs/huong-dan-mua-hang/chon-san-pham-goc.jpg`, giữ nguyên vùng thuộc tính/số lượng/nút, bỏ phần thông báo chính sách cũ. Chú thích dùng HTML, không dùng ảnh AI. Ảnh minh họa sẵn có `chon-san-pham-minh-hoa.png` được giữ nguyên nhưng không dùng vì chứa nội dung chính sách cũ.

## Đồng nhất giao diện

Đối chiếu mã nguồn và ảnh giao diện đang chạy tại localhost của Giới thiệu, Blog, Giỏ hàng và Sản phẩm. Các trang có nền trắng và dùng Open Sans; breadcrumb chung nền `#fbfbfb`, bo 3 px; tiêu đề phần chung 28 px, độ đậm 600. Hệ thống có những chỗ bo góc riêng (nút, ảnh sản phẩm, thẻ Blog, biểu mẫu), nên không áp một bán kính duy nhất lên toàn website.

Ba trang hướng dẫn/chính sách được chỉnh theo các thành phần sẵn có, thay vì giữ bộ khung trang riêng:

| Thành phần | Điều chỉnh và căn cứ |
| --- | --- |
| Nền và tiêu đề | Nền trắng; bỏ khung tiêu đề viền cam, bo 8 px và đổ bóng. Dùng `section__heading`, `section__text-wrap` và khoảng cách chung như Giỏ hàng/Thanh toán. |
| Đường dẫn | Dùng `breadcrumb__wrap`, `breadcrumb__list` cùng các trang Giới thiệu, Blog, Giỏ hàng. |
| Tab liên trang | Dùng `pd-tab__list`, `nav-link`, `active` của trang Sản phẩm: chữ 14 px, gạch chân 2 px, cam `#ff4500` cho tab đang chọn. Đây là liên kết trang, không gọi JavaScript chuyển tab nội dung sản phẩm. |
| Mục lục | Bỏ hộp bao, dùng tiêu đề `blog-w__h` của sidebar Blog. Giữ mục lục cố định khi đọc trên desktop và xuống một cột trên điện thoại. |
| Nội dung, lưu ý và liên hệ | Bỏ nền cam nhạt, khung bo góc và huy hiệu bước trùng với số trong tiêu đề. Phân chia bằng khoảng cách và đường kẻ xám `#eee`; chữ bài viết 16 px, giãn dòng 2 như phần mô tả sản phẩm. |
| So sánh và câu hỏi thường gặp | So sánh hai cột không hộp bao; câu hỏi dùng đường kẻ đơn giản, giữ thao tác mở/đóng. |
| Dropdown header | Nền trắng, bóng nhẹ `0 0 4px` và khoảng đệm như dropdown hiện có; bỏ viền cam, nền cam khi rê chuột và bo góc riêng. Cỡ chữ/độ đậm của nút mở khớp các mục menu desktop và mobile. |
| Nút và liên kết | Giữ các nút nghiệp vụ của sản phẩm, giỏ hàng và thanh toán. Liên kết hỗ trợ dùng màu cam chung; không tạo thêm kiểu nút hành động riêng cho các trang tài liệu. |

CSS riêng chỉ xử lý bố cục bài dài, mục lục, hình minh họa và trạng thái focus bàn phím. Không chỉnh `app.css` hay ghi đè kiểu của trang khác. Tăng phiên bản CSS hướng dẫn và điều hướng lên `v=2` để tránh dùng bản thiết kế cũ trong bộ nhớ trình duyệt.

Đã chạy lại 2 kiểm tra MVC và toàn bộ kiểm tra trình duyệt hướng dẫn sau lần sửa giao diện này. Ảnh toàn trang, ảnh phần đầu `*-overview-1440.png` / `*-overview-390.png` và ảnh bước chọn phân loại `buying-step-mobile.png` nằm trong `target/support-preview/`.

## Kiểm tra an toàn

Kiểm tra trang qua môi trường MVC/Thymeleaf độc lập với cơ sở dữ liệu; bản xem trước phục vụ HTML đã render cùng tài nguyên thật trên localhost. Kiểm tra trình duyệt dùng các trang hướng dẫn/chính sách đã render và giỏ thu nhỏ rỗng mẫu, không gửi yêu cầu tạo đơn, chuyển tiền, email hoặc sửa dữ liệu khách hàng. Các kiểm tra tích hợp dùng cơ sở dữ liệu thật không nằm trong nhóm chạy cho thay đổi này.

Kết quả đã chạy:

- 2 kiểm tra `SupportPageMvcTest` qua MVC, Thymeleaf và `SecurityConfig` thật: truy cập ẩn danh HTTP 200, render ba trang, ảnh/tài nguyên, menu đúng hai mục, liên hệ, mục lục và liên kết chéo, thông báo 7 ngày thống nhất.
- 33 kiểm tra đơn vị hiện có: `CheckoutControllerTest` (8), `CheckoutRetryTest` (21), `CustomerPriceTemplateTest` (4), đều qua.
- 24 kiểm tra JavaScript hiện có của checkout và tìm kiếm, đều qua.
- `support-pages.browser.cjs`: ba trang ở 1440/390/320 px, không tràn ngang; ảnh tải được khi vào vùng nhìn; dropdown desktop bằng hover, Enter/Space, Tab, mũi tên và Escape; dropdown điện thoại bằng chạm; liên kết điều hướng, FAQ và mục lục; không lỗi JavaScript.
- Kiểm tra cú pháp JavaScript và `git diff --check` qua.

Để xuất lại bản xem trước: dùng Java 21, chạy `npm ci`, chạy Maven với `-Dtest=SupportPageMvcTest -Dsupport.preview=true test`, rồi `node src/test/js/support-pages.browser.cjs`. Kiểm tra trình duyệt dùng Puppeteer từ `package-lock.json`, có thể chỉ định trình duyệt bằng biến `SUPPORT_CHROME`. Ảnh và báo cáo nằm tại `target/support-preview/`.

Giới hạn kiểm tra: các trang mới và header/footer được render bằng Thymeleaf thật trong môi trường kiểm tra độc lập. Giỏ thu nhỏ trong bản xem trước trả dữ liệu rỗng mẫu; không chạy ứng dụng đầy đủ với cơ sở dữ liệu production. Liên kết thêm vào giỏ/checkout được kiểm tra từ template, và JavaScript checkout hiện có được kiểm tra hồi quy.
