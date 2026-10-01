package com.smashvn.shop.security;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/** Keeps the existing MVC session identity and Spring Security identity on the same login. */
public final class LoginSession {
    private static final HttpSessionSecurityContextRepository CONTEXTS = new HttpSessionSecurityContextRepository();
    private static final SessionAuthenticationStrategy STRATEGY = new CompositeSessionAuthenticationStrategy(List.of(
        new ChangeSessionIdAuthenticationStrategy(),
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository())
    ));
    private LoginSession() {}

    public static void establish(HttpServletRequest request, HttpServletResponse response, TaiKhoan account) {
        // A verified temporary password permits setup only; it is not a customer/admin authority.
        var authorities = account.getTrangThaiTaiKhoan() == AccountStatus.GUEST
            ? List.<SimpleGrantedAuthority>of()
            : List.of(new SimpleGrantedAuthority("ROLE_" + account.getVaiTro()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(account.getUsername(), null, authorities);
        STRATEGY.onAuthentication(authentication, request, response);
        clearIdentity(request.getSession(true));
        // Do not mutate a SecurityContext shared with an earlier request or account.
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        CONTEXTS.saveContext(context, request, response);
    }

    public static void clearAuthentication(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) clearIdentity(session);
        var context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);
        CONTEXTS.saveContext(context, request, response);
    }

    private static void clearIdentity(HttpSession session) {
        for (String key : List.of("idNguoiDung", "nguoiDungDangNhap", "vaiTro", "activeRole", "tenHienThi",
                "isGuestView", "guestCheckoutEmail", "allowedGuestOrderAccesses",
                "temporaryPasswordVerified", "pendingPasswordSetupAccountId")) {
            session.removeAttribute(key);
        }
        // Cart and checkout context are deliberately retained for the existing transfer flow.
    }
}

