# Kế hoạch demo bảo vệ đồ án SMASH VN cho 5 thành viên

> Mục tiêu của buổi demo không phải là đi qua mọi màn hình, mà chứng minh một chuỗi nghiệp vụ bán vợt cầu lông hoàn chỉnh, có kiểm soát dữ liệu, tồn kho, thanh toán, vận chuyển, hậu mãi và phân quyền.

## 1. Câu chuyện chung của buổi demo

Toàn đội dùng chung một tình huống xuyên suốt:

1. Khách hàng tìm một cây vợt phù hợp và tương tác với cửa hàng.
2. Cửa hàng cấu hình đúng sản phẩm, biến thể, lô nhập và ưu đãi.
3. Khách đặt hàng online, hệ thống tính lại giá, voucher và phí vận chuyển.
4. Nhân viên xử lý đơn, giao hàng, đổi/trả và phân loại hàng hoàn.
5. Cửa hàng bán tại quầy, kiểm soát giao dịch, phân quyền và xem hiệu quả kinh doanh.

Thời lượng đề xuất: **40 phút**.

- Giới thiệu chung: 3 phút.
- Mỗi thành viên: 7 phút, bằng nhau.
- Kết luận chung: 2 phút.

Trong 7 phút của mỗi người:

- 30 giây: nêu vai trò và vấn đề nghiệp vụ.
- 5 phút 30 giây: luồng chính.
- 45 giây: một tình huống hệ thống phải chặn.
- 15 giây: bàn giao cho thành viên tiếp theo.

## 2. Phân chia chức năng

| Thành viên | Vai trò trong câu chuyện | Chức năng chính phải demo | Logic trọng tâm phải nói |
|---|---|---|---|
| 1 | Khách hàng và trải nghiệm mua sắm | Đăng nhập, hồ sơ/địa chỉ, tìm kiếm–lọc, chi tiết sản phẩm, yêu thích, đánh giá hoặc chatbot | Chỉ sản phẩm/biến thể đang bán và còn hàng mới được mua; dữ liệu khách hàng thuộc đúng tài khoản |
| 2 | Quản trị danh mục, sản phẩm, kho và marketing | Danh mục/thương hiệu, thuộc tính động, sản phẩm–biến thể, nhập lô, khuyến mãi và voucher | Giá và tồn kho ở cấp biến thể; nhập kho theo lô; chống khuyến mãi chồng lấn; voucher có thời gian, số lượt, đơn tối thiểu và mức giảm tối đa |
| 3 | Bán hàng online | Giỏ hàng, mua ngay/chọn sản phẩm, checkout, địa chỉ GHN, phí ship, voucher, COD và giải thích SePay, lịch sử đơn | Không tin giá/phí ship từ trình duyệt; khóa biến thể và voucher; COD chờ xác nhận, SePay chờ thanh toán; chống thanh toán lặp |
| 4 | Vận hành đơn hàng và hậu mãi | Xác nhận đơn, FIFO, chuỗi trạng thái, GHN, yêu cầu trả/đổi, kiểm định hàng hoàn, hoàn tiền hoặc giao hàng đổi | Chỉ chuyển trạng thái hợp lệ; trả/đổi trong 7 ngày từ lúc giao thật; chỉ nhập lại kho sau kiểm định; hàng lỗi tách riêng |
| 5 | Bán tại quầy, tài chính và quản trị hệ thống | POS, tiền mặt/QR SePay, in hóa đơn, giao dịch, thống kê–Excel, tài khoản nhân viên và RBAC | POS trừ FIFO ngay khi hoàn tất; doanh thu chỉ ghi nhận đơn đã giao/hoàn tất; hoàn tiền đảo doanh thu; quyền được chặn ở máy chủ |

Các chức năng phụ như blog, newsletter, quên mật khẩu, Google OAuth hoặc điều phối từ khóa xấu chỉ dùng khi hội đồng hỏi thêm; không chen tất cả vào luồng chính.

## 3. Kịch bản chi tiết từng thành viên

### Thành viên 1 — Khách hàng và trải nghiệm mua sắm

**Mục tiêu:** chứng minh hệ thống giúp khách tìm đúng sản phẩm và quản lý thông tin cá nhân, không chỉ là một trang danh sách hàng hóa.

**Luồng demo:**

