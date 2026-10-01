package com.smashvn.shop.controller.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smashvn.shop.dto.user.AddressResolutionDto;
import com.smashvn.shop.dto.user.AddressResolutionDto.ResolutionLevel;
import com.smashvn.shop.service.api.AddressResolutionService;

class AddressRestControllerTest {

    private AddressResolutionService addressResolutionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        addressResolutionService = mock(AddressResolutionService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AddressRestController(addressResolutionService))
                .build();
    }

    @Test
    void rejectsInvalidCoordinatesBeforeCallingProviders() throws Exception {
        mockMvc.perform(get("/api/address/resolve").param("lat", "91").param("lng", "105"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tọa độ không hợp lệ. Vui lòng chọn địa chỉ bên dưới."));

        verifyNoInteractions(addressResolutionService);
    }

    @Test
    void returnsResolvedDeliveryArea() throws Exception {
        when(addressResolutionService.resolve(21.5942, 105.8482))
                .thenReturn(AddressResolutionDto.builder()
                        .success(true)
                        .resolutionLevel(ResolutionLevel.WARD)
                        .manualSelectionRequired(false)
                        .provinceId(244)
                        .districtId(9001)
                        .wardCode("W001")
                        .build());

        mockMvc.perform(get("/api/address/resolve")
                        .param("lat", "21.5942").param("lng", "105.8482"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resolutionLevel").value("WARD"))
                .andExpect(jsonPath("$.manualSelectionRequired").value(false))
                .andExpect(jsonPath("$.provinceId").value(244))
                .andExpect(jsonPath("$.districtId").value(9001))
                .andExpect(jsonPath("$.wardCode").value("W001"));
    }

    @Test
    void providerFailureReturnsFriendlyManualFallback() throws Exception {
        when(addressResolutionService.resolve(21.0, 105.0))
                .thenThrow(new IllegalStateException("technical provider failure"));

        mockMvc.perform(get("/api/address/resolve").param("lat", "21").param("lng", "105"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.resolutionLevel").value("NONE"))
                .andExpect(jsonPath("$.manualSelectionRequired").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới."));
    }
}
