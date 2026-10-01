package com.smashvn.shop.controller.payment;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.payment.SepayGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SepaySimulationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private SepayGatewayService sepayGatewayService;

    @Mock
    private com.smashvn.shop.service.payment.SepayPaymentOrchestratorService sepayPaymentOrchestratorService;

    @Mock
    private com.smashvn.shop.config.SepayConfig sepayConfig;

    @InjectMocks
    private SepaySimulationController sepaySimulationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sepayConfig.getBankAccount()).thenReturn("123456");
        when(sepayConfig.getBankName()).thenReturn("Vietcombank");
        when(sepayConfig.getMemoPrefix()).thenReturn("SMASHVN");
        when(sepayConfig.isDebug()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(sepaySimulationController).build();
    }


    @Test
    void testShowSimulationPage_OrderNotFound() throws Exception {
        when(hoaDonRepository.findByMaDonHang("DH999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/payment/sepay/simulate")
                        .param("maDonHang", "DH999"))
                .andExpect(status().isOk())
                .andExpect(view().name("sepay-simulate"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testShowSimulationPage_OrderNotPaid_ReturnsView() throws Exception {
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("DH123");
        hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        hd.setPaymentStatus("pending");
        hd.setTongTien(new BigDecimal("500000"));

        when(hoaDonRepository.findByMaDonHang("DH123")).thenReturn(Optional.of(hd));

        mockMvc.perform(get("/payment/sepay/simulate")
                        .param("maDonHang", "DH123"))
                .andExpect(status().isOk())
                .andExpect(view().name("sepay-simulate"))
                .andExpect(model().attribute("order", hd))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    void testShowSimulationPage_OrderAlreadyPaid_RedirectsToHome() throws Exception {
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("DH123");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setPaymentStatus("paid");

        when(hoaDonRepository.findByMaDonHang("DH123")).thenReturn(Optional.of(hd));

        mockMvc.perform(get("/payment/sepay/simulate")
                        .param("maDonHang", "DH123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testSimulateSuccess_OrderAlreadyPaid_ReturnsSuccessWithoutProcessing() throws Exception {
        HashMap<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đơn hàng đã được thanh toán trước đó.");
        when(sepayPaymentOrchestratorService.orchestrateSimulatedPayment(eq("DH123"), any(), any(), any(), any())).thenReturn(res);


        mockMvc.perform(post("/payment/sepay/simulate/success")
                        .param("maDonHang", "DH123")
                        .param("amount", "500000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đơn hàng đã được thanh toán trước đó."));
    }

    @Test
    void testSimulateSuccess_OrderNotPaid_ProcessesSuccessfully() throws Exception {
        HashMap<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Simulated successfully!");
        when(sepayPaymentOrchestratorService.orchestrateSimulatedPayment(anyString(), any(), any(), any(), any())).thenReturn(res);

        mockMvc.perform(post("/payment/sepay/simulate/success")
                        .param("maDonHang", "DH123")
                        .param("amount", "500000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Simulated successfully!"));
    }
}