1. Đăng nhập tài khoản khách đã chuẩn bị sẵn.
2. Vào hồ sơ, chỉ ra thông tin cá nhân và sổ địa chỉ; chọn một địa chỉ đã chuẩn hóa tỉnh, quận/huyện, phường/xã.
3. Mở cửa hàng, tìm theo tên rồi lọc theo danh mục, thương hiệu, giá hoặc thuộc tính vợt.
4. Mở chi tiết sản phẩm, chọn một biến thể; chỉ ra giá niêm yết, giá đang giảm, thuộc tính và số lượng có thể mua.
5. Thêm sản phẩm vào yêu thích, sau đó thêm đúng biến thể vào giỏ.
6. Nếu còn thời gian, mở một đánh giá đã mua hàng hoặc hỏi chatbot một câu về cách chọn vợt.

**Tình huống chặn:** chọn số lượng lớn hơn tồn kho hoặc một biến thể đã ngừng bán. Kết quả mong đợi là không thể thêm hợp lệ vào giỏ.

**Lời thuyết minh cốt lõi:**

> Hệ thống kiểm tra khả năng bán ở cả sản phẩm, danh mục và biến thể. Giao diện có thể hỗ trợ chọn nhanh, nhưng phía máy chủ vẫn kiểm tra trạng thái và tồn kho để người dùng không thể sửa yêu cầu rồi mua hàng không hợp lệ.

**Câu bàn giao:**

> Sản phẩm khách vừa chọn được hình thành từ danh mục, thuộc tính, biến thể và các lô nhập. Thành viên 2 sẽ trình bày cách cửa hàng quản trị phần dữ liệu nguồn này.

### Thành viên 2 — Danh mục, sản phẩm, kho và marketing

**Mục tiêu:** chứng minh dữ liệu bán hàng có cấu trúc, có lịch sử nhập và có luật khuyến mãi rõ ràng.

**Luồng demo:**

1. Đăng nhập quyền quản lý và mở danh mục/thương hiệu; giải thích đây là dữ liệu dùng cho lọc sản phẩm.
2. Mở sản phẩm demo đã chuẩn bị; chỉ ra mô tả, ảnh, danh mục, thương hiệu và trạng thái hiển thị.
3. Mở danh sách biến thể; giải thích thuộc tính động tạo ra các phân loại bán cụ thể, mỗi biến thể có giá và tồn kho riêng.
4. Nhập thêm một lô nhỏ cho biến thể demo, có số lượng, giá nhập và ghi chú; mở lịch sử lô để đối chiếu.
5. Mở đợt giảm giá đang áp dụng cho sản phẩm; chỉ ra phần trăm giảm và thời gian hiệu lực.
6. Mở voucher `DEMO10`; chỉ ra đơn tối thiểu, số lượt còn lại và mức giảm tối đa.

**Tình huống chặn:** thử tạo một đợt giảm giá trùng thời gian trên chính sản phẩm đang có khuyến mãi. Kết quả mong đợi là hệ thống từ chối chồng lấn.

**Lời thuyết minh cốt lõi:**

> Kho được quản lý theo lô chứ không chỉ tăng một con số tổng. Khi xuất bán, hệ thống phân bổ theo FIFO để giá vốn và lượng tồn có thể truy vết. Khuyến mãi tác động lên giá sản phẩm, còn voucher tác động lên tổng đơn; hai khái niệm được lưu và kiểm tra riêng.

**Công thức nên nói đúng:**

- Giá bán sau khuyến mãi = giá niêm yết × (1 − phần trăm giảm/100).
- Voucher phần trăm = giá trị nhỏ hơn giữa số tiền giảm theo phần trăm và mức giảm tối đa.
- Tổng thanh toán = tạm tính − giảm voucher + phí vận chuyển.
- Mỗi đơn chỉ áp dụng một voucher.

**Câu bàn giao:**

> Sản phẩm đã có tồn kho và ưu đãi hợp lệ. Thành viên 3 sẽ dùng chính dữ liệu này để tạo một đơn online.

### Thành viên 3 — Giỏ hàng, checkout và thanh toán online

**Mục tiêu:** chứng minh hệ thống tạo đơn từ một ngữ cảnh checkout an toàn, không lấy giá hay phí vận chuyển do trình duyệt tự khai báo.

**Luồng demo ưu tiên COD để ổn định:**

