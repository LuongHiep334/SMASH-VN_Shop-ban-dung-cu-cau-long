package com.smashvn.shop.service.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.repository.SoDiaChiRepository;

class GhnAddressMatchingTest {

    private RestTemplate restTemplate;
    private GhnService service;

    @BeforeEach
    void setUp() {
        GhnConfig config = new GhnConfig();
        config.setBaseUrl("https://sandbox.example");
        config.setToken("test-token");
        config.setShopId(1);

        restTemplate = mock(RestTemplate.class);
        DonViVanChuyenDAO carrierRepository = mock(DonViVanChuyenDAO.class);
        when(carrierRepository.findAll()).thenReturn(List.of());
        service = new GhnService(
                config,
                restTemplate,
                carrierRepository,
                mock(SoDiaChiRepository.class),
                mock(GhnShipmentPersistenceService.class),
                mock(JdbcTemplate.class));

        when(restTemplate.exchange(
                eq("https://sandbox.example/shiip/public-api/master-data/province"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"code":200,"data":[
                          {"ProvinceID":244,"ProvinceName":"Thái Nguyên","NameExtension":["Tỉnh Thái Nguyên"]},
                          {"ProvinceID":201,"ProvinceName":"Hà Nội","NameExtension":["TP. Hà Nội"]}
                        ]}
                        """));
        when(restTemplate.postForEntity(
                eq("https://sandbox.example/shiip/public-api/master-data/district"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"code":200,"data":[
                          {"DistrictID":9001,"DistrictName":"TP. Thái Nguyên","NameExtension":["Thành phố Thái Nguyên"]},
                          {"DistrictID":9002,"DistrictName":"Huyện Đồng Hỷ","NameExtension":[]}
                        ]}
                        """));
        when(restTemplate.postForEntity(
                eq("https://sandbox.example/shiip/public-api/master-data/ward"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"code":200,"data":[
                          {"WardCode":"W001","WardName":"Phường Quyết Thắng","NameExtension":["P. Quyết Thắng"]},
                          {"WardCode":"W002","WardName":"Phường Tân Thịnh","NameExtension":[]}
                        ]}
                        """));
    }

    @Test
    void resolvesAliasesAccentsPunctuationAndAdministrativePrefixesWithinEachParent() {
        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Tỉnh Thái Nguyên"),
                List.of("Thành phố Thái Nguyên"),
                List.of("P. Quyết Thắng"));

        assertEquals(244, result.getProvinceId());
        assertEquals("Thái Nguyên", result.getProvinceName());
        assertEquals(9001, result.getDistrictId());
        assertEquals("TP. Thái Nguyên", result.getDistrictName());
        assertEquals("W001", result.getWardCode());
        assertEquals("Phường Quyết Thắng", result.getWardName());
    }

    @Test
    void doesNotUsePartialContainsMatchingForAmbiguousPlaceFragments() {
        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Nguyên"), List.of("Thái Nguyên"), List.of("Quyết Thắng"));

        assertNull(result.getProvinceId());
        assertNull(result.getDistrictId());
        assertNull(result.getWardCode());
    }

    @Test
    void validatesSubmittedHierarchyAndReturnsCanonicalNames() {
        GhnService.GhnAddressDetails result = service.validateSelectedAddress(244, 9001, "W001");

        assertEquals("Thái Nguyên", result.getProvinceName());
        assertEquals("TP. Thái Nguyên", result.getDistrictName());
        assertEquals("Phường Quyết Thắng", result.getWardName());
    }

    @Test
    void prefersAuthoritativeWardCandidateOverNeighbourhoodWithAnotherWardName() {
        when(restTemplate.postForEntity(
                eq("https://sandbox.example/shiip/public-api/master-data/ward"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"code":200,"data":[
                          {"WardCode":"120118","WardName":"Phường Trưng Vương","NameExtension":["Trưng Vương"]},
                          {"WardCode":"120106","WardName":"Phường Phan Đình Phùng","NameExtension":["Phan Đình Phùng"]}
                        ]}
                        """));

        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Tỉnh Thái Nguyên"),
                List.of("Thành phố Thái Nguyên"),
                List.of("Phường Phan Đình Phùng", "Trưng Vương"));

        assertEquals("120106", result.getWardCode());
        assertEquals("Phường Phan Đình Phùng", result.getWardName());
    }

    @Test
    void keepsProvinceAndDistrictWhenWardCatalogTemporarilyFails() {
        when(restTemplate.postForEntity(
                eq("https://sandbox.example/shiip/public-api/master-data/ward"),
                any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Tỉnh Thái Nguyên"),
                List.of("Thành phố Thái Nguyên"),
                List.of("Phường Quyết Thắng"));

        assertEquals(244, result.getProvinceId());
        assertEquals(9001, result.getDistrictId());
        assertNull(result.getWardCode());
    }

    @Test
    void infersDistrictAndWardWhenWardMatchesExactlyOneDistrictInProvince() {
        stubWardCatalogsByDistrict(false, false);

        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Tỉnh Thái Nguyên"),
                List.of(),
                List.of("Phường Quyết Thắng"));

        assertEquals(244, result.getProvinceId());
        assertEquals(9001, result.getDistrictId());
        assertEquals("TP. Thái Nguyên", result.getDistrictName());
        assertEquals("W001", result.getWardCode());
        assertEquals("Phường Quyết Thắng", result.getWardName());
    }

    @Test
    void keepsProvinceOnlyWhenWardMatchesMoreThanOneDistrict() {
        stubWardCatalogsByDistrict(true, false);

        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Tỉnh Thái Nguyên"),
                List.of(),
                List.of("Phường Quyết Thắng"));

        assertEquals(244, result.getProvinceId());
        assertNull(result.getDistrictId());
        assertNull(result.getWardCode());
    }

    @Test
    void keepsProvinceOnlyWhenCrossDistrictWardScanIsIncomplete() {
        stubWardCatalogsByDistrict(false, true);

        GhnService.GhnAddressDetails result = service.resolveAdministrativeAddress(
                List.of("Tỉnh Thái Nguyên"),
                List.of(),
                List.of("Phường Quyết Thắng"));

        assertEquals(244, result.getProvinceId());
        assertNull(result.getDistrictId());
        assertNull(result.getWardCode());
    }

    private void stubWardCatalogsByDistrict(boolean duplicateWard, boolean secondDistrictFails) {
        when(restTemplate.postForEntity(
                eq("https://sandbox.example/shiip/public-api/master-data/ward"),
                any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<?> request = invocation.getArgument(1);
                    Map<?, ?> body = (Map<?, ?>) request.getBody();
                    int districtId = ((Number) body.get("district_id")).intValue();

                    if (districtId == 9002 && secondDistrictFails) {
                        throw new ResourceAccessException("timeout");
                    }
                    if (districtId == 9001) {
                        return ResponseEntity.ok("""
                                {"code":200,"data":[
                                  {"WardCode":"W001","WardName":"Phường Quyết Thắng","NameExtension":["P. Quyết Thắng"]},
                                  {"WardCode":"W002","WardName":"Phường Tân Thịnh","NameExtension":[]}
                                ]}
                                """);
                    }
                    return ResponseEntity.ok(duplicateWard ? """
                            {"code":200,"data":[
                              {"WardCode":"W101","WardName":"Phường Quyết Thắng","NameExtension":[]}
                            ]}
                            """ : """
                            {"code":200,"data":[
                              {"WardCode":"W102","WardName":"Thị trấn Chùa Hang","NameExtension":[]}
                            ]}
                            """);
                });
    }
}
