package com.smashvn.shop.controller.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Public help pages: no customer session or database access is required. */
@Controller
public class SupportPageController {

    @GetMapping("/huong-dan-mua-hang")
    public String buyingGuide(Model model) {
        return page(model, "buying", "Hướng dẫn mua hàng",
                "Từ chọn sản phẩm đến theo dõi đơn hàng: hướng dẫn mua sắm tại SMASH VN.");
    }

    @GetMapping("/huong-dan-thanh-toan")
    public String paymentGuide(Model model) {
        return page(model, "payment", "Hướng dẫn thanh toán",
                "Hướng dẫn COD, chuyển khoản QR và kiểm tra kết quả thanh toán tại SMASH VN.");
    }

    @GetMapping("/chinh-sach")
    public String policies(Model model) {
        return page(model, "policies", "Chính sách mua hàng",
                "Thông tin tài khoản, giao hàng, hủy đơn, đổi trả và hỗ trợ sau mua tại SMASH VN.");
    }

    private String page(Model model, String page, String title, String description) {
        model.addAttribute("supportPage", page);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageDescription", description);
        return "support-page";
    }
}