1. Quay lại cửa sổ khách hàng, mở giỏ; thay đổi số lượng và chỉ chọn sản phẩm muốn thanh toán.
2. Bấm thanh toán; giải thích hệ thống tạo ngữ cảnh checkout một lần cho đúng các dòng đã chọn, hỗ trợ cả giỏ hàng và “mua ngay”.
3. Chọn địa chỉ đã lưu hoặc nhập địa chỉ mới đầy đủ; hệ thống ánh xạ địa chỉ GHN và tính phí ở máy chủ.
4. Áp dụng voucher `DEMO10`, đọc đủ tạm tính, giảm giá, phí vận chuyển và tổng tiền.
5. Chọn COD và đặt hàng. Kết quả mong đợi: đơn ở trạng thái **Chờ xác nhận**, thanh toán **Chờ thanh toán**, các dòng đã mua bị xóa khỏi giỏ nhưng dòng không chọn vẫn còn.
6. Mở lịch sử/chi tiết đơn và chỉ ra ảnh chụp tên sản phẩm, SKU/thuộc tính, đơn giá, voucher và địa chỉ nhận tại thời điểm đặt.

**Tình huống chặn:** áp voucher khi chưa đủ giá trị đơn tối thiểu hoặc dùng địa chỉ thiếu mã quận/phường GHN. Kết quả mong đợi là hệ thống từ chối và nói rõ nguyên nhân.

**Giải thích SePay, chỉ demo trực tiếp khi webhook đã được thử trước:**

- Đơn SePay ban đầu là **Chờ thanh toán**, chưa được coi là đơn đã tiếp nhận để xử lý.
- Khi ngân hàng/SePay gửi giao dịch hợp lệ, hệ thống đối chiếu mã đơn và số tiền, chống xử lý trùng theo mã giao dịch.
- Chỉ sau thanh toán thành công hệ thống mới trừ kho FIFO, giảm lượt voucher và chuyển đơn sang **Chờ xác nhận**.
- Nếu quá hạn, đơn được đánh dấu hủy/hết hạn và không làm rác lịch sử mua hàng của khách.
- Nếu tiền đến sau khi đơn đã hết hạn, hệ thống ghi nhận trường hợp “nhận tiền sau hủy” để nhân viên xử lý, không tự kích hoạt lại đơn và không tự trừ kho.

**Lời thuyết minh cốt lõi:**

> Trước khi lưu đơn, máy chủ khóa và đọc lại từng biến thể, tính lại giá hiện hành, kiểm tra tồn, khóa voucher rồi tự tính phí giao hàng. Vì vậy sửa giá trên giao diện hoặc gửi lại một yêu cầu cũ không làm sai tổng tiền hay bán vượt kho.

**Câu bàn giao:**

> Đơn online đã được ghi nhận nhưng chưa phải doanh thu. Thành viên 4 sẽ tiếp tục từ lúc cửa hàng xác nhận đến giao hàng và xử lý hậu mãi.

### Thành viên 4 — Xử lý đơn, vận chuyển và đổi/trả

**Mục tiêu:** chứng minh trạng thái đơn, tồn kho và hoàn tiền luôn di chuyển theo đúng nghiệp vụ.

**Phần A — xử lý đơn vừa tạo:**

1. Đăng nhập nhân viên, mở đơn COD ở trạng thái **Chờ xác nhận**.
2. Xác nhận đơn. Đây là thời điểm COD được trừ kho FIFO; nếu thiếu kho thì thao tác bị từ chối.
3. Chuyển lần lượt qua **Đã xác nhận → Đang chuẩn bị hàng → Sẵn sàng giao**.
4. Tại **Sẵn sàng giao**, bấm gửi sang GHN để tạo vận đơn. Không được tự chuyển thành “Đã tạo vận đơn GHN” khi chưa có mã GHN.
5. Sau khi có mã vận đơn, trạng thái lấy/giao hàng do GHN đồng bộ; không trình bày rằng nhân viên có thể tự ý nhảy thẳng tới “Đã giao”.

**Phần B — dùng một đơn đã giao chuẩn bị sẵn để demo hậu mãi:**

