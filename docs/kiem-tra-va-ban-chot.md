# Kiểm tra và bản chốt SMASH-VN

Repo công khai: https://github.com/LuongHiep334/SMASH-VN_Shop-ban-vot-cau-long

Bản chốt ngày **01/10/2026**, phiên bản ứng dụng **0.0.4**.

## Nội dung

- Bao gồm các cập nhật tìm kiếm tiếng Việt, giá sản phẩm, giỏ hàng, khôi phục yêu cầu thanh toán, đăng nhập, quản lý kho và trang hướng dẫn/chính sách.
- Cấu hình mẫu, schema cơ sở dữ liệu không chứa dữ liệu khách hàng, hướng dẫn chạy và GitHub Actions.
- Ảnh sản phẩm và bài viết phục vụ giao diện được giữ trong `uploads/product` và `uploads/blog`.
- Tệp `.env`, nhật ký ngrok, `node_modules`, thư mục thử nghiệm `scratch`, dữ liệu điều tra địa chỉ và tệp khách hàng tải lên không nằm trong bản công khai.
- Repo sử dụng lịch sử mới vì lịch sử cũ từng chứa `.env`. Lịch sử gốc vẫn được giữ trong repo trên máy; không đẩy các nhánh cũ lên repo công khai.

## Kiểm tra có thể chạy lại

Kết quả trên máy chốt với Java 21: **339 kiểm tra Java đạt**, không có bài lỗi hoặc bỏ qua; ứng dụng đóng gói thành JAR. Bộ kiểm tra JavaScript: **24/24 đạt**. Phạm vi chi tiết và giới hạn được ghi bên dưới.

Windows:

```powershell
.\mvnw.cmd -Pverification clean verify
npm ci
npm test
```

Linux/macOS:

```sh
sh mvnw -Pverification clean verify
npm ci
npm test
```

Profile `verification` chọn rõ 53 lớp kiểm tra đơn vị/MVC/template và kiểm tra tìm kiếm JPA bằng H2 trong bộ nhớ. Profile này tắt đọc `.env`, không kết nối SQL Server và không chạy các công cụ xóa/nạp dữ liệu. Danh sách nằm trong `pom.xml`; cần bổ sung khi có lớp kiểm tra độc lập mới.

Các kiểm tra JavaScript kiểm tra hành vi thanh toán, tìm kiếm bất đồng bộ, bàn phím, IME và bố cục trên màn hình nhỏ bằng trình duyệt Chromium. Puppeteer được cố định phiên bản trong `package-lock.json`.

## Giới hạn kiểm chứng

Kiểm tra độc lập không thay thế việc kiểm tra toàn bộ ứng dụng trên một SQL Server riêng. Chưa xác nhận lại giao dịch thực với SePay, GHN, SMTP hay Google OAuth trong lượt chốt này. Các lớp kiểm tra tích hợp và công cụ seed cũ vẫn được giữ để phát triển, nhưng một số có thao tác ghi/xóa dữ liệu hoặc phụ thuộc tệp `scratch` cục bộ; không chạy toàn bộ bằng `mvn test` trên cơ sở dữ liệu đang sử dụng.

## Khóa truy cập

`deploy.js` chỉ nhận `MCP_URL` và `MCP_TOKEN` qua biến môi trường và kiểm tra chứng chỉ HTTPS. Khóa mặc định của SePay/GHN đã bỏ khỏi mã nguồn; webhook từ chối khi chưa cấu hình khóa. Không ghi Authorization header vào log khi SePay xác thực thất bại.

Các khóa từng nằm trong lịch sử `.env` và khóa hosting cũ cần được thay hoặc thu hồi ở dịch vụ tương ứng. Việc loại chúng khỏi bản công khai không vô hiệu hóa các bản sao trong lịch sử cũ.

## Nguồn đóng góp

Bản này tiếp nối dự án của [tinhrenzi](https://github.com/tinhrenzi/SMASH-VN_Shop-ban-vot-cau-long), với các đóng góp của [LuongHiep334](https://github.com/LuongHiep334). Thông tin tác giả gốc được giữ trong README.
