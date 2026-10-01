package com.smashvn.shop.service.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.smashvn.shop.dto.user.AddressResolutionDto;
import com.smashvn.shop.dto.user.AddressResolutionDto.ResolutionLevel;

class AddressResolutionServiceTest {

    @Test
    void returnsResolvedIdsNamesDetailAndCoordinates() {
        LocationService locationService = mock(LocationService.class);
        GhnService ghnService = mock(GhnService.class);
        AddressResolutionService service = new AddressResolutionService(locationService, ghnService);

        when(locationService.reverseGeocode(21.5942, 105.8482))
                .thenReturn(new LocationService.ReverseGeocodedAddress(
                        List.of("Thái Nguyên"),
                        List.of("Thành phố Thái Nguyên"),
                        List.of("Phường Quyết Thắng"),
                        "Trường Đại học Nông Lâm Thái Nguyên",
                        "vn",
                        "Thái Nguyên, Việt Nam"));
        when(ghnService.resolveAdministrativeAddress(anyList(), anyList(), anyList()))
                .thenReturn(new GhnService.GhnAddressDetails(
                        244, "Thái Nguyên", 9001, "TP. Thái Nguyên", "W001", "Phường Quyết Thắng"));

        AddressResolutionDto result = service.resolve(21.5942, 105.8482);

        assertTrue(result.isSuccess());
        assertEquals(ResolutionLevel.WARD, result.getResolutionLevel());
        assertFalse(result.isManualSelectionRequired());
        assertEquals(244, result.getProvinceId());
        assertEquals(9001, result.getDistrictId());
        assertEquals("W001", result.getWardCode());
        assertEquals("Trường Đại học Nông Lâm Thái Nguyên", result.getAddressDetail());
        assertEquals(21.5942, result.getLatitude());
        assertEquals(105.8482, result.getLongitude());
    }

    @Test
    void keepsAddressDetailAndFriendlyFallbackWhenWardCannotBeMatched() {
        LocationService locationService = mock(LocationService.class);
        GhnService ghnService = mock(GhnService.class);
        AddressResolutionService service = new AddressResolutionService(locationService, ghnService);

        when(locationService.reverseGeocode(21.0, 105.0))
                .thenReturn(new LocationService.ReverseGeocodedAddress(
                        List.of("Hà Nội"), List.of("Ba Đình"), List.of("Khu vực lạ"),
                        "10 Kim Mã", "vn", "10 Kim Mã, Hà Nội"));
        when(ghnService.resolveAdministrativeAddress(anyList(), anyList(), anyList()))
                .thenReturn(new GhnService.GhnAddressDetails(
                        201, "Hà Nội", 1442, "Quận Ba Đình", null, null));

        AddressResolutionDto result = service.resolve(21.0, 105.0);

        assertFalse(result.isSuccess());
        assertEquals(ResolutionLevel.DISTRICT, result.getResolutionLevel());
        assertTrue(result.isManualSelectionRequired());
        assertEquals(201, result.getProvinceId());
        assertEquals(1442, result.getDistrictId());
        assertEquals("10 Kim Mã", result.getAddressDetail());
        assertEquals("Đã xác định Tỉnh/Thành phố và Quận/Huyện. Vui lòng chọn Phường/Xã.",
                result.getMessage());
    }

    @Test
    void keepsReverseGeocodedDetailWhenDeliveryAreaProviderFails() {
        LocationService locationService = mock(LocationService.class);
        GhnService ghnService = mock(GhnService.class);
        AddressResolutionService service = new AddressResolutionService(locationService, ghnService);

        when(locationService.reverseGeocode(21.0, 105.0))
                .thenReturn(new LocationService.ReverseGeocodedAddress(
                        List.of("Hà Nội"), List.of("Ba Đình"), List.of("Phúc Xá"),
                        "10 Kim Mã", "vn", "10 Kim Mã, Hà Nội"));
        when(ghnService.resolveAdministrativeAddress(anyList(), anyList(), anyList()))
                .thenThrow(new IllegalStateException("provider timeout"));

        AddressResolutionDto result = service.resolve(21.0, 105.0);

        assertFalse(result.isSuccess());
        assertEquals(ResolutionLevel.NONE, result.getResolutionLevel());
        assertTrue(result.isManualSelectionRequired());
        assertEquals("10 Kim Mã", result.getAddressDetail());
        assertEquals("Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới.",
                result.getMessage());
    }

    @Test
    void reportsProvinceOnlyWithoutDiscardingIt() {
        LocationService locationService = mock(LocationService.class);
        GhnService ghnService = mock(GhnService.class);
        AddressResolutionService service = new AddressResolutionService(locationService, ghnService);

        when(locationService.reverseGeocode(21.0, 105.0))
                .thenReturn(new LocationService.ReverseGeocodedAddress(
                        List.of("Hà Nội"), List.of("Khu vực lạ"), List.of("Khu vực lạ"),
                        "10 Kim Mã", "vn", "10 Kim Mã, Hà Nội"));
        when(ghnService.resolveAdministrativeAddress(anyList(), anyList(), anyList()))
                .thenReturn(new GhnService.GhnAddressDetails(
                        201, "Hà Nội", null, null, null, null));

        AddressResolutionDto result = service.resolve(21.0, 105.0);

        assertFalse(result.isSuccess());
        assertEquals(ResolutionLevel.PROVINCE, result.getResolutionLevel());
        assertEquals(201, result.getProvinceId());
        assertTrue(result.isManualSelectionRequired());
        assertEquals("Đã xác định Tỉnh/Thành phố. Vui lòng chọn Quận/Huyện và Phường/Xã.",
                result.getMessage());
    }
}