1. Ở cửa sổ khách, mở đơn đã giao trong vòng 7 ngày và gửi yêu cầu `TRA` hoặc `DOI`, chọn dòng hàng, số lượng, lý do và video bằng chứng.
2. Ở cửa sổ quản trị, phê duyệt yêu cầu; nếu là đổi hàng, hệ thống phải kiểm tra biến thể đổi còn đủ tồn.
3. Khi hàng về cửa hàng, thực hiện kiểm định một trong ba kết quả:
   - `BAN_LAI`: hàng đạt chất lượng, hoàn lại kho bán được theo lô.
   - `HANG_LOI`: không cộng vào kho bán được, chuyển sang kho sản phẩm lỗi.
   - `TU_CHOI`: phải có lý do và tổ chức trả lại hàng cho khách.
4. Sau kiểm định mới xác nhận hoàn tiền hoặc tạo lô hàng đổi. Với đổi hàng, tồn của hàng đổi được phân bổ trước khi giao.

**Tình huống chặn:** thử yêu cầu đổi/trả một đơn chưa giao hoặc đã quá 7 ngày. Hệ thống phải từ chối. Nếu không có sẵn đơn quá hạn, chỉ mở dữ liệu đã chuẩn bị và giải thích thay vì sửa ngày trực tiếp lúc demo.

**Lời thuyết minh cốt lõi:**

> Mốc 7 ngày được tính từ thời điểm giao thành công thực tế trong lịch sử trạng thái, không lấy ngày tạo đơn. Hệ thống không tự nhập kho ngay khi khách bấm trả hàng vì chưa biết hàng còn bán được hay đã hỏng. Hoàn tiền cũng có khóa chống hoàn hai lần và mã giao dịch hoàn không được trùng.

**Câu bàn giao:**

> Luồng online và hậu mãi đã hoàn tất. Thành viên 5 sẽ chứng minh cửa hàng còn vận hành tại quầy và dữ liệu giao dịch được tổng hợp đúng cho quản lý.

### Thành viên 5 — POS, giao dịch, phân quyền và thống kê

**Mục tiêu:** khép lại bài toán bán hàng đa kênh và chứng minh dữ liệu quản trị không bị cộng doanh thu sai.

**Luồng demo:**

1. Mở POS bằng tài khoản nhân viên; tìm nhanh sản phẩm, chọn biến thể và số lượng.
2. Tìm khách hàng hoặc chọn khách lẻ; có thể giới thiệu chức năng đăng ký nhanh khách tại quầy nhưng không cần tạo mới nếu thời gian ngắn.
3. Áp voucher hợp lệ, chọn tiền mặt, nhập số tiền khách đưa và chỉ ra tiền thừa. Hoàn tất hóa đơn.
4. Mở bản in K80 hoặc A5; chỉ ra mã hóa đơn, dòng hàng, giảm giá, số tiền và người bán.
5. Mở danh sách giao dịch để đối chiếu giao dịch POS/online, trạng thái và khả năng xuất Excel/CSV.
6. Đăng nhập quyền quản lý, mở thống kê theo khoảng ngày; chỉ ra doanh thu thực, giá vốn FIFO, lợi nhuận, đơn hủy/hoàn, sản phẩm bán chạy/chậm và xuất Excel.
7. Chốt phân quyền: nhân viên được xử lý sản phẩm, đơn, POS, khách hàng, giao dịch, đánh giá và blog; chỉ quản lý được quản lý nhân viên, từ khóa kiểm duyệt, khuyến mãi và thống kê.

**Tình huống chặn:** đăng nhập tài khoản nhân viên rồi truy cập thống kê hoặc quản lý nhân viên bằng đường dẫn trực tiếp. Kết quả mong đợi là bị chặn ở máy chủ, không chỉ là ẩn menu.

**Lời thuyết minh cốt lõi:**

> Hóa đơn POS hoàn tất trừ kho FIFO ngay trong giao dịch. Với online, doanh thu thực chỉ tính khi đơn đã giao hoặc hoàn tất; đơn đang vận chuyển chỉ là doanh thu dự kiến, đơn chờ và đơn hủy bị loại. Khoản hoàn tiền phải đảo ảnh hưởng doanh thu nên báo cáo không cộng doanh số ảo.

**Lời kết cho cả nhóm:**

> Năm phần vừa trình bày nối thành một chuỗi dữ liệu duy nhất: từ nhu cầu khách hàng, cấu hình hàng hóa, giao dịch, vận hành hậu mãi đến báo cáo quản trị. Điểm chúng em tập trung không chỉ là đủ màn hình, mà là bảo toàn giá, tồn kho, trạng thái, quyền truy cập và khả năng truy vết ở mọi bước.

