package com.smashvn.shop.controller.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.dto.inventory.AllocationResult;
import com.smashvn.shop.dto.inventory.AllocationStatus;
import com.smashvn.shop.dto.inventory.OrderItemRequest;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.exception.GhnCreateIndeterminateException;
import com.smashvn.shop.exception.GhnUnsupportedRouteException;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.api.GhnStatusMapper;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.service.order.OrderViewService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller nội bộ cung cấp các API GHN cho frontend: - GET
 * /api/ghn/districts?provinceId= → danh sách quận/huyện - GET
 * /api/ghn/wards?districtId= → danh sách phường/xã - POST /api/ghn/fee → tính
 * phí ship - GET /api/ghn/track/{orderCode} → tra cứu trạng thái vận đơn
 */
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@Slf4j
public class GhnRestController {

    private final GhnService ghnService;
    private final GhnStatusMapper ghnStatusMapper;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final OrderViewService orderViewService;
    private final GhnConfig ghnConfig;
    private final TaiKhoanRepository taiKhoanRepository;
    private final InventoryLotService inventoryLotService;
    private final SoDiaChiRepository soDiaChiRepository;

    /**
     * Lấy danh sách tỉnh/thành phố
     */
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        try {
            List<Map<String, Object>> provinces = ghnService.getProvincesOrThrow();
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("data", provinces);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Unable to load delivery provinces: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error",
                    "message", "Không thể tải danh sách Tỉnh/Thành phố. Vui lòng thử lại."));
        }
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        try {
            List<Map<String, Object>> districts = ghnService.getDistrictsOrThrow(provinceId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("data", districts);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Unable to load delivery districts for province {}: {}", provinceId, e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error",
                    "message", "Không thể tải danh sách Quận/Huyện. Vui lòng thử lại."));
        }
    }

    /**
     * Lấy danh sách phường/xã theo quận/huyện
     */
    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        try {
            List<Map<String, Object>> wards = ghnService.getWardsOrThrow(districtId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("data", wards);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Unable to load delivery wards for district {}: {}", districtId, e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error",
                    "message", "Không thể tải danh sách Phường/Xã. Vui lòng thử lại."));
        }
    }

    /**
     * Tính phí ship
     */
    @PostMapping("/fee")
    public ResponseEntity<?> calculateFee(@RequestBody Map<String, Object> body) {
        try {
            Integer toDistrictId = (Integer) body.get("toDistrictId");
            String toWardCode = (String) body.get("toWardCode");
            Integer insuranceValue = body.get("insuranceValue") != null
                    ? ((Number) body.get("insuranceValue")).intValue() : 0;

            if (toDistrictId == null || toWardCode == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Thiếu toDistrictId hoặc toWardCode"));
            }

            BigDecimal fee = ghnService.calculateShipFee(toDistrictId, toWardCode, insuranceValue);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("fee", fee);
            result.put("feeFormatted", String.format("%,.0f", fee) + " đ");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GHN fee calculation error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage(),
                    "fee", 30000, "feeFormatted", "30,000 đ"));
        }
    }

    /**
     * Tra cứu trạng thái vận đơn GHN
     */
    @GetMapping("/track/{orderCode}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Chưa đăng nhập"));
        }
        try {
            Map<String, Object> trackingData = ghnService.trackOrder(orderCode);
            if (trackingData == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không tìm thấy thông tin vận đơn"));
            }
            return ResponseEntity.ok(Map.of("status", "ok", "data", trackingData));
        } catch (Exception e) {
            log.error("GHN track error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Tra cứu vận đơn theo ID đơn hàng (dành cho user)
     */
    @GetMapping("/track/order/{orderId}")
    public ResponseEntity<?> trackByOrderId(@PathVariable Integer orderId, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Chua dang nhap"));
        }

        var taiKhoan = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        if (taiKhoan == null
                || taiKhoan.getTrangThaiTaiKhoan() != AccountStatus.ACTIVE
                || !"KH".equals(taiKhoan.getVaiTro())
                || !"hoat_dong".equalsIgnoreCase(taiKhoan.getTrangThai())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Chua dang nhap"));
        }

        HoaDon requestedOrder = hoaDonRepository.findById(orderId).orElse(null);
        if (requestedOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Khong tim thay don hang"));
        }

        Integer ownerAccountId = requestedOrder.getKhachHang() != null && requestedOrder.getKhachHang().getTaiKhoan() != null
                ? requestedOrder.getKhachHang().getTaiKhoan().getId()
                : null;
        if (!idNguoiDung.equals(ownerAccountId)) {
            log.warn("GHN trackByOrderId ownership denied: user #{} tried order #{}", idNguoiDung, orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Khong co quyen truy cap"));
        }
        try {
            String ghnCode = requestedOrder.getGhnOrderCode();
            if (ghnCode == null || ghnCode.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "no_ghn", "message", "Đơn hàng này chưa có mã vận đơn GHN",
                        "trangThaiDonHang", requestedOrder.getTrangThaiDonHang()));
            }

            if (ghnCode.startsWith("DEMO-GHN-")) {
                Map<String, Object> fallbackData = new HashMap<>();
                fallbackData.put("order_code", ghnCode);
                fallbackData.put("status", requestedOrder.getGhnStatus() != null ? requestedOrder.getGhnStatus() : "ready_to_pick");
                fallbackData.put("log", java.util.List.of());
                return ResponseEntity.ok(Map.of(
                        "status", "ok",
                        "isDemoFallback", true,
                        "ghnOrderCode", ghnCode,
                        "data", fallbackData,
                        "trangThaiDonHang", requestedOrder.getTrangThaiDonHang()
                ));
            }

            Map<String, Object> trackingData = ghnService.trackOrder(ghnCode);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ok");
            result.put("ghnOrderCode", ghnCode);
            result.put("data", trackingData);
            result.put("trangThaiDonHang", requestedOrder.getTrangThaiDonHang());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("GHN trackByOrderId error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * [ADMIN] Push thủ công đơn hàng lên GHN POST
     * /api/ghn/admin/push/{orderId}?toDistrictId=X&toWardCode=Y
     *
     * Dùng để tạo đơn vận chuyển GHN cho đơn hàng cũ (chưa có ghn_order_code)
     * hoặc khi cần retry sau lỗi.
     */
    @PostMapping("/admin/push/{orderId}")
    public ResponseEntity<?> adminPushToGhn(
            @PathVariable Integer orderId,
            @RequestParam(required = false) Integer toDistrictId,
            @RequestParam(required = false) String toWardCode,
            @RequestParam(required = false, defaultValue = "false") boolean forceRetry,
            HttpSession session) {

        // Chỉ admin/nhân viên mới được dùng
        String role = (String) session.getAttribute("vaiTro");
        if (role == null || (!role.equals("QL") && !role.equals("NV"))) {
            return ResponseEntity.status(403)
                    .body(Map.of("status", "error", "message", "Không có quyền truy cập"));
        }

        try {
            HoaDon hd = hoaDonRepository.findById(orderId).orElse(null);
            if (hd == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không tìm thấy đơn hàng ID=" + orderId));
            }

            if (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().isBlank()) {
                boolean isDemoFallback = hd.getGhnOrderCode().startsWith("DEMO-GHN-");
                return ResponseEntity.ok(Map.of(
                        "status", "already_exists",
                        "message", isDemoFallback
                                ? "Đơn này đã có vận đơn Demo Fallback: " + hd.getGhnOrderCode()
                                : "Đơn này đã có mã GHN: " + hd.getGhnOrderCode(),
                        "ghnOrderCode", hd.getGhnOrderCode(),
                        "isDemoFallback", isDemoFallback
                ));
            }

            // Lấy chi tiết đơn hàng
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(orderId);

            if (items.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Đơn hàng không có sản phẩm"));
            }

            // Nếu không truyền từ RequestParam, tìm thông tin từ entity / sổ địa chỉ / phân giải địa chỉ
            Integer finalDistrictId = toDistrictId != null ? toDistrictId : hd.getGhnToDistrictId();
            String finalWardCode = toWardCode != null && !toWardCode.isBlank() ? toWardCode : hd.getGhnToWardCode();

            if (finalDistrictId == null || finalWardCode == null || finalWardCode.isBlank()) {
                if (hd.getDiaChi() != null) {
                    SoDiaChi dc = null;
                    if (hd.getDiaChi().getId() != null) {
                        dc = soDiaChiRepository.findById(hd.getDiaChi().getId()).orElse(null);
                    }
                    if (dc != null) {
                        if (dc.getDistrictId() != null && dc.getWardCode() != null && !dc.getWardCode().isBlank()) {
                            finalDistrictId = dc.getDistrictId();
                            finalWardCode = dc.getWardCode();
                        } else {
                            GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddressOrThrow(dc);
                            if (mapping != null && mapping.getDistrictId() != null && mapping.getWardCode() != null) {
                                finalDistrictId = mapping.getDistrictId();
                                finalWardCode = mapping.getWardCode();
                            }
                        }
                    }
                }
            }

            if (finalDistrictId == null || finalWardCode == null || finalWardCode.isBlank()) {
                if (hd.getDiaChiNhan() != null && !hd.getDiaChiNhan().isBlank()) {
                    SoDiaChi tempDc = new SoDiaChi();
                    tempDc.setDiaChiCuThe(hd.getDiaChiNhan());
                    String[] parts = hd.getDiaChiNhan().split(",");
                    if (parts.length > 0) {
                        tempDc.setTinhThanh(parts[parts.length - 1].trim());
                    }
                    GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddressOrThrow(tempDc);
                    if (mapping != null && mapping.getDistrictId() != null && mapping.getWardCode() != null) {
                        finalDistrictId = mapping.getDistrictId();
                        finalWardCode = mapping.getWardCode();
                    }
                }
            }

            boolean useSandboxAddressFallback = (finalDistrictId == null || finalWardCode == null || finalWardCode.isBlank())
                    && ghnService.isSandboxEnvironment();

            if (!useSandboxAddressFallback && (finalDistrictId == null || finalWardCode == null || finalWardCode.isBlank())) {
                return ResponseEntity.ok(Map.of(
                        "status", "error",
                        "message", "Đơn hàng thiếu mã Quận/Huyện hoặc Phường/Xã để đẩy lên GHN. Hãy đảm bảo khách hàng có địa chỉ hợp lệ."
                ));
            }

            if (!useSandboxAddressFallback) {
                hd.setGhnToDistrictId(finalDistrictId);
                hd.setGhnToWardCode(finalWardCode);
            }

            // Đảm bảo hàng tồn kho đã được trừ FIFO an toàn nếu nhân viên bấm gửi GHN trực tiếp
            boolean isStockDeducted = orderViewService.isStockDeductedState(hd, hd.getTrangThaiDonHang());
            if (!isStockDeducted) {
                List<OrderItemRequest> reqs = new ArrayList<>();
                for (HoaDonChiTiet item : items) {
                    if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                        reqs.add(OrderItemRequest.builder()
                                .representativeSpctId(item.getSanPhamChiTiet().getId())
                                .quantity(item.getSoLuong())
                                .build());
                    }
                }
                if (!reqs.isEmpty()) {
                    AllocationResult allocResult = inventoryLotService.allocateFifo(reqs);
                    if (allocResult != null && allocResult.status() == AllocationStatus.INSUFFICIENT_STOCK) {
                        return ResponseEntity.ok(Map.of(
                                "status", "error",
                                "message", allocResult.message() != null ? allocResult.message() : "Sản phẩm không đủ hàng tồn kho để gửi sang GHN!"
                        ));
                    }
                }
            }

            String ghnCode;
            if (useSandboxAddressFallback) {
                if (ghnService.hasUnknownGhnCreateStatus(orderId)) {
                    throw new GhnCreateIndeterminateException(
                            "Đơn hàng từng có kết quả tạo vận đơn GHN chưa xác định. Không thể chuyển sang Demo Fallback cho đến khi đã đối soát trên GHN.");
                }
                log.warn("[ADMIN_GHN_SANDBOX] Order ID {} has unresolvable address '{}'. Creating internal Demo Fallback without sending fake district/ward data to GHN.",
                        orderId, hd.getDiaChiNhan());
                ghnCode = ghnService.createFallbackShippingOrder(
                        hd,
                        new GhnUnsupportedRouteException("GHN Sandbox không thể phân giải địa chỉ nhận của khách hàng."));
            } else {
                ghnCode = ghnService.createShippingOrderOrThrow(hd, items, finalDistrictId, finalWardCode, forceRetry);
            }
            if (ghnCode != null && !ghnCode.isBlank()) {
                boolean isDemoFallback = ghnCode.startsWith("DEMO-GHN-");
                hd.setGhnOrderCode(ghnCode);
                hd.setGhnStatus("ready_to_pick");
                String currentStatus = hd.getTrangThaiDonHang();
                if (currentStatus != null && ("san_sang_giao".equalsIgnoreCase(currentStatus)
                        || "dang_chuan_bi_hang".equalsIgnoreCase(currentStatus)
                        || "da_xac_nhan".equalsIgnoreCase(currentStatus)
                        || "cho_xac_nhan".equalsIgnoreCase(currentStatus))) {
                    hd.setTrangThaiDonHang(OrderStatus.DA_TAO_VAN_DON_GHN.getValue());
                }
                hoaDonRepository.save(hd);
                log.info("[ADMIN] Đã tạo {} cho đơn #{}, mã: {}",
                        isDemoFallback ? "vận đơn Demo GHN Fallback" : "vận đơn GHN", orderId, ghnCode);
                return ResponseEntity.ok(Map.of(
                        "status", "ok",
                        "message", isDemoFallback
                                ? "GHN Sandbox không hỗ trợ đầy đủ địa chỉ/tuyến hoặc dữ liệu kho thử nghiệm. Hệ thống đã tạo vận đơn Demo Fallback để mô phỏng."
                                : "Tạo đơn GHN thành công!",
                        "ghnOrderCode", ghnCode,
                        "orderId", orderId,
                        "isDemoFallback", isDemoFallback,
                        "shipmentProvider", isDemoFallback ? "GHN_FALLBACK" : "GHN"
                ));
            } else {
                return ResponseEntity.ok(Map.of("status", "error", "message", "GHN không trả về mã vận đơn"));
            }
        } catch (GhnCreateIndeterminateException e) {
            log.error("[ADMIN_GHN_CREATE_RESULT_UNKNOWN] Push GHN indeterminate orderId={}: {}", orderId, e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "unknown_result",
                    "ghnCreateUnknown", true,
                    "requireForceRetry", true,
                    "message", "Kết quả tạo vận đơn GHN chưa xác định. Vui lòng kiểm tra trên GHN trước khi tạo lại để tránh tạo trùng vận đơn."
            ));
        } catch (GhnUnsupportedRouteException e) {
            log.error("[ADMIN_GHN_UNSUPPORTED_ROUTE] Push GHN unsupported route orderId={}: {}", orderId, e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "GHN chưa hỗ trợ tuyến giao hàng cho địa chỉ này: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("[ADMIN] Push GHN error orderId={}: {}", orderId, e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
            String detailedMsg = errorMsg;

            String baseUrl = ghnConfig.getBaseUrl();
            boolean isSandbox = baseUrl != null && (baseUrl.contains("dev.ghn.vn") || baseUrl.contains("5sao"));
            if (isSandbox) {
                detailedMsg += "\n\n[LƯU Ý MÔI TRƯỜNG SANDBOX GHN]:\n"
                        + "1. Hãy đảm bảo API Token (GHN_TOKEN) và Shop ID (GHN_SHOP_ID) cấu hình trong file .env được tạo trên hệ thống thử nghiệm Sandbox (https://5sao.dev.ghn.vn) (Không dùng token thật từ app.ghn.vn).\n"
                        + "2. Mã Quận/Huyện (ghnToDistrictId) và Phường/Xã (ghnToWardCode) của địa chỉ nhận phải khớp chính xác và đang hoạt động trên cơ sở dữ liệu Sandbox của GHN.\n"
                        + "3. Cửa hàng gửi phải được thiết lập địa chỉ kho và được hỗ trợ gói dịch vụ giao hàng chuẩn trên Sandbox.";
            }
            return ResponseEntity.ok(Map.of("status", "error", "message", detailedMsg));
        }
    }

    /**
     * [ADMIN] Đồng bộ thủ công trạng thái đơn hàng từ GHN API về hệ thống nội
     * bộ POST /api/ghn/admin/sync/{orderId}
     */
    @PostMapping("/admin/sync/{orderId}")
    public ResponseEntity<?> adminSyncGhnStatus(
            @PathVariable Integer orderId,
            @RequestParam(value = "provider", required = false) String provider,
            HttpSession session) {
        String role = (String) session.getAttribute("vaiTro");
        if (role == null || (!role.equals("QL") && !role.equals("NV"))) {
            return ResponseEntity.status(403)
                    .body(Map.of("status", "error", "message", "Không có quyền truy cập"));
        }

        try {
            HoaDon hd = hoaDonRepository.findById(orderId).orElse(null);
            if (hd == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không tìm thấy đơn hàng ID=" + orderId));
            }

            boolean syncReturn = "GHN_RETURN".equalsIgnoreCase(provider);
            boolean syncOutbound = "GHN".equalsIgnoreCase(provider);

            if (!syncOutbound) {
                String returnCode = orderViewService.resolveGhnReturnOrderCode(hd.getId(), hd);
                if (returnCode != null && !returnCode.isBlank()) {
                    if (returnCode.startsWith("DEMO-GHN-RETURN-")) {
                        String currentGhnStatus = hd.getGhnStatus() != null ? hd.getGhnStatus() : "ready_to_pick";
                        String ghnStatusLabel = ghnStatusMapper.getGhnStatusLabel(currentGhnStatus);
                        return ResponseEntity.ok(Map.of(
                                "status", "ok",
                                "message", "Mã vận đơn Demo Return Fallback do SMASH-VN tự quản lý (" + returnCode + ")",
                                "ghnStatus", currentGhnStatus,
                                "ghnStatusLabel", ghnStatusLabel,
                                "internalStatus", hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "WAITING_FOR_PICKUP"
                        ));
                    }
                    Map<String, Object> trackingData = ghnService.trackOrder(returnCode);
                    if (trackingData != null && trackingData.get("status") != null) {
                        String ghnStatus = (String) trackingData.get("status");
                        com.smashvn.shop.entity.ReturnStatus newReturnStatus = ghnStatusMapper.mapToReturnStatus(ghnStatus);
                        if (newReturnStatus != null) {
                            orderViewService.updateReturnStatusFromGhn(hd.getId(), newReturnStatus, ghnStatus, "ADMIN_SYNC");
                            log.info("[ADMIN] Đã đồng bộ thủ công ĐƠN HOÀN TRẢ #{}: GHN returnStatus={}, internalReturnStatus={}", orderId, ghnStatus, newReturnStatus.name());
                            String ghnStatusLabel = ghnStatusMapper.getGhnStatusLabel(ghnStatus);
                            return ResponseEntity.ok(Map.of(
                                    "status", "ok",
                                    "message", "Đồng bộ trạng thái GHN Thu Hồi thành công! Mã: " + returnCode,
                                    "ghnStatus", ghnStatus,
                                    "ghnStatusLabel", ghnStatusLabel,
                                    "internalStatus", newReturnStatus.name()
                            ));
                        }
                    }
                    if (syncReturn) {
                        return ResponseEntity.ok(Map.of("status", "error", "message", "Không thể truy vấn thông tin từ GHN API cho mã thu hồi " + returnCode));
                    }
                } else if (syncReturn) {
                    return ResponseEntity.ok(Map.of("status", "error", "message", "Đơn hàng chưa có mã GHN Thu Hồi để đồng bộ"));
                }
            }

            String ghnCode = hd.getGhnOrderCode();
            if (ghnCode == null || ghnCode.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Đơn hàng chưa có mã GHN để đồng bộ"));
            }

            if (ghnCode.startsWith("DEMO-GHN-")) {
                return ResponseEntity.ok(Map.of(
                        "status", "info",
                        "message", "Đây là vận đơn Demo Fallback (" + ghnCode + ") do hệ thống SMASH-VN tự quản lý, không thể đồng bộ trực tiếp từ GHN Sandbox.",
                        "ghnStatus", hd.getGhnStatus() != null ? hd.getGhnStatus() : "ready_to_pick",
                        "ghnStatusLabel", "Demo GHN Fallback",
                        "internalStatus", hd.getTrangThaiDonHang()
                ));
            }

            Map<String, Object> trackingData = ghnService.trackOrder(ghnCode);
            if (trackingData == null || trackingData.get("status") == null) {
                return ResponseEntity.ok(Map.of("status", "error", "message", "Không thể truy vấn thông tin từ GHN API cho mã " + ghnCode));
            }

            String ghnStatus = (String) trackingData.get("status");
            String internalStatus = ghnStatusMapper.mapToInternalStatus(ghnStatus);
            if (internalStatus == null) {
                internalStatus = hd.getTrangThaiDonHang();
            }

            orderViewService.applyShippingStatus(hd.getId(), internalStatus, ghnStatus);
            log.info("[ADMIN] Đã đồng bộ thủ công đơn #{}: GHN status={}, internalStatus={}", orderId, ghnStatus, internalStatus);

            String ghnStatusLabel = ghnStatusMapper.getGhnStatusLabel(ghnStatus);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Đồng bộ trạng thái GHN thành công!",
                    "ghnStatus", ghnStatus,
                    "ghnStatusLabel", ghnStatusLabel,
                    "internalStatus", internalStatus
            ));
        } catch (Exception e) {
            log.error("[ADMIN] Sync GHN error orderId={}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", "Lỗi đồng bộ: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Mô phỏng chuyển trạng thái kế tiếp cho vận đơn GHN Fallback (Demo
     * Simulator) POST /api/ghn/admin/demo-next-status/{orderId}
     */
    @PostMapping("/admin/demo-next-status/{orderId}")
    public ResponseEntity<?> advanceDemoNextStatus(
            @PathVariable Integer orderId,
            HttpSession session,
            HttpServletRequest request) {

        String role = (String) session.getAttribute("vaiTro");
        if (role == null || (!role.equals("QL") && !role.equals("NV"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Không có quyền truy cập"));
        }

        Integer accountId = (Integer) session.getAttribute("idNguoiDung");
        String actorName = (String) session.getAttribute("tenDangNhap");
        if (actorName == null) {
            actorName = "Admin/NV";
        }
        String clientIp = request != null ? request.getRemoteAddr() : "127.0.0.1";

        try {
            Map<String, Object> result = orderViewService.advanceDemoShippingStatus(orderId, accountId, actorName, clientIp);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of(
                    "status", "terminal",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("[DEMO_SIMULATOR] Lỗi chuyển trạng thái Demo đơn #{}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Lỗi mô phỏng trạng thái Demo: " + e.getMessage()
            ));
        }
    }

    /**
     * Webhook nhận cập nhật trạng thái đơn hàng từ GHN POST /api/ghn/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> ghnWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestParam(value = "token", required = false) String token) {
        log.info("========== GHN WEBHOOK ==========");
        log.info("Đã nhận webhook từ GHN: {}", payload);
        log.info("=================================");

        String expectedToken = ghnConfig.getWebhookToken();
        if (expectedToken == null || expectedToken.isBlank()
                || token == null || !token.equals(expectedToken)) {
            log.warn("[GHN_WEBHOOK] Webhook rejected. Invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "Unauthorized"));
        }
        log.info("[GHN_WEBHOOK] Webhook accepted.");
        try {
            String orderCode = (String) payload.get("OrderCode");
            String status = (String) payload.get("Status");

            if (orderCode == null || status == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing OrderCode or Status"));
            }

            // 1. Tìm đơn hàng theo mã vận đơn thu hồi hoặc mã vận đơn xuôi
            HoaDon hd = null;
            boolean isReturnOrderWebhook = false;
            boolean isRejectReturnOrderWebhook = false;
            boolean isExchangeOrderWebhook = false;

            for (HoaDon h : hoaDonRepository.findAll()) {
                String exCode = orderViewService.resolveGhnExchangeOrderCode(h.getId());
                if (exCode != null && exCode.equalsIgnoreCase(orderCode)) {
                    hd = h;
                    isExchangeOrderWebhook = true;
                    break;
                }
                String rCode = orderViewService.resolveGhnReturnOrderCode(h.getId(), h);
                if (rCode != null && rCode.equalsIgnoreCase(orderCode)) {
                    hd = h;
                    isReturnOrderWebhook = true;
                    break;
                }
                String rejectCode = orderViewService.resolveGhnRejectReturnCode(h.getId());
                if (rejectCode != null && rejectCode.equalsIgnoreCase(orderCode)) {
                    hd = h;
                    isRejectReturnOrderWebhook = true;
                    break;
                }
            }

            if (hd == null) {
                hd = hoaDonRepository.findByGhnOrderCode(orderCode).orElse(null);
            }

            if (hd == null) {
                String clientOrderCode = (String) payload.get("ClientOrderCode");
                if (clientOrderCode != null && !clientOrderCode.isBlank()) {
                    try {
                        String idStr = clientOrderCode.startsWith("EXCHANGE-HD-") ? clientOrderCode.substring(12) : clientOrderCode;
                        hd = hoaDonRepository.findById(Integer.parseInt(idStr)).orElse(null);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }

            if (hd != null) {
                if (isExchangeOrderWebhook || (hd.getTrangThaiHoanHang() != null && (hd.getTrangThaiHoanHang() == com.smashvn.shop.entity.ReturnStatus.EXCHANGE_STOCK_ALLOCATED || hd.getTrangThaiHoanHang() == com.smashvn.shop.entity.ReturnStatus.EXCHANGE_SHIPPING || hd.getTrangThaiHoanHang() == com.smashvn.shop.entity.ReturnStatus.EXCHANGED))) {
                    com.smashvn.shop.entity.ReturnStatus targetExchangeStatus = "delivered".equalsIgnoreCase(status)
                            ? com.smashvn.shop.entity.ReturnStatus.EXCHANGED
                            : com.smashvn.shop.entity.ReturnStatus.EXCHANGE_SHIPPING;
                    orderViewService.updateExchangeStatusFromGhn(hd.getId(), targetExchangeStatus, status, "GHN_WEBHOOK");
                    log.info("[GHN_WEBHOOK] Updated ExchangeStatus for HoaDon #{}: ghnStatus={} -> targetExchangeStatus={}", hd.getId(), status, targetExchangeStatus.name());
                    return ResponseEntity.ok(Map.of("status", "ok", "message", "Exchange status update success"));
                }

                if (isRejectReturnOrderWebhook) {
                    if ("delivered".equalsIgnoreCase(status) || "returned".equalsIgnoreCase(status) || "returned_to_sender".equalsIgnoreCase(status)) {
                        orderViewService.handleRejectReturnDeliveryFromGhn(hd.getId(), status, "GHN_WEBHOOK");
                        log.info("[GHN_WEBHOOK] Updated RejectReturnDelivery for HoaDon #{}: ghnStatus={}", hd.getId(), status);
                        return ResponseEntity.ok(Map.of("status", "ok", "message", "Reject return delivery status update success"));
                    }
                }

                if (isReturnOrderWebhook || orderViewService.resolveReturnStatus(hd.getId(), hd) != null) {
                    com.smashvn.shop.entity.ReturnStatus newReturnStatus = ghnStatusMapper.mapToReturnStatus(status);
                    if (newReturnStatus != null) {
                        orderViewService.updateReturnStatusFromGhn(hd.getId(), newReturnStatus, status, "GHN_WEBHOOK");
                        log.info("[GHN_WEBHOOK] Updated ReturnStatus for HoaDon #{}: ghnStatus={} -> newReturnStatus={}",
                                hd.getId(), status, newReturnStatus.name());
                        return ResponseEntity.ok(Map.of("status", "ok", "message", "Return status update success"));
                    }
                }

                String oldStatus = hd.getTrangThaiDonHang();
                String oldGhnStatus = hd.getGhnStatus();

                String internalStatus = ghnStatusMapper.mapToInternalStatus(status);
                if (internalStatus == null) {
                    internalStatus = oldStatus;
                }

                if (status.equalsIgnoreCase(oldGhnStatus) && internalStatus.equalsIgnoreCase(oldStatus)) {
                    log.info("[GHN_WEBHOOK] Duplicate update ignored. Order #{} is already in status {} and state {}", hd.getId(), status, internalStatus);
                    return ResponseEntity.ok(Map.of("status", "ok", "message", "Duplicate update ignored"));
                }

                orderViewService.applyShippingStatus(hd.getId(), internalStatus, status);

                log.info("[GHN_WEBHOOK] Updated HoaDon #{}: oldStatus={}, oldGhnStatus={} -> newStatus={}, newGhnStatus={}",
                        hd.getId(), oldStatus, oldGhnStatus, internalStatus, status);
                return ResponseEntity.ok(Map.of("status", "ok", "message", "Update success"));
            } else {
                log.warn("[GHN_WEBHOOK] No HoaDon found for OrderCode={}", orderCode);
                return ResponseEntity.ok(Map.of("status", "not_found", "message", "No matching order found"));
            }
        } catch (Exception e) {
            log.error("[GHN_WEBHOOK] Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
