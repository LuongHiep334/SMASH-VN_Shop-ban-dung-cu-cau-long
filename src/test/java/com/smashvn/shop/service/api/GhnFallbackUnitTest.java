package com.smashvn.shop.service.api;

import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.exception.GhnCreateIndeterminateException;
import com.smashvn.shop.exception.GhnSandboxLimitationException;
import com.smashvn.shop.exception.GhnUnsupportedRouteException;
import com.smashvn.shop.repository.SoDiaChiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GhnFallbackUnitTest {

    private GhnConfig config;
    private RestTemplate restTemplate;
    private GhnShipmentPersistenceService persistenceService;
    private GhnService ghnService;

    @BeforeEach
    void setUp() {
        config = new GhnConfig();
        config.setBaseUrl("https://dev-online-gateway.ghn.vn");
        restTemplate = mock(RestTemplate.class);
        persistenceService = mock(GhnShipmentPersistenceService.class);
        ghnService = new GhnService(
                config,
                restTemplate,
                mock(DonViVanChuyenDAO.class),
                mock(SoDiaChiRepository.class),
                persistenceService,
                mock(JdbcTemplate.class));
    }

    @Test
    void onlyExplicitUnsupportedRouteErrorsAreEligibleForSandboxFallback() {
        assertTrue(ghnService.isEligibleForSandboxFallback(
                new GhnUnsupportedRouteException("GHN Sandbox không hỗ trợ tuyến")));
        assertTrue(ghnService.isEligibleForSandboxFallback(
                new GhnSandboxLimitationException("GHN Sandbox không lấy được thông tin kho")));

        HttpClientErrorException unsupportedRoute = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                "{\"message\":\"Tuyến đường không hỗ trợ\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        assertTrue(ghnService.isEligibleForSandboxFallback(unsupportedRoute));

        assertFalse(ghnService.isEligibleForSandboxFallback(
                new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized")));
        assertFalse(ghnService.isEligibleForSandboxFallback(
                new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden")));
        assertFalse(ghnService.isEligibleForSandboxFallback(
                new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error")));
        assertFalse(ghnService.isEligibleForSandboxFallback(
                new ResourceAccessException("Network timeout")));
        assertFalse(ghnService.isEligibleForSandboxFallback(
                new GhnCreateIndeterminateException("Create result is unknown")));
        assertFalse(ghnService.isEligibleForSandboxFallback(
                new IllegalStateException("Invalid internal state")));
    }

    @Test
    void fallbackPersistsDedicatedProviderBeforeReturningDemoCode() {
        HoaDon order = persistedOrder(42);

        String code = ghnService.createFallbackShippingOrder(
                order,
                new GhnUnsupportedRouteException("Unsupported recipient address"));

        assertTrue(code.startsWith("DEMO-GHN-"));
        assertEquals(code, order.getGhnOrderCode());
        assertEquals("ready_to_pick", order.getGhnStatus());
        verify(persistenceService).saveShipment(42, code, "GHN_FALLBACK", "ready_to_pick");
    }

    @Test
    void knownSandboxWarehouseLimitationCanCreateDemoFallback() {
        HoaDon order = persistedOrder(45);

        String code = ghnService.createFallbackShippingOrder(
                order,
                new GhnSandboxLimitationException("Lỗi hệ thống - không lấy được thông tin kho"));

        assertTrue(code.startsWith("DEMO-GHN-"));
        verify(persistenceService).saveShipment(45, code, "GHN_FALLBACK", "ready_to_pick");
    }

    @Test
    void persistenceFailureIsPropagatedAndDoesNotReturnFalseSuccess() {
        HoaDon order = persistedOrder(43);
        DataAccessResourceFailureException databaseFailure =
                new DataAccessResourceFailureException("Cannot persist fallback shipment");
        doThrow(databaseFailure).when(persistenceService)
                .saveShipment(org.mockito.ArgumentMatchers.eq(43),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq("GHN_FALLBACK"),
                        org.mockito.ArgumentMatchers.eq("ready_to_pick"));

        DataAccessResourceFailureException thrown = assertThrows(
                DataAccessResourceFailureException.class,
                () -> ghnService.createFallbackShippingOrder(
                        order,
                        new GhnUnsupportedRouteException("Unsupported recipient address")));

        assertEquals(databaseFailure, thrown);
        assertNull(order.getGhnOrderCode());
        assertNull(order.getGhnStatus());
    }

    @Test
    void directFallbackRejectsUnrelatedErrorsAndProductionEnvironment() {
        HoaDon order = persistedOrder(44);

        assertThrows(IllegalArgumentException.class,
                () -> ghnService.createFallbackShippingOrder(order, new ResourceAccessException("Timeout")));
        verifyNoInteractions(persistenceService);

        config.setBaseUrl("https://online-gateway.ghn.vn");
        assertThrows(IllegalStateException.class,
                () -> ghnService.createFallbackShippingOrder(
                        order,
                        new GhnUnsupportedRouteException("Unsupported recipient address")));
        verifyNoInteractions(persistenceService);
    }

    @Test
    void strictAddressResolutionPropagatesLookupFailureInsteadOfReturningUnsupportedAddress() {
        ResourceAccessException lookupFailure = new ResourceAccessException("GHN address API timeout");
        org.mockito.Mockito.when(restTemplate.exchange(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpMethod.GET),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(String.class)))
                .thenThrow(lookupFailure);
        SoDiaChi address = new SoDiaChi();
        address.setTinhThanh("Hồ Chí Minh");
        address.setDiaChiCuThe("Phường Bến Nghé, Quận 1, TP Hồ Chí Minh");

        ResourceAccessException thrown = assertThrows(
                ResourceAccessException.class,
                () -> ghnService.resolveGhnAddressOrThrow(address));

        assertEquals(lookupFailure, thrown);
        verifyNoInteractions(persistenceService);
    }

    private HoaDon persistedOrder(int id) {
        HoaDon order = new HoaDon();
        order.setId(id);
        order.setMaDonHang("TEST-" + id);
        return order;
    }
}