## 4. Dữ liệu bắt buộc chuẩn bị trước buổi bảo vệ

### 4.1. Tài khoản

Tài khoản trong file khởi tạo CSDL hiện tại:

| Vai trò | Tài khoản | Mật khẩu | Dùng cho |
|---|---|---|---|
| Quản lý `QL` | `admin` | `123456` | Thành viên 2 và 5 |
| Nhân viên `NV` | `nhanvien1@smashvn.com` | `123456` | Thành viên 4 và POS |
| Khách hàng `KH` | `khachhang1@gmail.com` | `123456` | Thành viên 1 và 3 |

Tên tài khoản trong `ReadMe.md` có chỗ khác với file SQL hiện tại. Phải đăng nhập thử cả ba tài khoản trên đúng CSDL sẽ mang đi bảo vệ; không học thuộc thông tin trong tài liệu rồi đến hôm demo mới kiểm tra.

Nên dùng ba hồ sơ trình duyệt tách biệt:

- Cửa sổ A: khách hàng.
- Cửa sổ B: nhân viên.
- Cửa sổ C: quản lý.

### 4.2. Sản phẩm và kho

Chuẩn bị một sản phẩm duy nhất làm “sản phẩm xuyên suốt”, đáp ứng tất cả điều kiện:

- Danh mục và thương hiệu đang hoạt động.
- Có ảnh đẹp, mô tả ngắn, thông số đủ để lọc.
- Ít nhất 2 biến thể để minh họa thuộc tính và đổi hàng.
- Biến thể A có ít nhất 10 sản phẩm bán được.
- Có ít nhất 2 lô nhập với giá nhập khác nhau để giải thích FIFO.
- Không dùng sản phẩm đang bị một thành viên khác sửa trong lúc demo.

Ghi sẵn ra giấy: tên sản phẩm, ID hoặc SKU biến thể A/B, tồn trước demo và giá bán. Sau POS/đặt đơn, đối chiếu tồn giảm đúng số lượng.

### 4.3. Khuyến mãi và voucher

Tạo trước, không tạo lần đầu trên sân khấu:

- Đợt `DEMO FLASH SALE`: giảm 10%, đang hiệu lực, áp dụng đúng sản phẩm demo.
- Voucher `DEMO10`: giảm 10%, đơn tối thiểu 500.000đ, tối đa 100.000đ, còn ít nhất 10 lượt, đang hiệu lực.
- Một voucher `DEMO-HETHAN` hoặc `DEMO-TOITHIEU` để trình bày tình huống bị chặn.

Không dùng cùng một voucher cho quá nhiều lần diễn tập nếu số lượt ít. Trước giờ bảo vệ, kiểm tra số lượt còn lại.

### 4.4. Đơn hàng chuẩn bị sẵn

Không cố tạo và đẩy một đơn duy nhất qua toàn bộ vòng đời ngay trên sân khấu. Chuẩn bị các đơn độc lập:

| Mã gợi nhớ | Trạng thái ban đầu | Mục đích |
|---|---|---|
| `DEMO-COD-MOI` | Tạo trực tiếp ở buổi demo, Chờ xác nhận | TV3 đặt hàng, TV4 xác nhận và minh họa trừ FIFO |
| `DEMO-GHN` | Sẵn sàng giao, địa chỉ GHN hợp lệ | TV4 tạo vận đơn mà không mất thời gian đi qua mọi bước |
| `DEMO-TRA` | Đã giao trong 1–3 ngày gần nhất | Trả hàng và hoàn tiền |
| `DEMO-DOI` | Đã giao trong 1–3 ngày gần nhất | Đổi sang biến thể B còn đủ tồn |
| `DEMO-QUAHAN` | Đã giao trên 7 ngày | Chứng minh bị chặn nếu có thời gian |
| `DEMO-POS` | Chưa tạo | TV5 bán tiền mặt tại quầy |

Đơn dùng cho đổi/trả phải có lịch sử/audit ghi nhận thời điểm `da_giao`. Chỉ sửa trường ngày tạo đơn là không đủ, vì hệ thống cố ý dùng thời điểm giao thật.

Chuẩn bị một video MP4 ngắn, nhẹ và hợp lệ làm bằng chứng trả hàng. Mở thử file trước khi demo.

