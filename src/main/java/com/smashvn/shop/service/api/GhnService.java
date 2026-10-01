package com.smashvn.shop.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.dto.shipping.GhnOrderCreateRequestDTO;
import com.smashvn.shop.dto.shipping.GhnShipFeeRequestDTO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.exception.GhnCreateIndeterminateException;
import com.smashvn.shop.exception.GhnSandboxLimitationException;
import com.smashvn.shop.exception.GhnUnsupportedRouteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.text.Normalizer;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    private final GhnConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final SoDiaChiRepository soDiaChiRepository;
    private final GhnShipmentPersistenceService ghnShipmentPersistenceService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int LOCK_STRIPE_COUNT = 256;
    private final Object[] orderCreationLocks = new Object[LOCK_STRIPE_COUNT];

    {
        for (int i = 0; i < LOCK_STRIPE_COUNT; i++) {
            orderCreationLocks[i] = new Object();
        }
    }

    private Object getOrderLock(Integer orderId) {
        if (orderId == null) {
            return orderCreationLocks[0];
        }
        int index = Math.abs(orderId.hashCode() % LOCK_STRIPE_COUNT);
        return orderCreationLocks[index];
    }

    private String resolvedShopId = null;

    private static final String API_FEE = "/shiip/public-api/v2/shipping-order/fee";
    private static final String API_CREATE = "/shiip/public-api/v2/shipping-order/create";
    private static final String API_AVAILABLE_SERVICES = "/shiip/public-api/v2/shipping-order/available-services";
    private static final String API_DETAIL = "/shiip/public-api/v2/shipping-order/detail";
    private static final String API_DISTRICT = "/shiip/public-api/master-data/district";
    private static final String API_WARD = "/shiip/public-api/master-data/ward";
    private static final String API_PROVINCE = "/shiip/public-api/master-data/province";

    private DonViVanChuyen getGhnCarrier() {
        try {
            return donViVanChuyenDAO.findAll().stream()
                    .filter(dv -> DonViVanChuyen.isGhnCarrier(dv))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to fetch GHN carrier from DB: {}", e.getMessage());
            return null;
        }
    }

    private String getGhnToken() {
        DonViVanChuyen ghn = getGhnCarrier();
        if (ghn != null && ghn.getToken() != null && !ghn.getToken().isBlank()) {
            return ghn.getToken().trim();
        }
        return ghnConfig.getToken();
    }

    private String getGhnShopId() {
        if (resolvedShopId != null) {
            return resolvedShopId;
        }
        DonViVanChuyen ghn = getGhnCarrier();
        String configuredId = null;
        if (ghn != null && ghn.getClientId() != null && !ghn.getClientId().isBlank()) {
            configuredId = ghn.getClientId().trim();
        } else {
            configuredId = String.valueOf(ghnConfig.getShopId());
        }
        resolvedShopId = resolveShopId(configuredId, getGhnToken());
        return resolvedShopId;
    }

    private String resolveShopId(String configuredId, String token) {
        if (configuredId == null || configuredId.isBlank()) {
            return String.valueOf(ghnConfig.getShopId());
        }
        configuredId = configuredId.trim();
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shop/all";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("shops") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shops = (List<Map<String, Object>>) data.get("shops");
                    
                    // 1. If configuredId is a valid Shop ID, return it.
                    for (Map<String, Object> shop : shops) {
                        Object idObj = shop.get("_id");
                        if (idObj != null && String.valueOf(idObj).equals(configuredId)) {
                            return configuredId;
                        }
                    }
                    
                    // 2. If it is a Client ID, match the shop in district 1639 / ward 120125.
                    Integer fromDistrictId = ghnConfig.getFromDistrictId();
                    String fromWardCode = ghnConfig.getFromWardCode();
                    for (Map<String, Object> shop : shops) {
                        Object distObj = shop.get("district_id");
                        Object wardObj = shop.get("ward_code");
                        if (distObj != null && wardObj != null) {
                            if (String.valueOf(distObj).equals(String.valueOf(fromDistrictId)) && 
                                String.valueOf(wardObj).equals(fromWardCode)) {
                                return String.valueOf(shop.get("_id"));
                            }
                        }
                    }
                    
                    // 3. Fallback to first shop in list
                    if (!shops.isEmpty()) {
                        return String.valueOf(shops.get(0).get("_id"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve shop ID: {}", e.getMessage());
        }
        return configuredId;
    }

    private String findFallbackHanoiShop(String token) {
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shop/all";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("shops") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shops = (List<Map<String, Object>>) data.get("shops");
                    
                    // Look for Ba Dinh shop (Hanoi Shop ID 200610 or district 1484)
                    for (Map<String, Object> shop : shops) {
                        Object nameObj = shop.get("name");
                        Object addrObj = shop.get("address");
                        Object distObj = shop.get("district_id");
                        
                        String name = nameObj != null ? nameObj.toString().toLowerCase() : "";
                        String addr = addrObj != null ? addrObj.toString().toLowerCase() : "";
                        String dist = distObj != null ? distObj.toString() : "";
                        
                        if (name.contains("hanoi") || name.contains("hà nội") || 
                            addr.contains("hanoi") || addr.contains("hà nội") || 
                            "1484".equals(dist)) {
                            return String.valueOf(shop.get("_id"));
                        }
                    }
                    
                    if (!shops.isEmpty()) {
                        return String.valueOf(shops.get(0).get("_id"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find fallback Hanoi shop: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Tạo HttpHeaders với token GHN và shop ID (dùng cho tạo đơn và tra cứu đơn)
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", getGhnToken());
        headers.set("ShopId", getGhnShopId());
        return headers;
    }

    /**
     * Tạo HttpHeaders chỉ có token (dùng cho API tính phí và master data)
     */
    private HttpHeaders buildSimpleHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", getGhnToken());
        return headers;
    }

    /**
     * Tính phí ship từ kho đến địa chỉ người nhận
     *
     * @param toDistrictId  district_id GHN của người nhận
     * @param toWardCode    ward_code GHN của người nhận
     * @param insuranceValue giá trị bảo hiểm (tổng giá trị hàng)
     * @return phí ship (VND), null nếu lỗi
     */
    public BigDecimal calculateShipFee(Integer toDistrictId, String toWardCode, Integer insuranceValue) {
        try {
            GhnServiceInfo serviceInfo = resolveAvailableService(getGhnShopId(), ghnConfig.getFromDistrictId(), toDistrictId);
            GhnShipFeeRequestDTO req = new GhnShipFeeRequestDTO();
            req.setFromDistrictId(ghnConfig.getFromDistrictId());
            req.setFromWardCode(ghnConfig.getFromWardCode());
            req.setToDistrictId(toDistrictId);
            req.setToWardCode(toWardCode);
            req.setInsuranceValue(insuranceValue);
            req.setServiceId(serviceInfo.getServiceId());
            req.setServiceTypeId(serviceInfo.getServiceTypeId());

            String url = ghnConfig.getBaseUrl() + API_FEE;
            HttpEntity<GhnShipFeeRequestDTO> request = new HttpEntity<>(req, buildHeaders());

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});

            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("total") != null) {
                    return new BigDecimal(data.get("total").toString());
                }
            }
            log.warn("GHN calculateShipFee failed: code={}, message={}", code, response.get("message"));
        } catch (Exception e) {
            log.error("GHN calculateShipFee error: {}", e.getMessage());
        }
        // Fallback: 30,000đ
        return new BigDecimal("30000");
    }

    @SuppressWarnings("unchecked")
    private GhnServiceInfo resolveAvailableService(String shopId, Integer fromDistrictId, Integer toDistrictId) {
        if (fromDistrictId == null || toDistrictId == null) {
            throw new IllegalArgumentException("Thiếu quận/huyện gửi hoặc nhận để kiểm tra dịch vụ GHN.");
        }

        try {
            Map<String, Object> body = Map.of(
                    "shop_id", Integer.valueOf(shopId),
                    "from_district", fromDistrictId,
                    "to_district", toDistrictId
            );
            String url = ghnConfig.getBaseUrl() + API_AVAILABLE_SERVICES;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = response.get("code") instanceof Number ? ((Number) response.get("code")).intValue() : null;
            if (code != null && code == 200) {
                List<Map<String, Object>> services = (List<Map<String, Object>>) response.get("data");
                if (services != null && !services.isEmpty()) {
                    Map<String, Object> selected = services.stream()
                            .filter(s -> intValue(s.get("service_type_id")) != null && intValue(s.get("service_type_id")) == 2)
                            .findFirst()
                            .orElse(services.get(0));
                    Integer serviceId = intValue(selected.get("service_id"));
                    Integer serviceTypeId = intValue(selected.get("service_type_id"));
                    if (serviceId != null || serviceTypeId != null) {
                        log.debug("GHN service selected for route {} -> {}: service_id={}, service_type_id={}",
                                fromDistrictId, toDistrictId, serviceId, serviceTypeId);
                        return new GhnServiceInfo(serviceId, serviceTypeId != null ? serviceTypeId : 2);
                    }
                }
            }

            String msg = response.get("message") != null ? response.get("message").toString() : "Không có dịch vụ khả dụng";
            if ((code != null && code == 200) || isUnsupportedRouteResponse(msg)) {
                throw new GhnUnsupportedRouteException("GHN chưa hỗ trợ tuyến giao hàng này: " + msg);
            }
            throw new IllegalStateException("GHN không thể kiểm tra dịch vụ cho tuyến giao hàng: " + msg);
        } catch (GhnUnsupportedRouteException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            String msg = responseBody;
            try {
                Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});
                if (response.get("message") != null) {
                    msg = response.get("message").toString();
                }
            } catch (Exception ignored) {
                // Keep raw response body.
            }
            if (isUnsupportedRouteResponse(msg) || isUnsupportedRouteResponse(responseBody)) {
                throw new GhnUnsupportedRouteException("GHN chưa hỗ trợ tuyến giao hàng này: " + msg, e);
            }
            throw e;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xử lý phản hồi dịch vụ GHN.", e);
        }
    }

    private boolean isUnsupportedRouteResponse(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeString(value);
        return normalized.contains("khong ho tro tuyen")
                || normalized.contains("tuyen duong khong ho tro")
                || normalized.contains("khong co dich vu kha dung")
                || normalized.contains("khong tim thay dich vu")
                || normalized.contains("route not supported")
                || normalized.contains("unsupported route")
                || normalized.contains("service not available")
                || normalized.contains("no available service");
    }

    private boolean isKnownSandboxWarehouseLimitation(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeString(value);
        return normalized.contains("khong lay duoc thong tin kho")
                || normalized.contains("khong tim thay thong tin kho")
                || normalized.contains("cannot get shop information")
                || normalized.contains("failed to get shop information")
                || normalized.contains("shop information not found");
    }

    private Integer intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Tra cứu mã vận đơn GHN đã tồn tại trong database (TichHopVanChuyen)
     */
    public String findExistingGhnCode(Integer idHoaDon) {
        if (idHoaDon == null) return null;
        try {
            List<String> codes = jdbcTemplate.query(
                "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap IN ('GHN', 'GHN_FALLBACK') AND ma_van_don IS NOT NULL AND RTRIM(LTRIM(ma_van_don)) <> '' ORDER BY id DESC",
                (rs, rowNum) -> rs.getString("ma_van_don"),
                idHoaDon
            );
            if (!codes.isEmpty() && codes.get(0) != null && !codes.get(0).isBlank()) {
                return codes.get(0).trim();
            }
        } catch (Exception e) {
            log.warn("Failed to check existing GHN code for HoaDon #{}: {}", idHoaDon, e.getMessage());
        }
        return null;
    }

    /**
     * Kiểm tra xem đơn hàng có đang ở trạng thái GHN_CREATE_UNKNOWN do timeout lần trước hay không
     */
    public boolean hasUnknownGhnCreateStatus(Integer idHoaDon) {
        if (idHoaDon == null) return false;
        try {
            List<String> statuses = jdbcTemplate.query(
                "SELECT trang_thai FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN' ORDER BY id DESC",
                (rs, rowNum) -> rs.getString("trang_thai"),
                idHoaDon
            );
            return !statuses.isEmpty() && "GHN_CREATE_UNKNOWN".equalsIgnoreCase(statuses.get(0));
        } catch (Exception e) {
            log.warn("Failed to check unknown GHN create status for HoaDon #{}: {}", idHoaDon, e.getMessage());
            return false;
        }
    }

    public void clearUnknownGhnCreateStatus(Integer idHoaDon) {
        if (idHoaDon == null) return;
        try {
            jdbcTemplate.update(
                "DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN' AND trang_thai = 'GHN_CREATE_UNKNOWN'",
                idHoaDon
            );
            log.info("Cleared GHN_CREATE_UNKNOWN status for HoaDon #{}", idHoaDon);
        } catch (Exception e) {
            log.warn("Failed to clear GHN_CREATE_UNKNOWN status for HoaDon #{}: {}", idHoaDon, e.getMessage());
        }
    }

    public SoDiaChi getHoaDonDiaChiSafe(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getDiaChi() == null) {
            return null;
        }
        try {
            Integer diaChiId = hoaDon.getDiaChi().getId();
            if (diaChiId != null) {
                return soDiaChiRepository.findById(diaChiId).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve SoDiaChi safely for HoaDon #{}: {}", hoaDon.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Tra cứu thông tin vận đơn GHN theo client_order_code (mã đơn shop)
     * Theo tài liệu chính thức GHN API v2, response có trường "data" là Array/List các order.
     * Phương thức này trích xuất Map chi tiết đơn hàng (hoặc phần tử đầu tiên hợp lệ) từ data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrderDetailByClientOrderCode(String clientOrderCode) {
        if (clientOrderCode == null || clientOrderCode.isBlank()) {
            return null;
        }
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/detail-by-client-code";
            Map<String, Object> body = Map.of("client_order_code", clientOrderCode.trim());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
                Integer code = (Integer) response.get("code");
                if (code != null && code == 200) {
                    Object dataObj = response.get("data");
                    if (dataObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> itemMap) {
                                Object orderCode = itemMap.get("order_code");
                                if (orderCode != null && !String.valueOf(orderCode).trim().isBlank()) {
                                    return (Map<String, Object>) itemMap;
                                }
                            }
                        }
                    } else if (dataObj instanceof Map<?, ?> map) {
                        Object orderCode = map.get("order_code");
                        if (orderCode != null && !String.valueOf(orderCode).trim().isBlank()) {
                            return (Map<String, Object>) map;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("GHN detail-by-client-code query failed for client_order_code {}: {}", clientOrderCode, e.getMessage());
        }
        return null;
    }

    public boolean isOrderNotFoundOnGhn(String clientOrderCode) {
        if (clientOrderCode == null || clientOrderCode.isBlank()) {
            return false;
        }
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/detail-by-client-code";
            Map<String, Object> body = Map.of("client_order_code", clientOrderCode.trim());
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            return false;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            if (respBody != null && (respBody.contains("Đơn hàng không tồn tại") || respBody.contains("not found"))) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Tự động Reconcile với GHN nếu đơn hàng từng bị timeout
     */
    public String reconcileExistingGhnOrder(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getMaDonHang() == null) return null;
        try {
            Map<String, Object> detail = getOrderDetailByClientOrderCode(hoaDon.getMaDonHang());
            if (detail != null && detail.get("order_code") != null) {
                String orderCode = String.valueOf(detail.get("order_code")).trim();
                if (!orderCode.isBlank()) {
                    log.info("[GHN_RECONCILE_SUCCESS] Reconciled GHN shipping order {} by client_order_code {} for HoaDon #{}",
                            orderCode, hoaDon.getMaDonHang(), hoaDon.getId());
                    ghnShipmentPersistenceService.saveShipment(hoaDon.getId(), orderCode, "GHN", "ready_to_pick");
                    hoaDon.setGhnOrderCode(orderCode);
                    hoaDon.setGhnStatus("ready_to_pick");
                    return orderCode;
                }
            }
        } catch (Exception e) {
            log.warn("[GHN_RECONCILE] Reconcile attempt failed for HoaDon #{}: {}", hoaDon.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Trả về số lượng fixed striped locks
     */
    public int getLockStripeCount() {
        return orderCreationLocks.length;
    }

    public int getActiveOrderCreationLocksCount() {
        return orderCreationLocks.length;
    }

    /**
     * Tạo đơn vận chuyển trên GHN sau khi đặt hàng thành công
     *
     * @param hoaDon HoaDon entity đã được lưu
     * @param items  Danh sách HoaDonChiTiet
     * @param toDistrictId district_id người nhận
     * @param toWardCode   ward_code người nhận
     * @return order_code GHN (mã vận đơn), null nếu lỗi
     */
    public String createShippingOrder(HoaDon hoaDon, List<HoaDonChiTiet> items,
                                      Integer toDistrictId, String toWardCode) {
        try {
            return createShippingOrderOrThrow(hoaDon, items, toDistrictId, toWardCode);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("GHN createShippingOrder HTTP error for HoaDon #{}: {} - {}", hoaDon.getId(), e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("GHN createShippingOrder error for HoaDon #{}: {}", hoaDon.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Tạo đơn vận chuyển trên GHN và ném ngoại lệ nếu có lỗi (để Admin controller hiển thị lỗi)
     */
    private Map<String, Object> getShopDetails(String shopId, String token) {
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shop/all";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("shops") != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shops = (List<Map<String, Object>>) data.get("shops");
                    for (Map<String, Object> shop : shops) {
                        Object idObj = shop.get("_id");
                        if (idObj != null && String.valueOf(idObj).equals(shopId)) {
                            return shop;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch shop details from GHN: {}", e.getMessage());
        }
        return null;
    }

    public String createShippingOrderOrThrow(HoaDon hoaDon, List<HoaDonChiTiet> items,
                                             Integer toDistrictId, String toWardCode) throws Exception {
        return createShippingOrderOrThrow(hoaDon, items, toDistrictId, toWardCode, false);
    }

    public String createShippingOrderOrThrow(HoaDon hoaDon, List<HoaDonChiTiet> items,
                                             Integer toDistrictId, String toWardCode,
                                             boolean forceRetry) throws Exception {
        if (hoaDon == null) {
            throw new IllegalArgumentException("Không thể tạo vận đơn cho đơn hàng null.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Không thể tạo vận đơn cho đơn hàng không có sản phẩm.");
        }

        // Fast check outside lock: Reconcile / Idempotency
        String existingCode = findExistingGhnCode(hoaDon.getId());
        if (existingCode != null && !existingCode.isBlank()) {
            log.info("[GHN_CONCURRENCY] Order #{} already has GHN code: {}. Returning existing code without calling GHN API.", hoaDon.getId(), existingCode);
            hoaDon.setGhnOrderCode(existingCode);
            return existingCode;
        }

        // Fixed Striped Lock: prevents concurrent threads/requests from double-calling GHN for the same orderId
        Object lock = getOrderLock(hoaDon.getId());
        synchronized (lock) {
            // Double check inside lock
            existingCode = findExistingGhnCode(hoaDon.getId());
            if (existingCode != null && !existingCode.isBlank()) {
                log.info("[GHN_CONCURRENCY] Order #{} already has GHN code (double-check inside lock): {}. Returning existing code.", hoaDon.getId(), existingCode);
                hoaDon.setGhnOrderCode(existingCode);
                return existingCode;
            }

            // Check if order is currently in GHN_CREATE_UNKNOWN state
            if (hasUnknownGhnCreateStatus(hoaDon.getId())) {
                // Try reconciling with GHN first
                String reconciledCode = reconcileExistingGhnOrder(hoaDon);
                if (reconciledCode != null && !reconciledCode.isBlank()) {
                    log.info("[GHN_RECONCILE] Successfully resolved existing shipping order {} for HoaDon #{} from GHN.",
                            reconciledCode, hoaDon.getId());
                    return reconciledCode;
                }

                // If GHN confirms order does not exist, clear the unknown lock
                if (isOrderNotFoundOnGhn(hoaDon.getMaDonHang())) {
                    log.info("[GHN_RECONCILE] GHN confirmed order {} does not exist. Clearing GHN_CREATE_UNKNOWN status for HoaDon #{}.",
                            hoaDon.getMaDonHang(), hoaDon.getId());
                    clearUnknownGhnCreateStatus(hoaDon.getId());
                } else if (!forceRetry) {
                    // If reconcile did not find order and admin did not explicitly confirm force retry -> BLOCK
                    log.warn("[GHN_CREATE_UNKNOWN_BLOCKED] Order #{} is in GHN_CREATE_UNKNOWN state. Blocked automatic retry.", hoaDon.getId());
                    throw new GhnCreateIndeterminateException("Kết quả tạo vận đơn GHN trước đó chưa xác định (GHN_CREATE_UNKNOWN). Vui lòng kiểm tra trên GHN trước khi tạo lại để tránh tạo trùng vận đơn.");
                } else {
                    log.info("[GHN_FORCE_RETRY] Admin confirmed force retry for HoaDon #{}.", hoaDon.getId());
                    clearUnknownGhnCreateStatus(hoaDon.getId());
                }
            }

            if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
                SoDiaChi dc = getHoaDonDiaChiSafe(hoaDon);
                if (dc != null && dc.getDistrictId() != null && dc.getWardCode() != null) {
                    toDistrictId = dc.getDistrictId();
                    toWardCode = dc.getWardCode();
                } else if (hoaDon.getKhachHang() != null) {
                    try {
                        List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(hoaDon.getKhachHang().getId());
                        if (addresses != null) {
                            for (SoDiaChi sdc : addresses) {
                                if (sdc.getSdtNguoiNhan() != null 
                                        && sdc.getSdtNguoiNhan().trim().equalsIgnoreCase(hoaDon.getSdtNhan() != null ? hoaDon.getSdtNhan().trim() : "")
                                        && sdc.getGhnDistrictId() != null 
                                        && sdc.getGhnWardCode() != null) {
                                    toDistrictId = sdc.getGhnDistrictId();
                                    toWardCode = sdc.getGhnWardCode();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to resolve district/ward from SoDiaChi for GHN order: {}", e.getMessage());
                    }
                }
            }

            if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
                log.error("[RESOLVE_ADDRESS_FAILED] HoaDon #{} (maDonHang: {}) missing district/ward code for GHN",
                        hoaDon.getId(), hoaDon.getMaDonHang());
                throw new IllegalArgumentException("Đơn hàng #" + hoaDon.getId() + " thiếu mã Quận/Huyện hoặc Phường/Xã GHN hợp lệ.");
            }

            // Phase 1: RESOLVE_SHOP
            String shopIdStr;
            String tokenStr;
            Map<String, Object> shopDetails = null;
            try {
                shopIdStr = getGhnShopId();
                tokenStr = getGhnToken();
                shopDetails = getShopDetails(shopIdStr, tokenStr);
            } catch (Exception shopEx) {
                log.error("[GHN][RESOLVE_SHOP] Failed to resolve GHN shop/token for HoaDon #{} (maDonHang: {}, districtId: {}, wardCode: {}): {} - {}",
                        hoaDon.getId(), hoaDon.getMaDonHang(), toDistrictId, toWardCode,
                        shopEx.getClass().getSimpleName(), shopEx.getMessage());
                if (isSandboxEnvironment() && isEligibleForSandboxFallback(shopEx)) {
                    log.info("[GHN][SANDBOX_FALLBACK] HoaDon #{} -> Generating Sandbox Demo fallback due to resolve shop error", hoaDon.getId());
                    return createFallbackShippingOrder(hoaDon, shopEx);
                }
                throw shopEx;
            }

            // Phase 2: RESOLVE_SERVICE
            GhnServiceInfo serviceInfo = null;
            Integer fromDistrictId = (shopDetails != null && shopDetails.get("district_id") != null)
                    ? ((Number) shopDetails.get("district_id")).intValue()
                    : ghnConfig.getFromDistrictId();
            try {
                serviceInfo = resolveAvailableService(shopIdStr, fromDistrictId, toDistrictId);
            } catch (Exception serviceEx) {
                log.error("[GHN][RESOLVE_SERVICE] Failed to resolve available service for HoaDon #{} (route {} -> {}): {} - {}",
                        hoaDon.getId(), fromDistrictId, toDistrictId,
                        serviceEx.getClass().getSimpleName(), serviceEx.getMessage());
                if (isSandboxEnvironment() && isEligibleForSandboxFallback(serviceEx)) {
                    log.info("[GHN][SANDBOX_FALLBACK] HoaDon #{} -> Generating Sandbox Demo fallback due to resolve service error", hoaDon.getId());
                    return createFallbackShippingOrder(hoaDon, serviceEx);
                }
                throw serviceEx;
            }

            // Phase 3: CREATE_ORDER
            try {
                return executeCreateOrderCall(shopIdStr, tokenStr, hoaDon, items, toDistrictId, toWardCode, serviceInfo, shopDetails);
            } catch (GhnCreateIndeterminateException ghnIndEx) {
                // Record GHN_CREATE_UNKNOWN in TichHopVanChuyen so subsequent retries are protected
                ghnShipmentPersistenceService.saveShipment(hoaDon.getId(), null, "GHN", "GHN_CREATE_UNKNOWN");
                log.error("[GHN][GHN_CREATE_RESULT_UNKNOWN] Indeterminate create result for HoaDon #{} (maDonHang: {}, districtId: {}, wardCode: {}): {} - {}",
                        hoaDon.getId(), hoaDon.getMaDonHang(), toDistrictId, toWardCode,
                        ghnIndEx.getClass().getSimpleName(), ghnIndEx.getMessage());
                throw ghnIndEx;
            } catch (Exception e) {
                Exception lastException = e;
                if (isKnownSandboxWarehouseLimitation(e.getMessage())) {
                    log.warn("GHN: Primary Shop ID {} hit the known Sandbox warehouse-information limitation. Attempting fallback shop if available...", shopIdStr);
                    String fallbackShopId = findFallbackHanoiShop(tokenStr);
                    if (fallbackShopId != null && !fallbackShopId.equals(shopIdStr)) {
                        log.info("GHN: Retrying order creation with fallback Hanoi Shop ID {}", fallbackShopId);
                        try {
                            Map<String, Object> fallbackDetails = getShopDetails(fallbackShopId, tokenStr);
                            Integer fallbackFromDist = (fallbackDetails != null && fallbackDetails.get("district_id") != null)
                                    ? ((Number) fallbackDetails.get("district_id")).intValue()
                                    : ghnConfig.getFromDistrictId();
                            GhnServiceInfo fallbackServiceInfo = resolveAvailableService(fallbackShopId, fallbackFromDist, toDistrictId);
                            return executeCreateOrderCall(fallbackShopId, tokenStr, hoaDon, items, toDistrictId, toWardCode, fallbackServiceInfo, fallbackDetails);
                        } catch (GhnCreateIndeterminateException ghnIndEx) {
                            ghnShipmentPersistenceService.saveShipment(hoaDon.getId(), null, "GHN", "GHN_CREATE_UNKNOWN");
                            log.error("[GHN][GHN_CREATE_RESULT_UNKNOWN] Indeterminate create result on fallback shop for HoaDon #{} (maDonHang: {}): {}",
                                    hoaDon.getId(), hoaDon.getMaDonHang(), ghnIndEx.getMessage());
                            throw ghnIndEx;
                        } catch (Exception ex) {
                            log.error("GHN: Fallback to Hanoi Shop failed: {}", ex.getMessage());
                            lastException = ex;
                        }
                    }
                }

                log.error("[GHN][CREATE_ORDER] Failed for HoaDon #{} (maDonHang: {}, districtId: {}, wardCode: {}): {} - {}",
                        hoaDon.getId(), hoaDon.getMaDonHang(), toDistrictId, toWardCode,
                        lastException.getClass().getSimpleName(), lastException.getMessage());

                if (isSandboxEnvironment() && isEligibleForSandboxFallback(lastException)) {
                    log.info("[GHN][SANDBOX_FALLBACK] HoaDon #{} -> Generating Sandbox Demo fallback due to create order error", hoaDon.getId());
                    return createFallbackShippingOrder(hoaDon, lastException);
                }
                throw lastException;
            }
        }
    }

    /**
     * Kiểm tra xem môi trường hiện tại có phải GHN Sandbox hay không.
     * Smart Fallback CHỈ được phép kích hoạt trên Sandbox.
     */
    public boolean isSandboxEnvironment() {
        String baseUrl = ghnConfig.getBaseUrl();
        if (baseUrl == null) return false;
        String lower = baseUrl.toLowerCase().trim();
        return lower.contains("dev.ghn.vn") || lower.contains("5sao") || lower.contains("dev-online-gateway") || lower.contains("sandbox");
    }

    /**
     * Phân loại exception: Chỉ cho phép Fallback khi GHN Sandbox xác nhận tuyến/dịch vụ không được hỗ trợ.
     * KHÔNG fallback với lỗi dữ liệu nội bộ (null address, invalid items, NPE, DB errors, validation errors, 401/403 credentials).
     * KHÔNG fallback cho lỗi kết quả không xác định (GhnCreateIndeterminateException).
     */
    public boolean isEligibleForSandboxFallback(Exception e) {
        if (!isSandboxEnvironment()) {
            log.info("GHN: Environment is PRODUCTION. Smart Fallback is disabled.");
            return false;
        }
        if (e == null) {
            return false;
        }

        // BỎ QUA: Lỗi không xác định kết quả POST create -> KHÔNG ĐƯỢC FALLBACK để tránh duplicate
        if (e instanceof GhnCreateIndeterminateException) {
            log.warn("GHN: GhnCreateIndeterminateException (POST create timeout). Skipping Smart Fallback to prevent duplicate.");
            return false;
        }

        // ĐỦ ĐIỀU KIỆN Fallback: Sandbox không hỗ trợ tuyến giao hàng
        if (e instanceof GhnUnsupportedRouteException) {
            log.info("GHN: GhnUnsupportedRouteException detected on Sandbox. Eligible for Smart Fallback.");
            return true;
        }

        // BỎ QUA không fallback cho các lỗi dữ liệu nội bộ / lập trình / database
        if (e instanceof IllegalArgumentException ||
            e instanceof NullPointerException ||
            e instanceof IllegalStateException ||
            e instanceof org.springframework.dao.DataAccessException ||
            e instanceof jakarta.persistence.PersistenceException) {
            log.warn("GHN: Exception [{}] is internal data/validation error. Skipping Smart Fallback.", e.getClass().getSimpleName());
            return false;
        }

        // Phân loại HttpStatusCodeException từ RestTemplate
        if (e instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
            int statusCode = httpEx.getStatusCode().value();
            // HTTP 401 (Unauthorized), HTTP 403 (Forbidden): Lỗi sai Token / Shop ID / Phân quyền -> KHÔNG FALLBACK
            if (statusCode == 401 || statusCode == 403) {
                log.warn("GHN: HttpStatusCodeException [{}] (Unauthorized/Forbidden). Credentials/Permission error. Skipping Smart Fallback.", statusCode);
                return false;
            }
            // Chỉ fallback nếu chính phản hồi xác nhận tuyến/dịch vụ không được hỗ trợ.
            String responseBody = httpEx.getResponseBodyAsString();
            if (isUnsupportedRouteResponse(responseBody)) {
                log.info("GHN: Unsupported Sandbox route response [{}] in HTTP [{}]. Eligible for Smart Fallback.", responseBody, statusCode);
                return true;
            }
            log.warn("GHN: HttpStatusCodeException [{}] with body [{}] is not an unsupported-route error. Skipping Smart Fallback.", statusCode, responseBody);
            return false;
        }

        // GHN đã phản hồi dứt khoát một giới hạn nghiệp vụ đã biết của Sandbox.
        // Đây không phải timeout nên không có rủi ro kết quả tạo đơn chưa xác định.
        if (e instanceof GhnSandboxLimitationException) {
            log.info("GHN: Known Sandbox limitation detected. Eligible for Demo Fallback.");
            return true;
        }

        Throwable cause = e.getCause();
        if (cause instanceof Exception causeEx && isEligibleForSandboxFallback(causeEx)) {
            return true;
        }

        String msg = e.getMessage() != null ? e.getMessage() : "";
        log.warn("GHN: Exception [{}: {}] is not classified for Sandbox Fallback.", e.getClass().getSimpleName(), msg);
        return false;
    }

    /**
     * Sinh mã vận đơn DEMO Fallback và lưu vào bảng TichHopVanChuyen với nha_cung_cap = 'GHN_FALLBACK'.
     * Tái sử dụng GhnShipmentPersistenceService.saveShipment (với REQUIRES_NEW transaction) và cập nhật HoaDon.
     */
    public String createFallbackShippingOrder(HoaDon hoaDon, Exception cause) {
        if (!isSandboxEnvironment()) {
            throw new IllegalStateException("Không thể tạo mã vận đơn Demo trên môi trường Production.");
        }
        if (!isEligibleForSandboxFallback(cause)) {
            throw new IllegalArgumentException(
                    "Chỉ được tạo vận đơn Demo khi GHN Sandbox xác nhận tuyến/dịch vụ không hỗ trợ hoặc gặp giới hạn kho đã biết.",
                    cause);
        }
        if (hoaDon == null || hoaDon.getId() == null) {
            throw new IllegalArgumentException("Không thể tạo vận đơn Demo cho hóa đơn chưa được lưu.");
        }
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueSuffix = String.format("%04d", (int)(Math.random() * 10000));
        Integer orderId = hoaDon.getId();
        String demoCode = "DEMO-GHN-" + timestamp + "-" + orderId + "-" + uniqueSuffix;

        log.warn("[GHN][SANDBOX_FALLBACK] GHN Sandbox cannot complete the supported demo flow for HoaDon #{}. Generating Fallback Code: {} (Reason: {})",
                orderId, demoCode, cause != null ? cause.getMessage() : "Unknown");

        ghnShipmentPersistenceService.saveShipment(orderId, demoCode, "GHN_FALLBACK", "ready_to_pick");
        hoaDon.setGhnOrderCode(demoCode);
        hoaDon.setGhnStatus("ready_to_pick");
        log.info("[GHN][SANDBOX_FALLBACK] Successfully persisted fallback shipment mapping (GHN_FALLBACK) for HoaDon #{}", orderId);

        return demoCode;
    }

    private String executeCreateOrderCall(String shopId, String token, HoaDon hoaDon, List<HoaDonChiTiet> items,
                                          Integer toDistrictId, String toWardCode,
                                          GhnServiceInfo serviceInfo, Map<String, Object> shopDetails) throws Exception {
        GhnOrderCreateRequestDTO req = new GhnOrderCreateRequestDTO();
        req.setClient_order_code(hoaDon.getMaDonHang());
        
        req.setFrom_name("SmashVN Shop");
        req.setFrom_phone("0835420088");
        
        req.setFrom_address(ghnConfig.getFromAddress());
        if (shopDetails != null) {
            req.setFrom_district_id(shopDetails.get("district_id") != null ? ((Number) shopDetails.get("district_id")).intValue() : ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(shopDetails.get("ward_code") != null ? shopDetails.get("ward_code").toString() : ghnConfig.getFromWardCode());
        } else {
            req.setFrom_district_id(ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(ghnConfig.getFromWardCode());
        }

        String receiverName = null;
        if (hoaDon.getKhachHang() != null) {
            try {
                List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(hoaDon.getKhachHang().getId());
                if (addresses != null) {
                    for (SoDiaChi sdc : addresses) {
                        if (sdc.getSdtNguoiNhan() != null && sdc.getSdtNguoiNhan().trim().equals(hoaDon.getSdtNhan().trim())) {
                            receiverName = (sdc.getHoNguoiNhan() + " " + sdc.getTenNguoiNhan()).trim();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to lookup receiver name from SoDiaChi: {}", e.getMessage());
            }
            if (receiverName == null || receiverName.isEmpty()) {
                receiverName = (hoaDon.getKhachHang().getHoKh() + " " + hoaDon.getKhachHang().getTenKh()).trim();
            }
        }
        if (receiverName == null || receiverName.isEmpty()) {
            receiverName = "Khách hàng";
        }

        req.setTo_name(receiverName);
        req.setTo_phone(hoaDon.getSdtNhan());
        req.setTo_address(hoaDon.getDiaChiNhan());
        req.setTo_district_id(toDistrictId);
        req.setTo_ward_code(toWardCode);
        if (serviceInfo != null) {
            req.setService_id(serviceInfo.getServiceId());
            req.setService_type_id(serviceInfo.getServiceTypeId());
        }

        // COD amount = 0 nếu đã thanh toán online
        boolean isOnlinePaid = "PAID".equals(hoaDon.getPaymentStatus()) || "ZaloPay".equalsIgnoreCase(hoaDon.getPaymentMethod());
        req.setCod_amount(isOnlinePaid ? 0 : hoaDon.getTongTien().intValue());
        req.setInsurance_value(hoaDon.getTongTien().intValue());

        req.setNote(hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "");

        // Tính toán tổng số lượng và thiết lập kích thước/trọng lượng động cho gói hàng
        int totalQty = items.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum();
        req.setWeight(totalQty * 500); // 500g mỗi vợt (kèm bao và hộp đóng gói)
        req.setHeight(Math.min(150, 10 + (totalQty - 1) * 2)); // Tăng chiều cao hộp khi gửi nhiều cây vợt

        // Build items
        List<GhnOrderCreateRequestDTO.GhnItemDTO> ghnItems = new ArrayList<>();
        for (HoaDonChiTiet ct : items) {
            GhnOrderCreateRequestDTO.GhnItemDTO item = new GhnOrderCreateRequestDTO.GhnItemDTO();
            String classification = ct.getSanPhamChiTiet().getPhanLoaiHienThi();
            String detailName = ct.getSanPhamChiTiet().getSanPham().getTenSanPham() + (!classification.isEmpty() ? " [" + classification + "]" : "");
            item.setName(detailName);
            item.setCode("SP-" + ct.getSanPhamChiTiet().getId());
            item.setQuantity(ct.getSoLuong());
            item.setPrice(ct.getDonGia().intValue());
            ghnItems.add(item);
        }
        req.setItems(ghnItems);

        String url = ghnConfig.getBaseUrl() + API_CREATE;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        
        HttpEntity<GhnOrderCreateRequestDTO> request = new HttpEntity<>(req, headers);
        
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String orderCode = (String) data.get("order_code");
                    log.info("GHN: Created shipping order {} for HoaDon #{} using Shop ID {}", orderCode, hoaDon.getId(), shopId);
                    try {
                        jdbcTemplate.update(
                            "MERGE INTO TichHopVanChuyen WITH (HOLDLOCK) AS target " +
                            "USING (SELECT ? AS id_hoa_don, ? AS ma_van_don, ? AS trang_thai) AS source " +
                            "ON target.id_hoa_don = source.id_hoa_don AND target.nha_cung_cap = 'GHN' " +
                            "WHEN MATCHED THEN UPDATE SET ma_van_don = COALESCE(source.ma_van_don, target.ma_van_don), " +
                            "                             ma_don_hang_ngoai = COALESCE(source.ma_van_don, target.ma_don_hang_ngoai), " +
                            "                             trang_thai = COALESCE(source.trang_thai, target.trang_thai) " +
                            "WHEN NOT MATCHED THEN INSERT (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) " +
                            "VALUES (source.id_hoa_don, 'GHN', source.ma_van_don, source.ma_van_don, source.trang_thai, GETDATE());",
                            hoaDon.getId(), orderCode, "ready_to_pick"
                        );
                        log.info("GHN: Successfully saved shipping mapping in TichHopVanChuyen for HoaDon #{}", hoaDon.getId());
                    } catch (Exception dbEx) {
                        log.error("GHN: Failed to save shipping mapping in TichHopVanChuyen for HoaDon #{}: {}", hoaDon.getId(), dbEx.getMessage(), dbEx);
                    }
                    return orderCode;
                }
            }
            String msg = response.get("message") != null ? response.get("message").toString() : "Không rõ lý do";
            if (isKnownSandboxWarehouseLimitation(msg)) {
                throw new GhnSandboxLimitationException("GHN Sandbox không lấy được thông tin kho: " + msg);
            }
            if (isUnsupportedRouteResponse(msg)) {
                throw new GhnUnsupportedRouteException("GHN chưa hỗ trợ tuyến giao hàng này: " + msg);
            }
            throw new RuntimeException("Đơn vị vận chuyển GHN từ chối tạo đơn: " + msg);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("[GHN][GHN_CREATE_RESULT_UNKNOWN] Timeout/Network error during POST create for HoaDon #{} (maDonHang: {}, districtId: {}, wardCode: {}): {}",
                    hoaDon.getId(), hoaDon.getMaDonHang(), toDistrictId, toWardCode, e.getMessage());
            throw new GhnCreateIndeterminateException("Kết nối tới GHN bị timeout khi tạo vận đơn. Chưa thể xác định đơn vị vận chuyển đã tạo đơn hay chưa. Vui lòng kiểm tra lại trên hệ thống GHN hoặc thử lại sau.", e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            String msg = null;
            try {
                Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
                msg = (String) response.get("message");
            } catch (Exception ignored) {}
            if (msg == null || msg.isBlank()) {
                msg = "HTTP " + e.getStatusCode() + ": " + body;
            }
            log.error("[GHN][CREATE_ORDER] HTTP Error {}: {}", e.getStatusCode(), body);
            if (isKnownSandboxWarehouseLimitation(msg) || isKnownSandboxWarehouseLimitation(body)) {
                throw new GhnSandboxLimitationException("GHN Sandbox không lấy được thông tin kho: " + msg, e);
            }
            throw new RuntimeException("Đơn vị vận chuyển GHN từ chối tạo đơn: " + msg, e);
        }
    }

    /**
     * Tạo vận đơn GHN chiều trả (từ Khách hàng về lại Shop) khi Admin duyệt yêu cầu trả hàng
     */
    @SuppressWarnings("unchecked")
    public String createReturnShippingOrder(HoaDon hoaDon, List<HoaDonChiTiet> items) {
        try {
            Integer toDistrictId = ghnConfig.getFromDistrictId();
            String toWardCode = ghnConfig.getFromWardCode();
            SoDiaChi returnDc = getHoaDonDiaChiSafe(hoaDon);
            Integer fromDistrictId = (returnDc != null && returnDc.getDistrictId() != null) ? returnDc.getDistrictId() : ghnConfig.getFromDistrictId();
            String fromWardCode = (returnDc != null && returnDc.getWardCode() != null) ? returnDc.getWardCode() : ghnConfig.getFromWardCode();
            
            String shopIdStr = getGhnShopId();
            String tokenStr = getGhnToken();
            
            GhnOrderCreateRequestDTO req = new GhnOrderCreateRequestDTO();
            String senderName = (hoaDon.getKhachHang() != null ? (hoaDon.getKhachHang().getHoKh() + " " + hoaDon.getKhachHang().getTenKh()) : "Khách hàng").trim();
            req.setFrom_name(senderName);
            req.setFrom_phone(hoaDon.getSdtNhan() != null ? hoaDon.getSdtNhan() : "0900000000");
            req.setFrom_address(hoaDon.getDiaChiNhan() != null ? hoaDon.getDiaChiNhan() : "Địa chỉ khách hàng");
            req.setFrom_district_id(fromDistrictId != null ? fromDistrictId : ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(fromWardCode != null ? fromWardCode : ghnConfig.getFromWardCode());
            
            req.setTo_name("SmashVN Shop (Kho Nhận Hàng Trả)");
            req.setTo_phone("0835420088");
            req.setTo_address(ghnConfig.getFromAddress());
            req.setTo_district_id(toDistrictId);
            req.setTo_ward_code(toWardCode);
            
            req.setCod_amount(0);
            req.setInsurance_value(hoaDon.getTongTien() != null ? hoaDon.getTongTien().intValue() : 0);
            req.setNote("ĐƠN HÀNG THU HỒI TRẢ VỀ SHOP - " + (hoaDon.getLyDoHoanTien() != null ? hoaDon.getLyDoHoanTien() : ""));
            
            int totalQty = items != null ? items.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum() : 1;
            req.setWeight(totalQty * 500);
            
            List<GhnOrderCreateRequestDTO.GhnItemDTO> ghnItems = new ArrayList<>();
            if (items != null) {
                for (HoaDonChiTiet ct : items) {
                    GhnOrderCreateRequestDTO.GhnItemDTO item = new GhnOrderCreateRequestDTO.GhnItemDTO();
                    item.setName(ct.getSanPhamChiTiet() != null ? ct.getSanPhamChiTiet().getSanPham().getTenSanPham() : "Sản phẩm trả");
                    item.setQuantity(ct.getSoLuong());
                    item.setPrice(ct.getDonGia().intValue());
                    ghnItems.add(item);
                }
            }
            req.setItems(ghnItems);
            
            String url = ghnConfig.getBaseUrl() + API_CREATE;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", tokenStr);
            headers.set("ShopId", shopIdStr);
            
            HttpEntity<GhnOrderCreateRequestDTO> request = new HttpEntity<>(req, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("order_code") != null) {
                    String orderCode = (String) data.get("order_code");
                    log.info("GHN: Created return order {} for HoaDon #{}", orderCode, hoaDon.getId());
                    return orderCode;
                }
            }
            String msg = response.get("message") != null ? response.get("message").toString() : "Không rõ lý do";
            throw new RuntimeException("Đơn vị vận chuyển GHN từ chối tạo đơn thu hồi: " + msg);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            try {
                Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
                String msg = (String) response.get("message");
                if (msg != null && !msg.isBlank()) {
                    throw new RuntimeException("Đơn vị vận chuyển GHN từ chối tạo đơn thu hồi: " + msg);
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ignored) {}
            log.error("Lỗi kết nối GHN API (HTTP {}): {}", e.getStatusCode(), body);
            throw new RuntimeException("Không thể kết nối đơn vị vận chuyển GHN. Vui lòng thử lại sau.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("GHN createReturnShippingOrder API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo vận đơn GHN thu hồi. Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Tạo vận đơn GHN giao sản phẩm đổi mới (Shop -> Khách) với COD = 0.
     * CHỈ gọi GHN API và trả về orderCode, KHÔNG tự MERGE TichHopVanChuyen hay update DB.
     */
    public String createExchangeShippingOrderOrThrow(HoaDon hoaDon, List<HoaDonChiTiet> items,
                                                     Integer toDistrictId, String toWardCode) throws Exception {
        if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
            SoDiaChi dc = getHoaDonDiaChiSafe(hoaDon);
            if (dc != null && dc.getDistrictId() != null && dc.getWardCode() != null) {
                toDistrictId = dc.getDistrictId();
                toWardCode = dc.getWardCode();
            } else if (hoaDon.getKhachHang() != null) {
                try {
                    List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(hoaDon.getKhachHang().getId());
                    if (addresses != null) {
                        for (SoDiaChi sdc : addresses) {
                            if (sdc.getSdtNguoiNhan() != null 
                                    && sdc.getSdtNguoiNhan().trim().equalsIgnoreCase(hoaDon.getSdtNhan() != null ? hoaDon.getSdtNhan().trim() : "")
                                    && sdc.getGhnDistrictId() != null 
                                    && sdc.getGhnWardCode() != null) {
                                toDistrictId = sdc.getGhnDistrictId();
                                toWardCode = sdc.getGhnWardCode();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve district/ward from SoDiaChi for GHN exchange order: {}", e.getMessage());
                }
            }
        }
        if (toDistrictId == null) {
            toDistrictId = ghnConfig.getFromDistrictId();
        }
        if (toWardCode == null || toWardCode.isBlank()) {
            toWardCode = ghnConfig.getFromWardCode();
        }
        String shopIdStr = getGhnShopId();
        String tokenStr = getGhnToken();

        GhnOrderCreateRequestDTO req = new GhnOrderCreateRequestDTO();
        req.setClient_order_code("EXCHANGE-HD-" + hoaDon.getId());
        req.setFrom_name("SmashVN Shop");
        req.setFrom_phone("0835420088");
        
        Map<String, Object> shopDetails = getShopDetails(shopIdStr, tokenStr);
        req.setFrom_address(ghnConfig.getFromAddress());
        if (shopDetails != null) {
            req.setFrom_district_id(shopDetails.get("district_id") != null ? ((Number) shopDetails.get("district_id")).intValue() : ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(shopDetails.get("ward_code") != null ? shopDetails.get("ward_code").toString() : ghnConfig.getFromWardCode());
        } else {
            req.setFrom_district_id(ghnConfig.getFromDistrictId());
            req.setFrom_ward_code(ghnConfig.getFromWardCode());
        }

        String receiverName = null;
        if (hoaDon.getKhachHang() != null) {
            try {
                List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(hoaDon.getKhachHang().getId());
                if (addresses != null) {
                    for (SoDiaChi sdc : addresses) {
                        if (sdc.getSdtNguoiNhan() != null && sdc.getSdtNguoiNhan().trim().equals(hoaDon.getSdtNhan().trim())) {
                            receiverName = (sdc.getHoNguoiNhan() + " " + sdc.getTenNguoiNhan()).trim();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to lookup receiver name from SoDiaChi: {}", e.getMessage());
            }
            if (receiverName == null || receiverName.isEmpty()) {
                receiverName = (hoaDon.getKhachHang().getHoKh() + " " + hoaDon.getKhachHang().getTenKh()).trim();
            }
        }
        if (receiverName == null || receiverName.isEmpty()) {
            receiverName = "Khách hàng";
        }

        req.setTo_name(receiverName);
        req.setTo_phone(hoaDon.getSdtNhan());
        req.setTo_address(hoaDon.getDiaChiNhan());
        req.setTo_district_id(toDistrictId);
        req.setTo_ward_code(toWardCode);
        GhnServiceInfo serviceInfo = resolveAvailableService(shopIdStr, req.getFrom_district_id(), toDistrictId);
        req.setService_id(serviceInfo.getServiceId());
        req.setService_type_id(serviceInfo.getServiceTypeId());

        // COD = 0 CHO ĐƠN ĐỔI HÀNG THAY THẾ
        req.setCod_amount(0);
        req.setInsurance_value(hoaDon.getTongTien() != null ? hoaDon.getTongTien().intValue() : 0);
        req.setNote("Đổi hàng thay thế cho đơn #" + hoaDon.getId() + ". " + (hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : ""));

        int totalQty = items.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum();
        req.setWeight(totalQty * 500);
        req.setHeight(Math.min(150, 10 + (totalQty - 1) * 2));

        List<GhnOrderCreateRequestDTO.GhnItemDTO> ghnItems = new ArrayList<>();
        for (HoaDonChiTiet ct : items) {
            GhnOrderCreateRequestDTO.GhnItemDTO item = new GhnOrderCreateRequestDTO.GhnItemDTO();
            String classification = ct.getSanPhamChiTiet().getPhanLoaiHienThi();
            String detailName = ct.getSanPhamChiTiet().getSanPham().getTenSanPham() + (!classification.isEmpty() ? " [" + classification + "]" : "");
            item.setName(detailName);
            item.setCode("SP-" + ct.getSanPhamChiTiet().getId());
            item.setQuantity(ct.getSoLuong());
            item.setPrice(ct.getDonGia().intValue());
            ghnItems.add(item);
        }
        req.setItems(ghnItems);

        String url = ghnConfig.getBaseUrl() + API_CREATE;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", tokenStr);
        headers.set("ShopId", shopIdStr);

        HttpEntity<GhnOrderCreateRequestDTO> request = new HttpEntity<>(req, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String orderCode = (String) data.get("order_code");
                    log.info("GHN: Created exchange shipping order {} for HoaDon #{}", orderCode, hoaDon.getId());
                    return orderCode;
                }
            }
            String msg = response.get("message") != null ? response.get("message").toString() : "Không rõ lý do";
            throw new RuntimeException("Đơn vị vận chuyển GHN từ chối tạo đơn giao hàng đổi: " + msg);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.error("Lỗi kết nối GHN API (HTTP {}): {}", e.getStatusCode(), body);
            throw new RuntimeException("Không thể kết nối đơn vị vận chuyển GHN. Vui lòng thử lại sau.");
        }
    }

    /**
     * Tra cứu trạng thái vận đơn GHN
     *
     * @param orderCode mã vận đơn GHN
     * @return Map chứa status, estimated_delivery_time, logs, v.v.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> trackOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank() || orderCode.startsWith("DEMO-GHN-")) {
            log.debug("GHN trackOrder skipped for Demo orderCode: {}", orderCode);
            return null;
        }
        try {
            Map<String, Object> body = Map.of("order_code", orderCode);
            String url = ghnConfig.getBaseUrl() + API_DETAIL;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                return (Map<String, Object>) response.get("data");
            }
            log.warn("GHN trackOrder failed: code={}, message={}", code, response.get("message"));
        } catch (Exception e) {
            log.error("GHN trackOrder error for {}: {}", orderCode, e.getMessage());
        }
        return null;
    }

    /**
     * Lấy danh sách Tỉnh/Thành phố
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProvinces() {
        return getProvinces(false);
    }

    public List<Map<String, Object>> getProvincesOrThrow() {
        return getProvinces(true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getProvinces(boolean failOnLookupError) {
        try {
            String url = ghnConfig.getBaseUrl() + API_PROVINCE;
            HttpEntity<Void> request = new HttpEntity<>(buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                Object data = response.get("data");
                if (data instanceof List<?>) {
                    return (List<Map<String, Object>>) data;
                }
                throw new IllegalStateException("GHN trả về dữ liệu Tỉnh/Thành phố không hợp lệ.");
            }
            throw new IllegalStateException("GHN từ chối tra cứu Tỉnh/Thành phố: " + response.get("message"));
        } catch (Exception e) {
            log.error("GHN getProvinces error: {}", e.getMessage());
            if (failOnLookupError) {
                throw addressLookupFailure("Tỉnh/Thành phố", e);
            }
        }
        return List.of();
    }

    /**
     * Lấy danh sách Quận/Huyện theo tỉnh
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDistricts(Integer provinceId) {
        return getDistricts(provinceId, false);
    }

    public List<Map<String, Object>> getDistrictsOrThrow(Integer provinceId) {
        return getDistricts(provinceId, true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDistricts(Integer provinceId, boolean failOnLookupError) {
        try {
            Map<String, Object> body = Map.of("province_id", provinceId);
            String url = ghnConfig.getBaseUrl() + API_DISTRICT;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                Object data = response.get("data");
                if (data instanceof List<?>) {
                    return (List<Map<String, Object>>) data;
                }
                throw new IllegalStateException("GHN trả về dữ liệu Quận/Huyện không hợp lệ.");
            }
            throw new IllegalStateException("GHN từ chối tra cứu Quận/Huyện: " + response.get("message"));
        } catch (Exception e) {
            log.error("GHN getDistricts error: {}", e.getMessage());
            if (failOnLookupError) {
                throw addressLookupFailure("Quận/Huyện", e);
            }
        }
        return List.of();
    }

    /**
     * Lấy danh sách Phường/Xã theo quận/huyện
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getWards(Integer districtId) {
        return getWards(districtId, false);
    }

    public List<Map<String, Object>> getWardsOrThrow(Integer districtId) {
        return getWards(districtId, true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getWards(Integer districtId, boolean failOnLookupError) {
        try {
            Map<String, Object> body = Map.of("district_id", districtId);
            String url = ghnConfig.getBaseUrl() + API_WARD;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildSimpleHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
            Integer code = (Integer) response.get("code");
            if (code != null && code == 200) {
                Object data = response.get("data");
                if (data instanceof List<?>) {
                    return (List<Map<String, Object>>) data;
                }
                throw new IllegalStateException("GHN trả về dữ liệu Phường/Xã không hợp lệ.");
            }
            throw new IllegalStateException("GHN từ chối tra cứu Phường/Xã: " + response.get("message"));
        } catch (Exception e) {
            log.error("GHN getWards error: {}", e.getMessage());
            if (failOnLookupError) {
                throw addressLookupFailure("Phường/Xã", e);
            }
        }
        return List.of();
    }

    private RuntimeException addressLookupFailure(String level, Exception cause) {
        if (cause instanceof org.springframework.web.client.RestClientException restClientException) {
            return restClientException;
        }
        if (cause instanceof IllegalStateException illegalStateException) {
            return illegalStateException;
        }
        return new IllegalStateException("Không thể tra cứu danh mục " + level + " từ GHN.", cause);
    }

    /**
     * Hủy đơn hàng / vận đơn trên hệ thống GHN.
     * Hỗ trợ xử lý an toàn cho cả mã DEMO / Fallback và mã GHN thật.
     *
     * @param ghnOrderCode Mã vận đơn GHN cần hủy
     * @return true nếu hủy thành công hoặc mã demo, false nếu có lỗi
     */
    public boolean cancelOrder(String ghnOrderCode) {
        if (ghnOrderCode == null || ghnOrderCode.trim().isEmpty()) {
            return false;
        }
        String cleanCode = ghnOrderCode.trim();
        log.info("[GHN_CANCEL] Đang yêu cầu hủy vận đơn GHN: {}", cleanCode);

        // Trường hợp đơn DEMO / Fallback nội bộ
        if (cleanCode.startsWith("DEMO-") || cleanCode.startsWith("FALLBACK-")) {
            try {
                jdbcTemplate.update("UPDATE TichHopVanChuyen SET trang_thai = 'cancel' WHERE ma_van_don = ?", cleanCode);
            } catch (Exception dbEx) {
                log.warn("[GHN_CANCEL] Lỗi cập nhật trạng thái bảng TichHopVanChuyen cho mã {}: {}", cleanCode, dbEx.getMessage());
            }
            log.info("[GHN_CANCEL] Đã đánh dấu hủy vận đơn nội bộ: {}", cleanCode);
            return true;
        }

        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/switch-status/cancel";
            Map<String, Object> body = Map.of("order_codes", List.of(cleanCode));
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            if (responseEntity.getBody() != null) {
                Map<String, Object> response = objectMapper.readValue(responseEntity.getBody(), new TypeReference<>() {});
                Integer code = (Integer) response.get("code");
                if (code != null && code == 200) {
                    try {
                        jdbcTemplate.update("UPDATE TichHopVanChuyen SET trang_thai = 'cancel' WHERE ma_van_don = ?", cleanCode);
                    } catch (Exception dbEx) {
                        log.warn("[GHN_CANCEL] Lỗi cập nhật trạng thái bảng TichHopVanChuyen: {}", dbEx.getMessage());
                    }
                    log.info("[GHN_CANCEL] Hủy vận đơn GHN thành công trên hệ thống GHN cho mã: {}", cleanCode);
                    return true;
                } else {
                    log.warn("[GHN_CANCEL] GHN trả về phản hồi khi hủy vận đơn {}: code={}, message={}",
                            cleanCode, code, response.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("[GHN_CANCEL] Lỗi kết nối khi gọi API hủy vận đơn GHN {}: {}", cleanCode, e.getMessage());
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    private static class GhnServiceInfo {
        private Integer serviceId;
        private Integer serviceTypeId;
    }

    @Data
    @AllArgsConstructor
    public static class GhnAddressMapping {
        private Integer provinceId;
        private Integer districtId;
        private String wardCode;
    }

    @Data
    @AllArgsConstructor
    public static class GhnAddressDetails {
        private Integer provinceId;
        private String provinceName;
        private Integer districtId;
        private String districtName;
        private String wardCode;
        private String wardName;
    }

    public String normalizeString(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT);

        normalized = normalized.replace("đ", "d")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .replaceAll("\\bt\\s*p\\b", " thanh pho ")
                .replaceAll("\\btp\\b", " thanh pho ")
                .replaceAll("\\bq\\b", " quan ")
                .replaceAll("\\bp\\b", " phuong ")
                .replaceAll("\\bh\\b", " huyen ")
                .replaceAll("\\btx\\b", " thi xa ");

        return normalized.replaceAll("\\s+", " ").trim();
    }

    /**
     * Kiểm tra lại toàn bộ quan hệ Province -> District -> Ward ở backend và
     * lấy tên chuẩn từ danh mục. Không tin các hidden text do trình duyệt gửi.
     */
    public GhnAddressDetails validateSelectedAddress(Integer provinceId, Integer districtId, String wardCode) {
        if (provinceId == null || provinceId <= 0 || districtId == null || districtId <= 0
                || wardCode == null || wardCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã.");
        }

        Map<String, Object> province = findByNumber(
                getProvincesOrThrow(), "ProvinceID", provinceId);
        if (province == null) {
            throw new IllegalArgumentException("Tỉnh/Thành phố đã chọn không hợp lệ.");
        }

        Map<String, Object> district = findByNumber(
                getDistrictsOrThrow(provinceId), "DistrictID", districtId);
        if (district == null) {
            throw new IllegalArgumentException("Quận/Huyện không thuộc Tỉnh/Thành phố đã chọn.");
        }

        Map<String, Object> ward = getWardsOrThrow(districtId).stream()
                .filter(item -> wardCode.trim().equals(String.valueOf(item.get("WardCode"))))
                .findFirst()
                .orElse(null);
        if (ward == null) {
            throw new IllegalArgumentException("Phường/Xã không thuộc Quận/Huyện đã chọn.");
        }

        return detailsOf(province, district, ward);
    }

    /**
     * Ưu tiên match theo từng cấp hành chính. Mỗi cấp chỉ dùng so sánh exact sau
     * chuẩn hóa (kể cả NameExtension), không dùng contains. Khi provider thiếu
     * District, Ward chỉ được dùng để suy ra hierarchy nếu duy nhất trong
     * Province đã match.
     */
    public GhnAddressDetails resolveAdministrativeAddress(
            List<String> provinceCandidates,
            List<String> districtCandidates,
            List<String> wardCandidates) {
        Map<String, Object> province = findUniqueAdministrativeMatch(
                getProvincesOrThrow(), "ProvinceName", "ProvinceID", provinceCandidates);
        if (province == null) {
            return emptyAddressDetails();
        }

        Integer provinceId = intValue(province.get("ProvinceID"));
        List<Map<String, Object>> districts;
        Map<String, Object> district;
        try {
            districts = getDistrictsOrThrow(provinceId);
            district = findUniqueAdministrativeMatch(
                    districts, "DistrictName", "DistrictID", districtCandidates);
        } catch (Exception exception) {
            log.warn("Unable to continue automatic address resolution after province {}: {}",
                    provinceId, exception.getMessage());
            return new GhnAddressDetails(provinceId, stringValue(province.get("ProvinceName")),
                    null, null, null, null);
        }
        if (district == null) {
            try {
                GhnAddressDetails inferred = inferUniqueDistrictFromWard(
                        province, districts, wardCandidates);
                if (inferred != null) return inferred;
            } catch (Exception exception) {
                log.warn("Unable to infer district from ward candidates in province {}: {}",
                        provinceId, exception.getMessage());
            }
            return new GhnAddressDetails(provinceId, stringValue(province.get("ProvinceName")),
                    null, null, null, null);
        }

        Integer districtId = intValue(district.get("DistrictID"));
        Map<String, Object> ward;
        try {
            ward = findUniqueAdministrativeMatch(
                    getWardsOrThrow(districtId), "WardName", "WardCode", wardCandidates);
        } catch (Exception exception) {
            log.warn("Unable to continue automatic address resolution after district {}: {}",
                    districtId, exception.getMessage());
            return new GhnAddressDetails(provinceId, stringValue(province.get("ProvinceName")),
                    districtId, stringValue(district.get("DistrictName")), null, null);
        }
        if (ward == null) {
            return new GhnAddressDetails(provinceId, stringValue(province.get("ProvinceName")),
                    districtId, stringValue(district.get("DistrictName")), null, null);
        }

        return detailsOf(province, district, ward);
    }

    /**
     * Fallback cho provider chỉ trả Province và Ward. Quét toàn bộ Ward trong
     * Province đã match và chỉ suy ra hierarchy khi đúng một cặp District/Ward
     * khớp exact sau normalize/NameExtension. Bất kỳ lỗi lookup nào được ném ra
     * để caller giữ partial resolution ở cấp Province.
     */
    private GhnAddressDetails inferUniqueDistrictFromWard(
            Map<String, Object> province,
            List<Map<String, Object>> districts,
            List<String> wardCandidates) {
        if (districts == null || districts.isEmpty()
                || wardCandidates == null || wardCandidates.isEmpty()) {
            return null;
        }

        Map<String, WardDistrictMatch> matches = new LinkedHashMap<>();
        for (Map<String, Object> candidateDistrict : districts) {
            Integer districtId = intValue(candidateDistrict.get("DistrictID"));
            if (districtId == null) continue;

            for (Map<String, Object> candidateWard : getWardsOrThrow(districtId)) {
                String wardCode = stringValue(candidateWard.get("WardCode"));
                if (wardCode == null || wardCode.isBlank()
                        || !matchesAnyAdministrativeCandidate(
                                candidateWard, "WardName", wardCandidates)) {
                    continue;
                }
                matches.putIfAbsent(
                        districtId + ":" + wardCode,
                        new WardDistrictMatch(candidateDistrict, candidateWard));
            }
        }

        if (matches.size() != 1) {
            if (matches.size() > 1) {
                log.info("Ward-to-district fallback remains ambiguous in province {}: matchCount={}",
                        province.get("ProvinceID"), matches.size());
            }
            return null;
        }

        WardDistrictMatch uniqueMatch = matches.values().iterator().next();
        log.info("Inferred GHN district {} and ward {} from unique ward match in province {}",
                uniqueMatch.district().get("DistrictID"),
                uniqueMatch.ward().get("WardCode"),
                province.get("ProvinceID"));
        return detailsOf(province, uniqueMatch.district(), uniqueMatch.ward());
    }

    private boolean matchesAnyAdministrativeCandidate(
            Map<String, Object> item, String nameKey, List<String> candidates) {
        for (String candidate : candidates) {
            for (String alias : administrativeNames(item, nameKey)) {
                if (administrativeMatchScore(candidate, alias) > 0) return true;
            }
        }
        return false;
    }

    private record WardDistrictMatch(
            Map<String, Object> district,
            Map<String, Object> ward) {
    }

    private GhnAddressDetails detailsOf(Map<String, Object> province,
            Map<String, Object> district, Map<String, Object> ward) {
        return new GhnAddressDetails(
                intValue(province.get("ProvinceID")), stringValue(province.get("ProvinceName")),
                intValue(district.get("DistrictID")), stringValue(district.get("DistrictName")),
                stringValue(ward.get("WardCode")), stringValue(ward.get("WardName")));
    }

    private GhnAddressDetails emptyAddressDetails() {
        return new GhnAddressDetails(null, null, null, null, null, null);
    }

    private Map<String, Object> findByNumber(List<Map<String, Object>> items, String key, Integer expected) {
        return items.stream()
                .filter(item -> Objects.equals(expected, intValue(item.get(key))))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> findUniqueAdministrativeMatch(List<Map<String, Object>> items,
            String nameKey, String idKey, List<String> candidates) {
        if (items == null || items.isEmpty() || candidates == null || candidates.isEmpty()) {
            return null;
        }

        // Provider candidates are ordered from the most authoritative field to
        // progressively weaker fallbacks. Resolve one candidate at a time so a
        // neighbourhood name cannot invalidate a unique administrative ward.
        for (String candidate : candidates) {
            Map<String, Object> bestMatch = null;
            int bestScore = 0;
            boolean ambiguous = false;

            for (Map<String, Object> item : items) {
                int itemScore = 0;
                for (String alias : administrativeNames(item, nameKey)) {
                    itemScore = Math.max(itemScore, administrativeMatchScore(candidate, alias));
                }

                if (itemScore > bestScore) {
                    bestScore = itemScore;
                    bestMatch = item;
                    ambiguous = false;
                } else if (itemScore > 0 && itemScore == bestScore && bestMatch != null
                        && !Objects.equals(String.valueOf(item.get(idKey)), String.valueOf(bestMatch.get(idKey)))) {
                    ambiguous = true;
                }
            }

            if (bestScore > 0 && !ambiguous) return bestMatch;
        }

        return null;
    }

    private int administrativeMatchScore(String left, String right) {
        String normalizedLeft = normalizeString(left);
        String normalizedRight = normalizeString(right);
        if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) return 0;
        if (normalizedLeft.equals(normalizedRight)) return 100;

        String coreLeft = stripAdministrativePrefix(normalizedLeft);
        String coreRight = stripAdministrativePrefix(normalizedRight);
        return !coreLeft.isEmpty() && coreLeft.equals(coreRight) ? 90 : 0;
    }

    private String stripAdministrativePrefix(String value) {
        return value.replaceFirst(
                "^(tinh|thanh pho|quan|huyen|thi xa|phuong|xa|thi tran)\\s+", "").trim();
    }

    private Set<String> administrativeNames(Map<String, Object> item, String primaryNameKey) {
        Set<String> names = new LinkedHashSet<>();
        addAdministrativeName(names, item.get(primaryNameKey));
        Object extensions = item.get("NameExtension");
        if (extensions instanceof Collection<?> collection) {
            collection.forEach(value -> addAdministrativeName(names, value));
        } else {
            addAdministrativeName(names, extensions);
        }
        return names;
    }

    private void addAdministrativeName(Set<String> names, Object value) {
        if (value == null) return;
        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) names.add(text);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public GhnAddressMapping resolveGhnAddress(SoDiaChi dc) {
        return resolveGhnAddress(dc, false);
    }

    /**
     * Phân giải địa chỉ cho luồng tạo vận đơn Admin. Lỗi gọi API/dữ liệu GHN được ném ra
     * để không bị hiểu nhầm thành địa chỉ không được Sandbox hỗ trợ.
     */
    public GhnAddressMapping resolveGhnAddressOrThrow(SoDiaChi dc) {
        return resolveGhnAddress(dc, true);
    }

    private GhnAddressMapping resolveGhnAddress(SoDiaChi dc, boolean failOnLookupError) {
        if (dc == null) return null;

        // Bypass fuzzy matching only when all GHN fields are fully present and valid
        if (dc.getProvinceId() != null && dc.getDistrictId() != null && 
            dc.getWardCode() != null && !dc.getWardCode().trim().isEmpty()) {
            log.info("Using pre-saved GHN address mapping for address ID {}: provinceId={}, districtId={}, wardCode={}",
                     dc.getId(), dc.getProvinceId(), dc.getDistrictId(), dc.getWardCode());
            return new GhnAddressMapping(dc.getProvinceId(), dc.getDistrictId(), dc.getWardCode().trim());
        }

        String userProvince = dc.getTinhThanh();
        String userDistrict = dc.getDiaChiCuThe();
        String userWard = dc.getDiaChiCuThe();

        // 1. Fetch provinces and match dc.getTinhThanh() or dc.getDiaChiCuThe()
        List<Map<String, Object>> provinces = getProvinces(failOnLookupError);
        String targetProvince = normalizeString(userProvince);
        Integer provinceId = null;
        Map<String, Object> matchedProvince = null;

        // Pass 1: Try to match province using dc.getTinhThanh()
        for (Map<String, Object> p : provinces) {
            String name = normalizeString((String) p.get("ProvinceName"));
            if (!name.isEmpty() && (name.contains(targetProvince) || targetProvince.contains(name))) {
                provinceId = (Integer) p.get("ProvinceID");
                matchedProvince = p;
                break;
            }
        }

        // Pass 2: Fallback to match province using dc.getDiaChiCuThe()
        if (provinceId == null) {
            String streetAddr = normalizeString(dc.getDiaChiCuThe());
            for (Map<String, Object> p : provinces) {
                String name = normalizeString((String) p.get("ProvinceName"));
                String cleanProvName = name.replaceAll("^(tinh|thanh pho|tp)\\s+", "").trim();
                if (!cleanProvName.isEmpty() && streetAddr.contains(cleanProvName)) {
                    provinceId = (Integer) p.get("ProvinceID");
                    matchedProvince = p;
                    break;
                }
            }
        }

        if (provinceId == null) {
            log.warn("Unable to resolve GHN province from address: {}", dc.getDiaChiCuThe());
            return null;
        }

        log.debug("GHN Province matched: {} -> {}",
                userProvince,
                (String) matchedProvince.get("ProvinceName"));

        String cleanProvinceName = normalizeString((String) matchedProvince.get("ProvinceName"))
                .replaceAll("^(tinh|thanh pho|tp)\\s+", "").trim();

        // 2. Fetch districts and match dc.getDiaChiCuThe() first, then fallback to dc.getThanhPho()
        List<Map<String, Object>> districts = getDistricts(provinceId, failOnLookupError);
        String streetAddress = normalizeString(dc.getDiaChiCuThe());
        String fallbackDistrict = normalizeString(dc.getThanhPho());
        Integer districtId = null;
        Map<String, Object> matchedDistrict = null;

        Map<Map<String, Object>, String> districtCleanNames = new java.util.HashMap<>();
        Map<Map<String, Object>, String> districtNormNames = new java.util.HashMap<>();
        for (Map<String, Object> d : districts) {
            String rawDistrictName = (String) d.get("DistrictName");
            if (rawDistrictName != null) {
                String normDistrictName = normalizeString(rawDistrictName);
                String cleanDistrictName = normDistrictName.replaceAll("^(quan|huyen|thi xa|thanh pho|tp)\\s+", "").trim();
                districtCleanNames.put(d, cleanDistrictName);
                districtNormNames.put(d, normDistrictName);
            }
        }

        List<Map<String, Object>> sortedDistricts = new java.util.ArrayList<>(districts);
        sortedDistricts.sort((d1, d2) -> {
            String name1 = districtCleanNames.getOrDefault(d1, "");
            String name2 = districtCleanNames.getOrDefault(d2, "");
            return Integer.compare(name2.length(), name1.length()); // Descending
        });

        // Pass 1: Match districts whose clean name is NOT equal to the clean province name
        for (Map<String, Object> d : sortedDistricts) {
            String cleanDistName = districtCleanNames.get(d);
            if (cleanDistName == null || cleanDistName.isEmpty() || cleanDistName.equals(cleanProvinceName)) {
                continue;
            }
            String normDistName = districtNormNames.get(d);
            if (cleanDistName.matches("^\\d+$")) {
                if (streetAddress.contains(normDistName)) {
                    districtId = (Integer) d.get("DistrictID");
                    matchedDistrict = d;
                    break;
                }
            } else {
                if (streetAddress.contains(cleanDistName)) {
                    districtId = (Integer) d.get("DistrictID");
                    matchedDistrict = d;
                    break;
                }
            }
        }

        // Pass 2: Match district whose clean name is equal to the clean province name (e.g. Thành phố Thái Nguyên in Thái Nguyên)
        if (districtId == null) {
            for (Map<String, Object> d : sortedDistricts) {
                String cleanDistName = districtCleanNames.get(d);
                if (cleanDistName != null && !cleanDistName.isEmpty() && cleanDistName.equals(cleanProvinceName)) {
                    String normDistName = districtNormNames.get(d);
                    if (cleanDistName.matches("^\\d+$")) {
                        if (streetAddress.contains(normDistName)) {
                            districtId = (Integer) d.get("DistrictID");
                            matchedDistrict = d;
                            break;
                        }
                    } else {
                        if (streetAddress.contains(cleanDistName)) {
                            districtId = (Integer) d.get("DistrictID");
                            matchedDistrict = d;
                            break;
                        }
                    }
                }
            }
        }

        // Pass 3: Fallback to old method (match against dc.getThanhPho())
        if (districtId == null) {
            String cleanFallback = fallbackDistrict.replaceAll("^(tinh|thanh pho|tp)\\s+", "").trim();
            for (Map<String, Object> d : sortedDistricts) {
                String cleanDistName = districtCleanNames.get(d);
                if (cleanDistName != null && !cleanDistName.isEmpty() && 
                    (cleanDistName.contains(cleanFallback) || cleanFallback.contains(cleanDistName))) {
                    districtId = (Integer) d.get("DistrictID");
                    matchedDistrict = d;
                    break;
                }
            }
        }

        // Pass 4: If district is not explicitly written in text, search across all districts of this province to match Ward name in street address
        String preMatchedWardCode = null;
        if (districtId == null) {
            for (Map<String, Object> d : districts) {
                Integer dId = (Integer) d.get("DistrictID");
                if (dId == null) continue;
                List<Map<String, Object>> candidateWards = getWards(dId, false);
                for (Map<String, Object> w : candidateWards) {
                    String rawWardName = (String) w.get("WardName");
                    if (rawWardName == null) continue;
                    String normWName = normalizeString(rawWardName);
                    String cleanWName = normWName.replaceAll("^(phuong|xa|thi tran)\\s+", "").trim();
                    if (!cleanWName.isEmpty() && cleanWName.length() >= 3 && streetAddress.contains(cleanWName)) {
                        districtId = dId;
                        matchedDistrict = d;
                        preMatchedWardCode = (String) w.get("WardCode");
                        log.info("Resolved District ({}) and Ward ({}) by reverse ward search from address: {}",
                                d.get("DistrictName"), rawWardName, dc.getDiaChiCuThe());
                        break;
                    }
                }
                if (districtId != null) break;
            }
        }

        if (districtId == null) {
            log.warn("Unable to resolve GHN district from address: {}",
                    dc.getDiaChiCuThe());
            return null;
        }

        log.debug("GHN District matched: {} -> {}",
                userDistrict,
                (String) matchedDistrict.get("DistrictName"));

        // 3. Fetch wards and match within dc.getDiaChiCuThe() first, then other fields
        String wardCode = preMatchedWardCode;
        Map<String, Object> matchedWard = null;

        if (wardCode == null) {
            List<Map<String, Object>> wards = getWards(districtId, failOnLookupError);
            String targetStreet = streetAddress; // dc.getDiaChiCuThe() normalized
            String targetOtherFields = normalizeString(dc.getTinhThanh() + " " + dc.getThanhPho());

        Map<Map<String, Object>, String> wardCleanNames = new java.util.HashMap<>();
        Map<Map<String, Object>, String> wardNormNames = new java.util.HashMap<>();
        for (Map<String, Object> w : wards) {
            String rawWardName = (String) w.get("WardName");
            if (rawWardName != null) {
                String normWardName = normalizeString(rawWardName);
                String cleanWardName = normWardName.replaceAll("^(phuong|xa|thi tran)\\s+", "").trim();
                wardCleanNames.put(w, cleanWardName);
                wardNormNames.put(w, normWardName);
            }
        }

        List<Map<String, Object>> sortedWards = new java.util.ArrayList<>(wards);
        sortedWards.sort((w1, w2) -> {
            String name1 = wardCleanNames.getOrDefault(w1, "");
            String name2 = wardCleanNames.getOrDefault(w2, "");
            return Integer.compare(name2.length(), name1.length()); // Descending
        });

        // Pass 1: Match ward inside dc.getDiaChiCuThe()
        for (Map<String, Object> w : sortedWards) {
            String cleanWardName = wardCleanNames.get(w);
            if (cleanWardName == null || cleanWardName.isEmpty()) continue;
            
            String normWardName = wardNormNames.get(w);
            if (cleanWardName.matches("^\\d+$")) {
                if (targetStreet.contains(normWardName)) {
                    wardCode = (String) w.get("WardCode");
                    matchedWard = w;
                    break;
                }
            } else {
                if (targetStreet.contains(cleanWardName)) {
                    wardCode = (String) w.get("WardCode");
                    matchedWard = w;
                    break;
                }
            }
        }

        // Pass 2: Fallback to match ward inside TinhThanh / ThanhPho
        if (wardCode == null) {
            for (Map<String, Object> w : sortedWards) {
                String cleanWardName = wardCleanNames.get(w);
                if (cleanWardName == null || cleanWardName.isEmpty()) continue;
                
                String normWardName = wardNormNames.get(w);
                if (cleanWardName.matches("^\\d+$")) {
                    if (targetOtherFields.contains(normWardName)) {
                        wardCode = (String) w.get("WardCode");
                        matchedWard = w;
                        break;
                    }
                } else {
                    if (targetOtherFields.contains(cleanWardName)) {
                        wardCode = (String) w.get("WardCode");
                        matchedWard = w;
                        break;
                    }
                }
            }
        }
        }

        if (wardCode == null) {
            log.warn("Unable to resolve GHN ward from address: {}",
                    dc.getDiaChiCuThe());
            return null;
        }

        log.debug("GHN Ward matched: {} -> {}",
                userWard,
                (String) matchedWard.get("WardName"));

        // Backfill GHN IDs to SoDiaChi to avoid future fuzzy match/API calls
        if (dc.getId() != null) {
            dc.setProvinceId(provinceId);
            dc.setDistrictId(districtId);
            dc.setWardCode(wardCode);
            dc.setProvinceName(matchedProvince != null && matchedProvince.get("ProvinceName") != null ? matchedProvince.get("ProvinceName").toString() : null);
            dc.setDistrictName(matchedDistrict != null && matchedDistrict.get("DistrictName") != null ? matchedDistrict.get("DistrictName").toString() : null);
            dc.setWardName(matchedWard != null && matchedWard.get("WardName") != null ? matchedWard.get("WardName").toString() : null);
            try {
                soDiaChiRepository.save(dc);
                log.info("[GHN_BACKFILL] Successfully backfilled GHN IDs to SoDiaChi ID {}", dc.getId());
            } catch (Exception e) {
                log.warn("[GHN_BACKFILL] Failed to backfill GHN IDs for SoDiaChi ID {}: {}", dc.getId(), e.getMessage());
            }
        }

        return new GhnAddressMapping(provinceId, districtId, wardCode);
    }
}

