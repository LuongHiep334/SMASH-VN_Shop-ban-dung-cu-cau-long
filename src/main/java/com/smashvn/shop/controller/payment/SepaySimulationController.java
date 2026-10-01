package com.smashvn.shop.controller.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.payment.SepayPaymentOrchestratorService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SepaySimulationController {

    private final HoaDonRepository hoaDonRepository;
    private final SepayPaymentOrchestratorService sepayPaymentOrchestratorService;
    private final SepayConfig sepayConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/payment/sepay/simulate")
    public String showSimulationPage(@RequestParam("maDonHang") String maDonHang, Model model) {
        if (!sepayConfig.isDebug()) {
            model.addAttribute("error", "Chức năng giả lập thanh toán SePay đang bị TẮT. Vui lòng cấu hình SEPAY_DEBUG=true trong file .env và KHỞI ĐỘNG LẠI server.");
            return "sepay-simulate";
        }

        Optional<HoaDon> orderOpt = hoaDonRepository.findByMaDonHang(maDonHang);
        if (!orderOpt.isPresent()) {
            model.addAttribute("error", "Không tìm thấy đơn hàng: " + maDonHang);
            return "sepay-simulate";
        }

        HoaDon hd = orderOpt.get();
        if ("DA_THANH_TOAN".equals(hd.getTrangThaiThanhToan()) || "paid".equals(hd.getPaymentStatus())) {
            log.info("Order {} is already paid. Redirecting to home page.", maDonHang);
            return "redirect:/";
        }

        boolean isGuest = hd.getKhachHang() == null 
                || hd.getKhachHang().getTaiKhoan() == null 
                || hd.getKhachHang().getTaiKhoan().getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST;
        model.addAttribute("isGuest", isGuest);
        model.addAttribute("order", hd);
        model.addAttribute("sepayBankAccount", sepayConfig.getBankAccount());
        model.addAttribute("sepayBankName", sepayConfig.getBankName());
        model.addAttribute("sepayMemoPrefix", sepayConfig.getMemoPrefix());
        return "sepay-simulate";
    }

    @PostMapping("/payment/sepay/simulate/success")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simulateSuccess(
            @RequestParam("maDonHang") String maDonHang,
            @RequestParam("amount") BigDecimal amount,
            HttpSession session) {

        log.info("Simulating successful SePay payment for order {}", maDonHang);

        if (!sepayConfig.isDebug()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Chức năng giả lập thanh toán SePay đang bị TẮT. Vui lòng cấu hình SEPAY_DEBUG=true trong file .env và khởi động lại server.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            SepayTransactionDto tx = new SepayTransactionDto();
            String transactionId = "SIM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            tx.setTransactionId(transactionId);
            tx.setTransferAmount(amount);
            tx.setAccountNumber("******7890");
            tx.setContent("Thanh toan don hang " + maDonHang);
            tx.setCode(maDonHang);
            tx.setReferenceCode("-");
            tx.setGateway("Vietcombank");
            tx.setStatus("success");
            tx.setTransactionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            SepayIpnRequest ipnRequest = new SepayIpnRequest();
            ipnRequest.setTransaction(tx);
            ipnRequest.setTransactionId(transactionId);
            ipnRequest.setTransferAmount(amount);
            ipnRequest.setContent("Thanh toan don hang " + maDonHang);
            ipnRequest.setCode(maDonHang);
            ipnRequest.setStatus("success");

            String rawJson = objectMapper.writeValueAsString(ipnRequest);

            Map<String, Object> result = sepayPaymentOrchestratorService.orchestrateSimulatedPayment(
                    maDonHang, amount, ipnRequest, rawJson, session);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", (result != null && result.containsKey("message")) ? result.get("message") : "Simulated successfully!");
            response.put("result", result);
            return ResponseEntity.ok(response);


        } catch (Exception e) {
            log.error("Simulation failed for order {}: {}", maDonHang, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