### 4.5. Dữ liệu thống kê

Có thể dùng `scratch/demo-statistics-seed.sql` để bổ sung dữ liệu thống kê và `scratch/demo-statistics-rollback.sql` để thu hồi đúng dữ liệu đó. Chạy thử trên bản sao CSDL trước, kiểm tra lại khoảng ngày cần hiển thị.

**Không chạy** `scratch/BadmintonShopDB1_ban_moi_nhat_.sql` ngay trước buổi bảo vệ: file này xóa và tạo lại CSDL, có thể làm mất toàn bộ dữ liệu diễn tập. Hãy sao lưu CSDL sau khi đã chuẩn bị xong.

## 5. Trình tự diễn tập và điểm đối chiếu

### Lần chạy 1 — đúng nghiệp vụ

- Bấm đúng trình tự, chưa cần nói.
- Ghi lại tổng tiền trước/sau voucher, phí ship, tồn kho trước/sau, số lượt voucher trước/sau.
- Kiểm tra tất cả cửa sổ đăng nhập không bị hết phiên.

### Lần chạy 2 — nói đúng thời gian

- Mỗi người tự bấm và nói trong 7 phút.
- Không để người sau phải tìm lại đơn/sản phẩm của người trước.
- Nếu quá 7 phút, cắt chức năng phụ trước; không cắt luật nghiệp vụ chính.

### Lần chạy 3 — diễn tập sự cố

- Tắt mạng và tập dùng dữ liệu/ảnh dự phòng cho GHN, Gemini, Google OAuth, SMTP, SePay.
- Cố ý dùng voucher sai và số lượng vượt tồn để chắc chắn thông báo dễ hiểu.
- Thử refresh, double-click và quay lại trang thanh toán để biết hệ thống phản ứng.
- Thử tài khoản nhân viên truy cập trang chỉ dành cho quản lý.

### Bảng đối chiếu nhanh

| Thời điểm | Giá/tồn/voucher cần kiểm tra |
|---|---|
| Thêm giỏ | Chưa trừ tồn, chỉ kiểm tra số lượng có thể mua |
| Tạo đơn COD | Đơn Chờ xác nhận; chưa trừ tồn FIFO |
| Xác nhận COD | Trừ tồn FIFO; nếu thiếu kho thì không xác nhận |
| Tạo đơn SePay | Đơn Chờ thanh toán; chưa trừ tồn và chưa tiêu lượt voucher |
| SePay thành công | Đối chiếu đủ tiền, trừ FIFO, tiêu lượt voucher, chuyển Chờ xác nhận |
| Hủy đơn đã từng trừ kho nhưng chưa bàn giao vận chuyển | Hoàn kho một lần, hoàn lượt voucher khi phù hợp |
| Hàng hoàn vừa về | Chưa tự cộng kho bán được |
| Kiểm định `BAN_LAI` | Cộng lại kho bán được |
| Kiểm định `HANG_LOI` | Chỉ tăng kho lỗi |
| POS hoàn tất | Trừ FIFO trong cùng giao dịch bán hàng |

## 6. Phương án dự phòng cho tích hợp ngoài hệ thống

| Tích hợp | Dấu hiệu đã sẵn sàng | Phương án nếu không ổn định |
|---|---|---|
| GHN | Lấy được tỉnh/quận/phường, tính phí và tạo mã vận đơn thử | Demo phí/địa chỉ đã lưu và mở đơn có mã GHN chuẩn bị trước; nói rõ đây là dữ liệu dự phòng |
| SePay | QR đúng số tiền/nội dung, webhook đến được máy demo | Dùng COD cho luồng chính; giải thích trạng thái bằng đơn SePay đã chuẩn bị và bản ghi giao dịch, không giả vờ webhook đang chạy |
| Google OAuth | Đăng nhập thử đúng callback trên máy demo | Dùng tài khoản/mật khẩu đã tạo sẵn |
| Gemini chatbot | Có khóa API, quota và Internet | Chỉ mở một hội thoại đã kiểm tra hoặc bỏ khỏi luồng chính |
| Gmail/newsletter | Gửi thử đến hộp thư demo, kiểm tra spam | Mở lịch sử/trạng thái gửi hoặc bỏ thao tác gửi trực tiếp |

