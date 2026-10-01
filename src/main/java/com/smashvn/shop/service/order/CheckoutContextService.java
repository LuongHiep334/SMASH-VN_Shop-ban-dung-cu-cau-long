package com.smashvn.shop.service.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smashvn.shop.dto.order.CheckoutContext;
import com.smashvn.shop.dto.order.CheckoutContextStatus;
import com.smashvn.shop.dto.order.CheckoutItemContext;
import com.smashvn.shop.dto.order.CheckoutSource;

import jakarta.servlet.http.HttpSession;
import com.smashvn.shop.dto.order.FullCartCheckoutResult;
import com.smashvn.shop.dto.order.InvalidCartItemView;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.product.ProductAvailabilityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutContextService {

    private final GuestCartService guestCartService;
    private final GioHangService gioHangService;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final ProductAvailabilityService productAvailabilityService;


    public static final String SESSION_CONTEXTS_KEY = "checkoutContexts";
    public static final int MAX_CONTEXTS_PER_SESSION = 10;
    public static final int CONTEXT_TTL_MINUTES = 30;

    @SuppressWarnings("unchecked")
    private Map<String, CheckoutContext> getContextMap(HttpSession session) {
        synchronized (session) {
            Map<String, CheckoutContext> map = (Map<String, CheckoutContext>) session.getAttribute(SESSION_CONTEXTS_KEY);
            if (map == null) {
                map = new HashMap<>();
                session.setAttribute(SESSION_CONTEXTS_KEY, map);
            }
            return map;
        }
    }

    private void cleanupExcessContexts(Map<String, CheckoutContext> map) {
        if (map.size() <= MAX_CONTEXTS_PER_SESSION) {
            return;
        }
        List<String> toRemove = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, CheckoutContext> entry : map.entrySet()) {
            CheckoutContext ctx = entry.getValue();
            if (ctx.isExpired() && ctx.getStatus() != CheckoutContextStatus.PROCESSING) {
                toRemove.add(entry.getKey());
            }
        }
        for (String k : toRemove) {
            map.remove(k);
        }
        if (map.size() > MAX_CONTEXTS_PER_SESSION) {
            String oldestKey = null;
            LocalDateTime oldestTime = LocalDateTime.MAX;
            for (Map.Entry<String, CheckoutContext> entry : map.entrySet()) {
                if (entry.getValue().getStatus() != CheckoutContextStatus.PROCESSING
                        && entry.getValue().getCreatedAt().isBefore(oldestTime)) {
                    oldestTime = entry.getValue().getCreatedAt();
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey != null) {
                map.remove(oldestKey);
            }
        }
    }

    public CheckoutContext createCartContext(HttpSession session, Integer customerId, List<CheckoutItemContext> items) {
        return createNewContext(session, customerId, CheckoutSource.CART, items);
    }

    public CheckoutContext createQuickAddContext(HttpSession session, Integer customerId, Integer idSanPhamChiTiet, Integer soLuongThem, Integer cartItemId) {
        List<CheckoutItemContext> items = new ArrayList<>();
        items.add(CheckoutItemContext.builder()
                .cartItemId(cartItemId)
                .idSanPhamChiTiet(idSanPhamChiTiet)
                .soLuong(soLuongThem)
                .fromCart(cartItemId != null)
                .build());
        return createNewContext(session, customerId, CheckoutSource.QUICK_ADD, items);
    }

    public CheckoutContext createBuyNowContext(HttpSession session, Integer customerId, Integer idSanPhamChiTiet, Integer soLuong) {
        List<CheckoutItemContext> items = new ArrayList<>();
        items.add(CheckoutItemContext.builder()
                .cartItemId(null)
                .idSanPhamChiTiet(idSanPhamChiTiet)
                .soLuong(soLuong)
                .fromCart(false)
                .build());
        return createNewContext(session, customerId, CheckoutSource.BUY_NOW, items);
    }

    private CheckoutContext createNewContext(HttpSession session, Integer customerId, CheckoutSource source, List<CheckoutItemContext> items) {
        Map<String, CheckoutContext> map = getContextMap(session);
        cleanupExcessContexts(map);

        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        CheckoutContext context = CheckoutContext.builder()
                .token(token)
                .source(source)
                .status(CheckoutContextStatus.READY)
                .customerId(customerId)
                .sessionId(session.getId())
                .createdAt(now)
                .expiresAt(now.plusMinutes(CONTEXT_TTL_MINUTES))
                .items(items)
                .build();

        map.put(token, context);
        log.info("[CHECKOUT_CONTEXT] Created token {} for source {} (items count: {})", token, source, items.size());
        return context;
    }

    public CheckoutContext getContext(HttpSession session, String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Map<String, CheckoutContext> map = getContextMap(session);
        CheckoutContext ctx = map.get(token.trim());
        if (ctx == null) {
            return null;
        }
        synchronized (ctx) {
            // An in-flight order must keep its lock, and a committed order must remain recoverable.
            if (ctx.getStatus() == CheckoutContextStatus.READY && ctx.isExpired()) {
                ctx.setStatus(CheckoutContextStatus.EXPIRED);
            }
            if (ctx.getStatus() == CheckoutContextStatus.EXPIRED) {
                return null;
            }
        }
        return ctx;
    }

    public boolean validateOwnership(CheckoutContext context, Integer currentUserId, String currentSessionId) {
        if (context == null) {
            return false;
        }
        if (context.getCustomerId() != null) {
            if (currentUserId != null) {
                return context.getCustomerId().equals(currentUserId);
            }
            return context.getSessionId() != null && context.getSessionId().equals(currentSessionId);
        }
        return context.getSessionId() != null && context.getSessionId().equals(currentSessionId);
    }

    public CheckoutContext promoteGuestContextToAuthenticatedUser(
            String token,
            HttpSession oldSession,
            HttpSession newSession,
            Integer customerId) {

        if (token == null || token.isBlank()) {
            return null;
        }

        String cleanToken = token.trim();
        CheckoutContext context = null;

        if (oldSession != null) {
            try {
                context = getContext(oldSession, cleanToken);
            } catch (Exception e) {
                // oldSession might be invalidated, fallback to newSession
            }
        }
        if (context == null && newSession != null) {
            try {
                context = getContext(newSession, cleanToken);
            } catch (Exception e) {
                // Ignore
            }
        }


        if (context == null) {
            log.warn("[CHECKOUT_CONTEXT] Cannot promote: token {} not found in old or new session", cleanToken);
            return null;
        }

        if (context.isExpired() || context.getStatus() != CheckoutContextStatus.READY) {
            log.warn("[CHECKOUT_CONTEXT] Cannot promote: token {} status is {}", cleanToken, context.getStatus());
            return null;
        }

        if (context.getCustomerId() != null && !context.getCustomerId().equals(customerId)) {
            log.warn("[CHECKOUT_CONTEXT] Cannot promote: token {} already belongs to another customer {}", cleanToken, context.getCustomerId());
            return null;
        }

        context.setCustomerId(customerId);
        if (newSession != null) {
            context.setSessionId(newSession.getId());
            Map<String, CheckoutContext> newMap = getContextMap(newSession);
            newMap.put(cleanToken, context);
        }
        if (oldSession != null) {
            try {
                Map<String, CheckoutContext> oldMap = getContextMap(oldSession);
                oldMap.put(cleanToken, context);
            } catch (Exception e) {
                // Ignore if oldSession is invalidated
            }
        }

        log.info("[CHECKOUT_CONTEXT] Successfully promoted token {} to customerId {} for session {}",
                cleanToken, customerId, newSession != null ? newSession.getId() : "null");

        return context;
    }

    public boolean isActiveAccount(Integer idNguoiDung) {
        if (idNguoiDung == null) {
            return false;
        }
        com.smashvn.shop.entity.TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        return tk != null
                && tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.ACTIVE
                && tk.getMatKhau() != null && !tk.getMatKhau().trim().isEmpty()
                && "hoat_dong".equalsIgnoreCase(tk.getTrangThai());
    }

    public FullCartCheckoutResult createFullCartContext(HttpSession session, Integer idNguoiDung) {
        boolean activeAccount = isActiveAccount(idNguoiDung);

        List<CheckoutItemContext> contextItems = new ArrayList<>();
        List<InvalidCartItemView> invalidItems = new ArrayList<>();
        int totalQuantity = 0;

        if (!activeAccount) {
            List<GuestCartService.GuestCartItem> guestCart = guestCartService.getGuestCartItems(session);
            if (guestCart.isEmpty()) {
                return FullCartCheckoutResult.builder()
                        .trangThai("error")
                        .thongBao("Giỏ hàng của bạn đang trống.")
                        .message("Giỏ hàng của bạn đang trống.")
                        .build();
            }

            for (GuestCartService.GuestCartItem item : guestCart) {
                Integer spctId = item.getIdSanPhamChiTiet();
                Integer reqQty = item.getSoLuong();

                if (spctId == null || reqQty == null || reqQty <= 0) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spctId)
                            .tenSanPham("Sản phẩm chưa xác định")
                            .requestedQuantity(reqQty)
                            .reason("Số lượng sản phẩm không hợp lệ.")
                            .build());
                    continue;
                }

                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(spctId).orElse(null);
                if (spct == null) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spctId)
                            .tenSanPham("Sản phẩm ID " + spctId)
                            .requestedQuantity(reqQty)
                            .reason("Sản phẩm không còn tồn tại trong hệ thống.")
                            .build());
                    continue;
                }

                String tenSp = (spct.getSanPham() != null) ? spct.getSanPham().getTenSanPham() : "Sản phẩm";
                if (!productAvailabilityService.isVariantPublished(spct)) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spctId)
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(spct.getSoLuongTon())
                            .reason("Sản phẩm hoặc phân loại đã ngừng kinh doanh.")
                            .build());
                    continue;
                }

                int tonKho = (spct.getSoLuongTon() != null) ? spct.getSoLuongTon() : 0;
                if (tonKho <= 0) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spctId)
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(0)
                            .reason("Sản phẩm đã hết hàng.")
                            .build());
                    continue;
                }

                if (reqQty > tonKho) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spctId)
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(tonKho)
                            .reason("Số lượng trong giỏ vượt tồn kho")
                            .build());
                    continue;
                }

                contextItems.add(CheckoutItemContext.builder()
                        .cartItemId(null)
                        .idSanPhamChiTiet(spctId)
                        .soLuong(reqQty)
                        .fromCart(true)
                        .build());
                totalQuantity += reqQty;
            }
        } else {
            List<com.smashvn.shop.entity.GioHangChiTiet> dbCartItems = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
            if (dbCartItems.isEmpty()) {
                return FullCartCheckoutResult.builder()
                        .trangThai("error")
                        .thongBao("Giỏ hàng của bạn đang trống.")
                        .message("Giỏ hàng của bạn đang trống.")
                        .build();
            }

            for (com.smashvn.shop.entity.GioHangChiTiet item : dbCartItems) {
                com.smashvn.shop.entity.SanPhamChiTiet spct = item.getSanPhamChiTiet();
                Integer reqQty = item.getSoLuong();

                if (spct == null) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(null)
                            .tenSanPham("Sản phẩm trong giỏ")
                            .requestedQuantity(reqQty)
                            .reason("Sản phẩm không hợp lệ.")
                            .build());
                    continue;
                }

                String tenSp = (spct.getSanPham() != null) ? spct.getSanPham().getTenSanPham() : "Sản phẩm";
                if (!productAvailabilityService.isVariantPublished(spct)) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spct.getId())
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(spct.getSoLuongTon())
                            .reason("Sản phẩm hoặc phân loại đã ngừng kinh doanh.")
                            .build());
                    continue;
                }

                if (reqQty == null || reqQty <= 0) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spct.getId())
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(spct.getSoLuongTon())
                            .reason("Số lượng sản phẩm trong giỏ không hợp lệ.")
                            .build());
                    continue;
                }

                int tonKho = (spct.getSoLuongTon() != null) ? spct.getSoLuongTon() : 0;
                if (tonKho <= 0) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spct.getId())
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(0)
                            .reason("Sản phẩm đã hết hàng.")
                            .build());
                    continue;
                }

                if (reqQty > tonKho) {
                    invalidItems.add(InvalidCartItemView.builder()
                            .idSanPhamChiTiet(spct.getId())
                            .tenSanPham(tenSp)
                            .requestedQuantity(reqQty)
                            .stockQuantity(tonKho)
                            .reason("Số lượng trong giỏ vượt tồn kho")
                            .build());
                    continue;
                }

                contextItems.add(CheckoutItemContext.builder()
                        .cartItemId(item.getId())
                        .idSanPhamChiTiet(spct.getId())
                        .soLuong(reqQty)
                        .fromCart(true)
                        .build());
                totalQuantity += reqQty;
            }
        }

        if (!invalidItems.isEmpty()) {
            String firstErrMsg = invalidItems.get(0).getTenSanPham() + ": " + invalidItems.get(0).getLyDo();
            return FullCartCheckoutResult.builder()
                    .trangThai("error")
                    .thongBao("Giỏ hàng chứa sản phẩm không hợp lệ: " + firstErrMsg)
                    .message("Giỏ hàng chứa sản phẩm không hợp lệ: " + firstErrMsg)
                    .invalidItems(invalidItems)
                    .build();
        }

        if (contextItems.isEmpty()) {
            return FullCartCheckoutResult.builder()
                    .trangThai("error")
                    .thongBao("Giỏ hàng không có sản phẩm hợp lệ để thanh toán.")
                    .message("Giỏ hàng không có sản phẩm hợp lệ để thanh toán.")
                    .build();
        }

        CheckoutContext context = createCartContext(session, activeAccount ? idNguoiDung : null, contextItems);

        return FullCartCheckoutResult.builder()
                .trangThai("ok")
                .thongBao("Khởi tạo thanh toán thành công.")
                .message("Khởi tạo thanh toán thành công.")
                .checkoutToken(context.getToken())
                .checkoutUrl("/checkout?token=" + context.getToken())
                .itemCount(contextItems.size())
                .totalQuantity(totalQuantity)
                .build();
    }
}


