package com.smashvn.shop.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.dto.order.CheckoutContext;
import com.smashvn.shop.dto.order.CheckoutItemContext;
import com.smashvn.shop.dto.order.CheckoutSource;
import com.smashvn.shop.dto.order.OrderCreationResult;
import com.smashvn.shop.dto.order.PurchasedItemSnapshot;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.GioHang;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentMethod;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.repository.GioHangRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.service.admin.AdminShippingService;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.api.ShippingFeeCalculator;
import com.smashvn.shop.service.product.PriceSnapshot;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.service.product.ProductAvailabilityService;
import com.smashvn.shop.util.PhoneUtils;
import com.smashvn.shop.util.VoucherCalculator;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GioHangService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public static final int MAX_CART_QUANTITY = 999;

    private final KhachHangRepository khachHangRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final ShippingFeeCalculator shippingFeeCalculator;
    private final AdminShippingService adminShippingService;
    private final GhnService ghnService;
    private final SoDiaChiRepository soDiaChiRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PricingService pricingService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final GuestCartService guestCartService;
    private final com.smashvn.shop.service.inventory.InventoryLotService inventoryLotService;
    private final ProductAvailabilityService productAvailabilityService;

    private KhachHang getOrCreateKhachHang(Integer idTaiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            TaiKhoan taiKhoan = taiKhoanRepository.findById(idTaiKhoan)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
            kh = new KhachHang();
            kh.setTaiKhoan(taiKhoan);
            kh.setHoKh("");
            String username = taiKhoan.getUsername();
            String name = "Người dùng";
            if (username != null) {
                if (username.contains("@")) {
                    name = username.split("@")[0];
                } else {
                    name = username;
                }
            }
            kh.setTenKh(name);
            kh.setSoDienThoaiKh("");
            kh.setNhanBanTin(false);
            kh.setLaTaiKhoanNoiBo("QL".equals(taiKhoan.getVaiTro()) || "NV".equals(taiKhoan.getVaiTro()));
            kh = khachHangRepository.save(kh);
        }
        return kh;
    }

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> themVaoGio(Integer idTaiKhoan, Integer idSanPhamChiTiet, Integer soLuong) {
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm thêm vào giỏ hàng phải lớn hơn 0.");
        }
        if (soLuong > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Số lượng sản phẩm thêm vào giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        if (idSanPhamChiTiet == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ.");
        }

        KhachHang khachHang = getOrCreateKhachHang(idTaiKhoan);

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setKhachHang(khachHang);
            gioHang = gioHangRepository.save(gioHang);
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), idSanPhamChiTiet);
        SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(idSanPhamChiTiet)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (!productAvailabilityService.isVariantPublished(spct)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Phân loại sản phẩm này đã ngừng bán!");
        }

        // --- BÀI TOÁN VALIDATE TỒN KHO THỰC TẾ ---
        // 1. Xem trong giỏ của khách đã có bao nhiêu chiếc này rồi?
        // Avoid possible NPE from auto-unboxing by capturing Integer first
        Integer soLuongHienCoObj = (chiTiet != null) ? chiTiet.getSoLuong() : null;
        int soLuongHienCo = (soLuongHienCoObj != null) ? soLuongHienCoObj : 0;

        // 2. Tính tổng số lượng khách sẽ có nếu thêm thành công
        long tongYeuCauLong = (long) soLuongHienCo + soLuong;
        if (tongYeuCauLong > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Tổng số lượng sản phẩm này trong giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        int tongYeuCau = (int) tongYeuCauLong;

        // 3. Khóa chặn: Nếu tổng yêu cầu vượt quá kho
        if (spct.getSoLuongTon() < tongYeuCau) {
            if (soLuongHienCo > 0) {
                throw new RuntimeException("Bạn đã có " + soLuongHienCo + " sản phẩm này trong giỏ. Không thể thêm vượt quá số lượng tồn kho (" + spct.getSoLuongTon() + ")!");
            } else {
                throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + spct.getSoLuongTon() + " sản phẩm.");
            }
        }

        // --- XỬ LÝ LƯU VÀO GIỎ ---
        if (chiTiet != null) {
            chiTiet.setSoLuong(tongYeuCau); // Đã có thì cộng dồn
        } else {
            chiTiet = new GioHangChiTiet(); // Chưa có thì tạo mới
            chiTiet.setGioHang(gioHang);
            chiTiet.setSanPhamChiTiet(spct);
            chiTiet.setSoLuong(soLuong);
        }

        gioHangChiTietRepository.save(chiTiet);

        // Đóng gói dữ liệu trả về cho Modal JS
        Map<String, Object> result = new HashMap<>();
        result.put("tenSanPham", spct.getSanPham().getTenSanPham());
        String sizeOrWeight = spct.getTrongLuong() != null && !spct.getTrongLuong().isBlank() ? spct.getTrongLuong() : (spct.getKichThuoc() != null ? spct.getKichThuoc() : "");
        String phanLoaiStr = (spct.getMauSac() != null ? spct.getMauSac() : "") + (!sizeOrWeight.isEmpty() ? " | " + sizeOrWeight : "");
        result.put("phanLoai", phanLoaiStr);
        result.put("giaBan", pricingService.calculateCurrentSellingPrice(spct));
        result.put("hinhAnh", spct.getHinhAnhSanPham());
        result.put("cartItemId", chiTiet.getId());
        result.put("soLuongThem", soLuong);

        return result;

    }

    public List<GioHangChiTiet> layDanhSachSanPhamTrongGio(Integer idTaiKhoan) {
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            return new ArrayList<>();
        }
        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            return new ArrayList<>();
        }
        return gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
    }

    // SỬA ĐỔI 2: Gom toàn bộ logic tính tiền, gom JSON của Mini Cart vào Service
    public Map<String, Object> layDuLieuMiniCart(Integer idTaiKhoan) {
        List<GioHangChiTiet> danhSach = layDanhSachSanPhamTrongGio(idTaiKhoan);

        BigDecimal tongTien = BigDecimal.ZERO;
        int tongSoLuong = 0;
        List<Map<String, Object>> danhSachMini = new ArrayList<>();

        for (GioHangChiTiet item : danhSach) {
            SanPham sp = item.getSanPhamChiTiet().getSanPham();
            int tonKho = item.getSanPhamChiTiet().getSoLuongTon();
            boolean hopLe = tonKho > 0
                    && productAvailabilityService.isVariantPublished(item.getSanPhamChiTiet())
                    && item.getSoLuong() != null
                    && item.getSoLuong() > 0;
            PriceSnapshot priceSnapshot = pricingService.buildPriceSnapshot(item.getSanPhamChiTiet());

            if (hopLe) {
                BigDecimal gia = priceSnapshot.giaBanSauGiam();
                BigDecimal soLuong = new BigDecimal(item.getSoLuong());
                tongTien = tongTien.add(gia.multiply(soLuong));
                tongSoLuong += item.getSoLuong();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("idChiTiet", item.getId());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("hinhAnh", item.getSanPhamChiTiet().getHinhAnhSanPham());
            map.put("giaBan", priceSnapshot.giaBanSauGiam());
            map.put("giaGoc", priceSnapshot.giaNiemYet());
            map.put("phanTramGiam", priceSnapshot.phanTramGiam());
            map.put("soLuong", item.getSoLuong());
            map.put("idSanPham", sp.getId());
            map.put("mauSac", item.getSanPhamChiTiet().getMauSac());
            map.put("trongLuong", item.getSanPhamChiTiet().getTrongLuong());
            map.put("kichThuoc", item.getSanPhamChiTiet().getKichThuoc());
            danhSachMini.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("trangThai", "ok");
        response.put("tongSoLuong", tongSoLuong);
        response.put("tongTien", tongTien);
        response.put("danhSach", danhSachMini);

        return response;
    }

    @org.springframework.transaction.annotation.Transactional
    public void xoaSanPhamKhoiGio(Integer idGioHangChiTiet, Integer idTaiKhoan) {
        KhachHang khachHang = getOrCreateKhachHang(idTaiKhoan);

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findById(idGioHangChiTiet)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        // Chống IDOR: Xác minh sản phẩm giỏ hàng thuộc về người dùng đang thao tác
        if (!chiTiet.getGioHang().getKhachHang().getId().equals(khachHang.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xóa sản phẩm này khỏi giỏ hàng!");
        }

        gioHangChiTietRepository.delete(chiTiet);
    }

    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> xoaNhieuSanPhamKhoiGio(List<Integer> selectedItemIds, Integer idTaiKhoan) {
        Map<String, Object> response = new HashMap<>();

        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            response.put("trangThai", "ok");
            response.put("deletedCount", 0);
            Map<String, Object> miniCartData = layDuLieuMiniCart(idTaiKhoan);
            response.put("cartItemCount", miniCartData.get("tongSoLuong"));
            response.put("cartTotalQuantity", miniCartData.get("tongSoLuong"));
            response.put("cartTotal", miniCartData.get("tongTien"));
            return response;
        }

        KhachHang khachHang = getOrCreateKhachHang(idTaiKhoan);

        List<Integer> distinctIds = selectedItemIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (distinctIds.isEmpty()) {
            response.put("trangThai", "ok");
            response.put("deletedCount", 0);
            Map<String, Object> miniCartData = layDuLieuMiniCart(idTaiKhoan);
            response.put("cartItemCount", miniCartData.get("tongSoLuong"));
            response.put("cartTotalQuantity", miniCartData.get("tongSoLuong"));
            response.put("cartTotal", miniCartData.get("tongTien"));
            return response;
        }

        // All-or-nothing IDOR validation:
        // Query ALL items belonging to this khachHang for the distinct requested IDs
        List<GioHangChiTiet> ownedItems = gioHangChiTietRepository.findAllByIdInAndGioHang_KhachHang_Id(distinctIds, khachHang.getId());

        // If count of owned items does not equal total distinct requested IDs -> payload contains foreign or invalid ID!
        if (ownedItems.size() != distinctIds.size()) {
            throw new org.springframework.security.access.AccessDeniedException("Một số sản phẩm không tồn tại hoặc không thuộc giỏ hàng của bạn!");
        }

        gioHangChiTietRepository.deleteAll(ownedItems);
        gioHangChiTietRepository.flush();

        response.put("trangThai", "ok");
        response.put("deletedCount", ownedItems.size());

        Map<String, Object> miniCartData = layDuLieuMiniCart(idTaiKhoan);
        response.put("cartItemCount", miniCartData.get("tongSoLuong"));
        response.put("cartTotalQuantity", miniCartData.get("tongSoLuong"));
        response.put("cartTotal", miniCartData.get("tongTien"));

        return response;
    }

    @org.springframework.transaction.annotation.Transactional
    public void capNhatSoLuong(Integer idGioHangChiTiet, Integer soLuongMoi, Integer idTaiKhoan) {
        if (soLuongMoi == null || soLuongMoi <= 0) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ hàng phải lớn hơn 0.");
        }
        if (soLuongMoi > MAX_CART_QUANTITY) {
            throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ hàng không được vượt quá " + MAX_CART_QUANTITY + ".");
        }
        if (idGioHangChiTiet == null) {
            throw new IllegalArgumentException("Chi tiết giỏ hàng không hợp lệ.");
        }

        KhachHang khachHang = getOrCreateKhachHang(idTaiKhoan);

        GioHangChiTiet chiTiet = gioHangChiTietRepository.findById(idGioHangChiTiet)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        // Chống IDOR: Xác minh sản phẩm giỏ hàng thuộc về người dùng đang thao tác
        if (!chiTiet.getGioHang().getKhachHang().getId().equals(khachHang.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền cập nhật giỏ hàng này!");
        }

        if (!productAvailabilityService.isVariantPublished(chiTiet.getSanPhamChiTiet())) {
            throw new RuntimeException("Phân loại sản phẩm này đã ngừng bán!");
        }

        // Chống sửa vượt quá tồn kho (H-7)
        if (chiTiet.getSanPhamChiTiet().getSoLuongTon() < soLuongMoi) {
            throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + chiTiet.getSanPhamChiTiet().getSoLuongTon() + " sản phẩm.");
        }
        chiTiet.setSoLuong(soLuongMoi);
        gioHangChiTietRepository.save(chiTiet);
    }

    @org.springframework.transaction.annotation.Transactional
    public void xoaTatCa(Integer idTaiKhoan) {
        List<GioHangChiTiet> danhSach = layDanhSachSanPhamTrongGio(idTaiKhoan);
        gioHangChiTietRepository.deleteAll(danhSach);
    }

    @org.springframework.transaction.annotation.Transactional
    public void expirePendingOrder(HoaDon order) {
        if (order == null || order.getId() == null) return;
        // The caller's timeout candidate can predate a committed payment.
        order = hoaDonRepository.findByIdWithLock(order.getId()).orElse(null);
        if (order == null) return;
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (order != null && OrderStatus.CHO_THANH_TOAN.getValue().equals(order.getTrangThaiDonHang())) {
            order.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue());
            order.setPaymentStatus("expired");
            order.setTrangThaiThanhToan("HỦY");
            hoaDonRepository.save(order);
            log.info("[GioHangService] Expired pending order #{} (marked as da_huy / expired)", order.getId());
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void cleanPendingOrders(Integer idTaiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh == null) {
            return;
        }
        List<HoaDon> existingOrders = hoaDonRepository.findByKhachHang_Id(kh.getId());
        java.time.LocalDateTime threeMinutesAgo = java.time.LocalDateTime.now().minusMinutes(3);
        for (HoaDon oldOrder : existingOrders) {
            if ("cho_thanh_toan".equals(oldOrder.getTrangThaiDonHang())
                    && "pending".equalsIgnoreCase(oldOrder.getPaymentStatus())) {

                // Expire pending orders created more than 3 minutes ago
                if (oldOrder.getNgayTao() != null && oldOrder.getNgayTao().isBefore(threeMinutesAgo)) {
                    expirePendingOrder(oldOrder);
                }
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void removePurchasedItemsFromCart(Integer idTaiKhoan, List<PurchasedItemSnapshot> purchasedItems) {
        if (idTaiKhoan == null || purchasedItems == null || purchasedItems.isEmpty()) {
            return;
        }
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (khachHang == null) {
            khachHang = khachHangRepository.findById(idTaiKhoan).orElse(null);
        }
        if (khachHang == null) {
            return;
        }
        GioHang gioHang = gioHangRepository.findByKhachHang_Id(khachHang.getId());
        if (gioHang == null) {
            return;
        }

        for (PurchasedItemSnapshot purchased : purchasedItems) {
            if (purchased == null || !purchased.isFromCart()) {
                continue;
            }
            GioHangChiTiet chiTiet = null;
            if (purchased.getCartItemId() != null) {
                chiTiet = gioHangChiTietRepository.findById(purchased.getCartItemId()).orElse(null);
            }
            if (chiTiet == null && purchased.getIdSanPhamChiTiet() != null) {
                chiTiet = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), purchased.getIdSanPhamChiTiet());
            }
            if (chiTiet != null && chiTiet.getGioHang().getId().equals(gioHang.getId())) {
                int currentQty = chiTiet.getSoLuong() != null ? chiTiet.getSoLuong() : 0;
                int purchasedQty = purchased.getSoLuongDaMua() != null ? purchased.getSoLuongDaMua() : 0;
                int newQty = currentQty - purchasedQty;
                if (newQty <= 0) {
                    gioHangChiTietRepository.delete(chiTiet);
                } else {
                    chiTiet.setSoLuong(newQty);
                    gioHangChiTietRepository.save(chiTiet);
                }
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public OrderCreationResult createOrderFromCheckout(
            Integer idTaiKhoan,
            CheckoutContext context,
            String hoTenNhan, String sdtNhan, String diaChiNhan,
            Integer idDonViVanChuyen, String phuongThucThanhToan, String ghiChu,
            Integer ghnToDistrictId, String ghnToWardCode, Integer ghnProvinceId,
            Integer idDiaChiLuu, String voucherCode) {

        String cleanGhiChu = sanitizeInput(ghiChu);
        if (cleanGhiChu != null && cleanGhiChu.length() > 500) {
            log.warn("[SECURITY_ALERT] Invalid note length for user: {}", idTaiKhoan);
            throw new IllegalArgumentException("Ghi chú đơn hàng tối đa 500 ký tự.");
        }

        if (context == null || context.getItems() == null || context.getItems().isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm thanh toán không hợp lệ.");
        }

        KhachHang kh = getOrCreateKhachHang(idTaiKhoan);

        String finalHoTenNhan;
        String finalSdtNhan;
        String finalDiaChiNhan;
        Integer finalDistrictId = ghnToDistrictId;
        String finalWardCode = ghnToWardCode;
        Integer finalProvinceId = ghnProvinceId;

        SoDiaChi finalDiaChi = null;

        if (idDiaChiLuu != null) {
            SoDiaChi soDiaChi = soDiaChiRepository.findById(idDiaChiLuu)
                    .orElseThrow(() -> new IllegalArgumentException("Địa chỉ đã lưu không tồn tại."));
            finalDiaChi = soDiaChi;
            if (!soDiaChi.getKhachHang().getId().equals(kh.getId())) {
                log.warn("[SECURITY_ALERT] Customer ID {} attempted to use unauthorized address ID {}", kh.getId(), idDiaChiLuu);
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền sử dụng địa chỉ này.");
            }

            if (soDiaChi.getProvinceId() == null || soDiaChi.getDistrictId() == null || soDiaChi.getWardCode() == null || soDiaChi.getWardCode().trim().isEmpty()) {
                if (ghnProvinceId != null && ghnToDistrictId != null && ghnToWardCode != null && !ghnToWardCode.trim().isEmpty()) {
                    soDiaChi.setProvinceId(ghnProvinceId);
                    soDiaChi.setDistrictId(ghnToDistrictId);
                    soDiaChi.setWardCode(ghnToWardCode.trim());
                    soDiaChiRepository.save(soDiaChi);
                    log.info("Auto-healed saved address ID {} with GHN IDs: provinceId={}, districtId={}, wardCode={}",
                            soDiaChi.getId(), ghnProvinceId, ghnToDistrictId, ghnToWardCode);
                }
            }

            GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddress(soDiaChi);
            if (mapping == null || mapping.getDistrictId() == null || mapping.getWardCode() == null) {
                throw new IllegalArgumentException("Địa chỉ đã lưu của bạn chưa được chuẩn hóa địa chỉ GHN. Vui lòng cập nhật sổ địa chỉ hoặc chọn \"Nhập địa chỉ mới\".");
            }

            finalHoTenNhan = soDiaChi.getHoNguoiNhan() + " " + soDiaChi.getTenNguoiNhan();
            finalSdtNhan = soDiaChi.getSdtNguoiNhan();
            String fullAddress = soDiaChi.getDiaChiCuThe();
            if (soDiaChi.getWardName() != null && !soDiaChi.getWardName().trim().isEmpty() && !fullAddress.contains(soDiaChi.getWardName())) {
                fullAddress += ", " + soDiaChi.getWardName().trim();
            }
            if (soDiaChi.getThanhPho() != null && !soDiaChi.getThanhPho().trim().isEmpty() && !soDiaChi.getThanhPho().equalsIgnoreCase(soDiaChi.getTinhThanh()) && !fullAddress.contains(soDiaChi.getThanhPho())) {
                fullAddress += ", " + soDiaChi.getThanhPho().trim();
            }
            if (soDiaChi.getTinhThanh() != null && !soDiaChi.getTinhThanh().trim().isEmpty() && !fullAddress.contains(soDiaChi.getTinhThanh())) {
                fullAddress += ", " + soDiaChi.getTinhThanh().trim();
            }
            if (soDiaChi.getQuocGia() != null && !soDiaChi.getQuocGia().trim().isEmpty() && !"Việt Nam".equalsIgnoreCase(soDiaChi.getQuocGia().trim()) && !fullAddress.contains(soDiaChi.getQuocGia())) {
                fullAddress += ", " + soDiaChi.getQuocGia().trim();
            }

            finalDiaChiNhan = fullAddress;
            finalDistrictId = mapping.getDistrictId();
            finalWardCode = mapping.getWardCode();
            finalProvinceId = mapping.getProvinceId();
        } else {
            finalHoTenNhan = sanitizeInput(hoTenNhan);
            finalSdtNhan = sanitizeInput(sdtNhan);
            finalDiaChiNhan = sanitizeInput(diaChiNhan);

            if (finalHoTenNhan == null || finalHoTenNhan.isEmpty()) {
                throw new IllegalArgumentException("Họ và tên người nhận không được để trống.");
            }
            if (finalHoTenNhan.length() < 2 || finalHoTenNhan.length() > 100) {
                throw new IllegalArgumentException("Họ và tên người nhận phải từ 2 đến 100 ký tự.");
            }

            finalSdtNhan = PhoneUtils.normalize(finalSdtNhan);
            if (finalSdtNhan == null || finalSdtNhan.isEmpty()) {
                throw new IllegalArgumentException("Số điện thoại không được để trống.");
            }
            if (!PhoneUtils.isValid(finalSdtNhan)) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
            }

            if (finalDiaChiNhan == null || finalDiaChiNhan.isEmpty()) {
                throw new IllegalArgumentException("Địa chỉ nhận hàng không được để trống.");
            }
            if (finalDiaChiNhan.length() < 5 || finalDiaChiNhan.length() > 255) {
                throw new IllegalArgumentException("Địa chỉ nhận hàng phải từ 5 đến 255 ký tự.");
            }
        }

        cleanPendingOrders(idTaiKhoan);

        BigDecimal tamTinh = BigDecimal.ZERO;
        List<PurchasedItemSnapshot> purchasedSnapshots = new ArrayList<>();
        List<Map<String, Object>> validatedItems = new ArrayList<>();

        for (CheckoutItemContext itemCtx : context.getItems()) {
            Integer spctId = itemCtx.getIdSanPhamChiTiet();
            if (spctId == null && itemCtx.getCartItemId() != null && idTaiKhoan != null) {
                GioHangChiTiet ghct = gioHangChiTietRepository.findById(itemCtx.getCartItemId()).orElse(null);
                if (ghct != null) {
                    spctId = ghct.getSanPhamChiTiet().getId();
                }
            }
            if (spctId == null) {
                throw new IllegalArgumentException("Sản phẩm thanh toán không hợp lệ.");
            }

            SanPhamChiTiet lockedSpct = sanPhamChiTietRepository.findByIdWithLock(spctId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            SanPham sp = lockedSpct.getSanPham();
            int tonKho = lockedSpct.getSoLuongTon();
            int buyQty = itemCtx.getSoLuong() != null ? itemCtx.getSoLuong() : 0;

            boolean hopLe = productAvailabilityService.isVariantPurchasable(lockedSpct, buyQty);
            if (!hopLe) {
                throw new RuntimeException("Sản phẩm '" + sp.getTenSanPham() + "' không đủ hàng tồn kho hoặc phân loại đã ngưng kinh doanh!");
            }

            BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(lockedSpct);
            tamTinh = tamTinh.add(giaBanSauGiam.multiply(new BigDecimal(buyQty)));

            Map<String, Object> map = new HashMap<>();
            map.put("spct", lockedSpct);
            map.put("soLuong", buyQty);
            validatedItems.add(map);

            purchasedSnapshots.add(PurchasedItemSnapshot.builder()
                    .cartItemId(itemCtx.getCartItemId())
                    .idSanPhamChiTiet(lockedSpct.getId())
                    .soLuongDaMua(buyQty)
                    .fromCart(itemCtx.isFromCart())
                    .build());
        }

        DonViVanChuyen dvvc = null;
        if (idDonViVanChuyen != null) {
            dvvc = donViVanChuyenDAO.findById(idDonViVanChuyen).orElse(null);
        }
        if (dvvc == null && idDonViVanChuyen != null) {
            dvvc = adminShippingService.getAllCarriers().stream()
                    .filter(c -> c.getId().equals(idDonViVanChuyen))
                    .findFirst()
                    .orElse(null);
        }
        if (dvvc == null || DonViVanChuyen.isCounterCarrier(dvvc)) {
            dvvc = donViVanChuyenDAO.findAll().stream()
                    .filter(c -> DonViVanChuyen.isGhnCarrier(c))
                    .findFirst()
                    .orElse(null);
        }
        if (dvvc == null) {
            dvvc = donViVanChuyenDAO.findAll().stream()
                    .filter(c -> !DonViVanChuyen.isCounterCarrier(c))
                    .findFirst()
                    .orElse(null);
        }
        if (dvvc == null) {
            DonViVanChuyen defaultGhn = new DonViVanChuyen();
            defaultGhn.setMaDonVi("GHN");
            defaultGhn.setTenDonVi("Giao Hàng Nhanh (GHN)");
            defaultGhn.setHotline("1900 636677");
            defaultGhn.setWebsite("https://ghn.vn");
            defaultGhn.setPhiLocal(new BigDecimal("25000"));
            defaultGhn.setPhiNationwide(new BigDecimal("38000"));
            dvvc = donViVanChuyenDAO.save(defaultGhn);
        }

        boolean isGhn = DonViVanChuyen.isGhnCarrier(dvvc);
        if (isGhn) {
            if (finalDistrictId == null || finalWardCode == null || finalWardCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã (GHN) để sử dụng đơn vị vận chuyển Giao Hàng Nhanh.");
            }
        }

        BigDecimal phiShip = shippingFeeCalculator.calculateFee(dvvc, finalDistrictId, finalWardCode, finalDiaChiNhan);

        BigDecimal giamGia = BigDecimal.ZERO;
        PhieuGiamGia appliedVoucher = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            PhieuGiamGia voucher = phieuGiamGiaRepository.findByMaPhieuWithLock(voucherCode.trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));
            if (!voucher.getActive()) {
                throw new IllegalArgumentException("Mã giảm giá không còn hoạt động.");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(voucher.getNgayBatDau())) {
                throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian áp dụng.");
            }
            if (now.isAfter(voucher.getNgayKetThuc())) {
                throw new IllegalArgumentException("Mã giảm giá đã hết hạn sử dụng.");
            }
            if (voucher.getSoLuongConLai() <= 0) {
                throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
            }
            if (tamTinh.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá này.");
            }

            giamGia = VoucherCalculator.calculateVoucherDiscount(tamTinh, voucher);

            if ("COD".equalsIgnoreCase(phuongThucThanhToan)) {
                voucher.setSoLuongConLai(voucher.getSoLuongConLai() - 1);
                phieuGiamGiaRepository.save(voucher);
            }
            appliedVoucher = voucher;
        }

        BigDecimal totalAmount = tamTinh.subtract(giamGia).add(phiShip);

        final String targetCode = "COD".equalsIgnoreCase(phuongThucThanhToan) ? "COD" : "SEPAY";
        List<PhuongThucThanhToan> allPttt = phuongThucThanhToanDAO.findAll();
        PhuongThucThanhToan pttt = allPttt.stream()
                .filter(p -> (p.getMaPhuongThuc() != null && targetCode.equalsIgnoreCase(p.getMaPhuongThuc()))
                || ("SEPAY".equalsIgnoreCase(targetCode) && (p.getId() != null && p.getId() == 1))
                || ("COD".equalsIgnoreCase(targetCode) && (p.getId() != null && p.getId() == 2)))
                .findFirst()
                .orElseGet(() -> {
                    Integer targetId = "SEPAY".equalsIgnoreCase(targetCode) ? 1 : 2;
                    return phuongThucThanhToanDAO.findById(targetId).orElseGet(() -> {
                        PhuongThucThanhToan newP = new PhuongThucThanhToan();
                        newP.setMaPhuongThuc(targetCode);
                        newP.setTenPhuongThuc("SEPAY".equalsIgnoreCase(targetCode) ? "Chuyển khoản Online" : "Thanh toán khi nhận hàng");
                        return phuongThucThanhToanDAO.save(newP);
                    });
                });

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(dvvc);
        hd.setDiaChi(finalDiaChi);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(totalAmount);
        hd.setPhiVanChuyen(phiShip);
        hd.setPhieuGiamGia(appliedVoucher);
        hd.setSoTienGiamVoucher(giamGia);

        if (appliedVoucher != null) {
            hd.setMaVoucherApDung(appliedVoucher.getMaPhieu());
            hd.setTenVoucherApDung("Voucher " + appliedVoucher.getMaPhieu());
            String limitDesc = appliedVoucher.getGiaTriGiamToiDa() != null ? " (Giảm tối đa " + appliedVoucher.getGiaTriGiamToiDa() + "đ)" : "";
            hd.setMoTaVoucherSnapshot("Giảm " + appliedVoucher.getGiaTri() + ("%".equals(appliedVoucher.getDonVi()) ? "%" : "đ") + limitDesc + " cho đơn hàng từ " + appliedVoucher.getGiaTriDonHangToiThieu() + "đ");
        } else {
            hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        }

        if ("COD".equalsIgnoreCase(phuongThucThanhToan)) {
            hd.setTrangThaiDonHang(OrderStatus.CHO_XAC_NHAN.getValue());
            hd.setPaymentMethod(PaymentMethod.COD.getValue());
        } else {
            hd.setTrangThaiDonHang(OrderStatus.CHO_THANH_TOAN.getValue());
            hd.setPaymentMethod(PaymentMethod.SEPAY.getValue());
        }

        hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        hd.setDiaChiNhan(finalDiaChiNhan);
        hd.setSdtNhan(finalSdtNhan);

        if (kh != null && kh.getTaiKhoan() != null && kh.getTaiKhoan().getUsername() != null) {
            String emailSnapshot = kh.getTaiKhoan().getUsername().trim();
            if (!emailSnapshot.isEmpty() && emailSnapshot.contains("@")) {
                hd.setEmailNguoiNhan(emailSnapshot);
            }
        }

        String resolvedTenNguoiNhan = "Quý khách";
        if (finalHoTenNhan != null && !finalHoTenNhan.trim().isEmpty()) {
            resolvedTenNguoiNhan = finalHoTenNhan.trim();
        }
        hd.setTenNguoiNhan(resolvedTenNguoiNhan);

        hd.setGhiChu(cleanGhiChu);
        hd.setPaymentStatus(PaymentStatus.PENDING.getValue());

        if (finalDistrictId != null) {
            hd.setGhnToDistrictId(finalDistrictId);
        }
        if (finalWardCode != null && !finalWardCode.isBlank()) {
            hd.setGhnToWardCode(finalWardCode);
        }

        hd = hoaDonRepository.save(hd);

        for (Map<String, Object> itemMap : validatedItems) {
            SanPhamChiTiet lockedSpct = (SanPhamChiTiet) itemMap.get("spct");
            Integer qty = (Integer) itemMap.get("soLuong");

            com.smashvn.shop.service.product.PriceSnapshot priceSnapshot = pricingService.buildPriceSnapshot(lockedSpct);

            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setHoaDon(hd);
            hdct.setSanPhamChiTiet(lockedSpct);
            hdct.setSoLuong(qty);
            hdct.setDonGia(priceSnapshot.giaBanSauGiam());
            hdct.setGiaGoc(priceSnapshot.giaNiemYet());
            hdct.setGiaSauGiam(priceSnapshot.giaBanSauGiam());
            hdct.setTenSanPhamSnapshot(lockedSpct.getSanPham().getTenSanPham());

            String sku = lockedSpct.getSku();
            if (sku == null || sku.isBlank()) {
                String maSp = (lockedSpct.getSanPham() != null && lockedSpct.getSanPham().getMaSanPham() != null)
                        ? lockedSpct.getSanPham().getMaSanPham()
                        : com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateProductCode(lockedSpct.getSanPham().getId());
                sku = com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateVariantSku(maSp, lockedSpct.getId());
            }
            hdct.setSkuSnapshot(sku);
            hdct.setTenDotGiamGiaSnapshot(priceSnapshot.tenDotGiamGia());
            hdct.setThuocTinhSnapshot(lockedSpct.getPhanLoaiHienThi());

            hoaDonChiTietRepository.save(hdct);
        }

        return OrderCreationResult.builder()
                .hoaDon(hd)
                .purchasedItems(purchasedSnapshots)
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public OrderCreationResult submitCodOrder(
            Integer idTaiKhoan,
            CheckoutContext context,
            HttpSession session,
            String hoTenNhan, String sdtNhan, String diaChiNhan,
            Integer idDonViVanChuyen, String ghiChu,
            Integer ghnToDistrictId, String ghnToWardCode, Integer ghnProvinceId,
            Integer idDiaChiLuu, String voucherCode) {

        OrderCreationResult result = createOrderFromCheckout(
                idTaiKhoan, context, hoTenNhan, sdtNhan, diaChiNhan,
                idDonViVanChuyen, "COD", ghiChu,
                ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                idDiaChiLuu, voucherCode);

        HoaDon hd = result.getHoaDon();

        if (context.getSource() == CheckoutSource.CART || context.getSource() == CheckoutSource.QUICK_ADD) {
            if (idTaiKhoan != null) {
                removePurchasedItemsFromCart(idTaiKhoan, result.getPurchasedItems());
            }
            if (session != null) {
                guestCartService.removePurchasedItemsFromGuestCart(session, result.getPurchasedItems());
            }
        }

        try {
            if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                ThongBao thongBaoOrder = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe("Đặt hàng thành công")
                        .noiDung("Đơn hàng #" + hd.getMaDonHang() + " của bạn đã được hệ thống ghi nhận thành công. Cảm ơn bạn đã mua sắm tại Smash VN!")
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBaoOrder);
            }
        } catch (Exception e) {
            log.error("[GioHangService] Failed to save order notification: {}", e.getMessage());
        }

        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public OrderCreationResult createSepayPendingOrder(
            Integer idTaiKhoan,
            CheckoutContext context,
            HttpSession session,
            String hoTenNhan, String sdtNhan, String diaChiNhan,
            Integer idDonViVanChuyen, String ghiChu,
            Integer ghnToDistrictId, String ghnToWardCode, Integer ghnProvinceId,
            Integer idDiaChiLuu, String voucherCode) {

        OrderCreationResult result = createOrderFromCheckout(
                idTaiKhoan, context, hoTenNhan, sdtNhan, diaChiNhan,
                idDonViVanChuyen, "SePay", ghiChu,
                ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                idDiaChiLuu, voucherCode);

        if (context.getSource() == CheckoutSource.CART || context.getSource() == CheckoutSource.QUICK_ADD) {
            if (idTaiKhoan != null) {
                removePurchasedItemsFromCart(idTaiKhoan, result.getPurchasedItems());
            }
            if (session != null) {
                guestCartService.removePurchasedItemsFromGuestCart(session, result.getPurchasedItems());
            }
        }

        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public OrderCreationResult createSepayPendingOrder(
            Integer idTaiKhoan,
            CheckoutContext context,
            String hoTenNhan, String sdtNhan, String diaChiNhan,
            Integer idDonViVanChuyen, String ghiChu,
            Integer ghnToDistrictId, String ghnToWardCode, Integer ghnProvinceId,
            Integer idDiaChiLuu, String voucherCode) {

        return createSepayPendingOrder(
                idTaiKhoan, context, null,
                hoTenNhan, sdtNhan, diaChiNhan,
                idDonViVanChuyen, ghiChu,
                ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                idDiaChiLuu, voucherCode);
    }

    @org.springframework.transaction.annotation.Transactional
    public HoaDon createOrder(Integer idTaiKhoan, String hoTenNhan, String sdtNhan, String diaChiNhan,
            Integer idDonViVanChuyen, String phuongThucThanhToan, String ghiChu,
            Integer ghnToDistrictId, String ghnToWardCode, Integer ghnProvinceId,
            Integer idDiaChiLuu, String voucherCode) {

        List<GioHangChiTiet> cartItems = layDanhSachSanPhamTrongGio(idTaiKhoan);
        List<CheckoutItemContext> itemContexts = new ArrayList<>();
        for (GioHangChiTiet item : cartItems) {
            itemContexts.add(CheckoutItemContext.builder()
                    .cartItemId(item.getId())
                    .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                    .soLuong(item.getSoLuong())
                    .fromCart(true)
                    .build());
        }
        CheckoutContext context = CheckoutContext.builder()
                .source(CheckoutSource.CART)
                .items(itemContexts)
                .build();

        OrderCreationResult result = createOrderFromCheckout(idTaiKhoan, context, hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyen, phuongThucThanhToan, ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId, idDiaChiLuu, voucherCode);
        if ("COD".equalsIgnoreCase(phuongThucThanhToan)) {
            removePurchasedItemsFromCart(idTaiKhoan, result.getPurchasedItems());
        }
        return result.getHoaDon();
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return Jsoup.clean(input, Safelist.none()).trim();
    }
}