Nếu dùng webhook từ Internet vào máy cục bộ, phải kiểm tra URL công khai, firewall và cấu hình callback sau mỗi lần đổi mạng. Không dùng nút xác nhận thủ công thanh toán POS để nói rằng đó là webhook tự động; hãy giới thiệu đúng là phương án do nhân viên được ủy quyền xác nhận.

## 7. Những điểm tuyệt đối không trình bày sai

1. Không nói “đặt đơn là trừ kho ngay” cho mọi phương thức:
   - COD trừ FIFO khi nhân viên xác nhận.
   - SePay trừ FIFO khi thanh toán thành công.
2. Không nói phí GHN hoặc tổng tiền do JavaScript quyết định; máy chủ tính và kiểm tra lại.
3. Không nói đơn có thể nhảy trạng thái tùy ý; hệ thống có ma trận chuyển trạng thái và khóa chống cập nhật đồng thời.
4. Không nói hàng trả về là nhập kho ngay; phải kiểm định `BAN_LAI`, `HANG_LOI` hoặc `TU_CHOI`.
5. Không nói đơn vừa tạo đã là doanh thu; doanh thu thực của online chỉ tính khi đã giao/hoàn tất.
6. Không gọi QR thanh toán POS là QR theo dõi hóa đơn; đó là QR chuyển khoản ngân hàng.
7. Không chọn ZaloPay để demo: mã dự án còn giá trị tương thích cũ, nhưng luồng thanh toán hiện tại là COD và SePay.
8. Không nói nhân viên có quyền xem khuyến mãi hay thống kê. Theo phân quyền hiện tại, các trang đó chỉ dành cho `QL`.
9. Không nói toàn bộ kiểm thử đã xanh nếu chưa xử lý môi trường OAuth loopback. Các kiểm thử mục tiêu vừa sửa đã đạt, nhưng lần chạy toàn bộ còn lỗi khởi tạo ngữ cảnh do kết nối loopback của môi trường chạy.

## 8. Câu hỏi phản biện và câu trả lời ngắn

### Vì sao hóa đơn lưu ảnh chụp tên sản phẩm, SKU, thuộc tính và giá?

Vì dữ liệu sản phẩm có thể đổi sau khi bán. Hóa đơn phải giữ đúng nội dung tại thời điểm giao dịch để in lại, đối soát và giải quyết khiếu nại.

### Làm sao hạn chế bán vượt kho khi hai người mua cùng lúc?

Khi tạo/xác nhận giao dịch, hệ thống dùng transaction, khóa ghi biến thể hoặc lô theo thứ tự ổn định và kiểm tra lại tồn. Chỉ một giao dịch được phân bổ FIFO thành công; giao dịch còn lại nhận lỗi thiếu kho thay vì làm tồn âm.

### Vì sao dùng FIFO?

FIFO ưu tiên xuất lô nhập trước, giúp truy vết tồn và tính giá vốn theo lô. Nó phù hợp vận hành kho và làm lợi nhuận phản ánh giá nhập thực tế thay vì dùng một giá vốn giả định.

### Làm sao chống một webhook SePay được gửi nhiều lần?

Giao dịch có mã nhận diện duy nhất và trạng thái đơn được khóa khi xử lý. Nếu mã giao dịch đã tồn tại hoặc đơn đã thanh toán, hệ thống không trừ kho và không ghi nhận thanh toán lần hai.

### Nếu khách chuyển sai số tiền thì sao?

Hệ thống không tự xác nhận đơn như một khoản thanh toán hợp lệ; giao dịch được giữ để đối soát theo trạng thái chênh lệch số tiền.

### Vì sao đơn SePay hết hạn không hiện trong lịch sử mua hàng?

Đó mới là ý định thanh toán chưa hoàn tất, chưa phải đơn cửa hàng đã tiếp nhận. Ẩn bản ghi hết hạn giúp lịch sử khách hàng không bị lẫn với đơn thật, trong khi hệ thống vẫn giữ dữ liệu để audit và xử lý tiền đến muộn.

### Làm sao voucher không bị dùng quá số lượt?

Voucher được khóa khi kiểm tra, xác minh trạng thái, thời gian, đơn tối thiểu và số lượt. Lượt dùng chỉ được giảm ở mốc nghiệp vụ phù hợp; khi hủy hợp lệ, hệ thống hoàn lại một lần.

### Vì sao mốc trả hàng không tính từ ngày tạo đơn?

Khách chỉ có thể kiểm tra sản phẩm sau khi nhận. Vì vậy 7 ngày phải bắt đầu từ sự kiện giao thành công trong lịch sử trạng thái, không phải lúc đặt hoặc thanh toán.

### Vì sao không hoàn kho ngay khi chấp nhận yêu cầu trả?

Hàng đang ở phía khách hoặc đang vận chuyển về và chưa được kiểm định. Cộng ngay vào tồn bán được có thể khiến cửa hàng bán tiếp hàng hỏng hoặc hàng chưa thực nhận.

### Hoàn tiền có làm tăng tồn kho không?

Không mặc định. Hoàn tiền là nghiệp vụ tài chính; nhập lại kho là kết quả kiểm định vật lý. Hai bước liên quan nhưng phải được kiểm soát riêng để không cộng kho sai.

### Doanh thu và lợi nhuận được tính thế nào?

Doanh thu thực lấy đơn online đã giao/hoàn tất và hóa đơn POS hoàn tất; loại đơn chờ, thất bại, hủy và điều chỉnh khoản hoàn. Lợi nhuận dựa trên doanh thu thuần trừ giá vốn FIFO.

### Phân quyền có phải chỉ ẩn nút trên giao diện?

Không. Tuyến quản trị bị giới hạn theo vai trò và các service quan trọng còn kiểm tra người thực hiện. Gọi trực tiếp đường dẫn hoặc sửa request vẫn bị từ chối và các thao tác nhạy cảm có audit log.

### Tại sao chia hệ thống thành controller, service và repository?

Controller nhận yêu cầu và điều hướng; service giữ luật nghiệp vụ và transaction; repository truy cập dữ liệu. Cách chia này tránh nhét luật vào giao diện, dễ kiểm thử và cho phép web, webhook hay tác vụ quản trị dùng chung một nghiệp vụ.

## 9. Checklist 30 phút trước khi demo

- [ ] Bản sao CSDL demo đã được sao lưu.
- [ ] Ứng dụng khởi động đúng Java 21 và kết nối đúng CSDL.
- [ ] Ba tài khoản khách/nhân viên/quản lý đăng nhập được.
- [ ] Ba cửa sổ trình duyệt đang ở đúng vai trò, không lưu nhầm mật khẩu.
- [ ] Sản phẩm demo còn tồn, biến thể đổi hàng còn đủ số lượng.
- [ ] Hai lô nhập hiện rõ trong lịch sử để giải thích FIFO.
- [ ] `DEMO FLASH SALE` và `DEMO10` đang hiệu lực, không chồng đợt khác.
- [ ] Địa chỉ khách có đủ mã GHN; phí vận chuyển tính được.
- [ ] Đơn trả/đổi có thời điểm giao thật trong 7 ngày.
- [ ] Video bằng chứng mở và tải lên được.
- [ ] Máy in hoặc màn hình xem trước hóa đơn POS hoạt động.
- [ ] Khoảng ngày thống kê có dữ liệu; file Excel xuất và mở được.
- [ ] URL SePay/GHN và Internet đã thử; ảnh/dữ liệu dự phòng đã mở sẵn.
- [ ] Tắt thông báo, ứng dụng chat và cửa sổ chứa mật khẩu/khóa API.
- [ ] Mỗi người có một tờ ghi: tài khoản, URL/màn hình bắt đầu, mã đơn và câu bàn giao.

## 10. Tiêu chí chấm nội bộ sau buổi diễn tập

Chấm mỗi thành viên trên thang 10:

- 2 điểm: thao tác đúng, không cần người khác cứu.
- 2 điểm: nói đúng quy tắc nghiệp vụ và thời điểm thay đổi dữ liệu.
- 2 điểm: chứng minh được kết quả trước/sau, không chỉ thông báo “thành công”.
- 2 điểm: xử lý được tình huống bị chặn hoặc mất tích hợp ngoài.
- 1 điểm: đúng 7 phút.
- 1 điểm: trả lời được ít nhất hai câu phản biện thuộc phần mình.

Mỗi chức năng trong buổi bảo vệ phải trả lời được bốn câu: **Ai được làm? Điều kiện nào cho phép? Dữ liệu nào thay đổi? Nếu lỗi thì hệ thống bảo toàn điều gì?**
