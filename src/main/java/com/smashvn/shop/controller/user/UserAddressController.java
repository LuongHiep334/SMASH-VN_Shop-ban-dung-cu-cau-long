package com.smashvn.shop.controller.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.dto.user.UserAddressDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.service.user.UserAddressService;
import com.smashvn.shop.service.user.UserDashboardService;
import com.smashvn.shop.service.order.OrderViewService;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;
    private final UserDashboardService dashboardService;
    private final OrderViewService orderViewService;
    private final SanPhamYeuThichRepository wishlistRepository;

    private KhachHang getLoggedInCustomer(HttpSession session) {
        if (session == null || Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            return null;
        }
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        KhachHang kh = dashboardService.layThongTinKhachHang(idTaiKhoan);
        if (kh == null || kh.getTaiKhoan() == null) {
            return null;
        }
        TaiKhoan tk = kh.getTaiKhoan();
        if (tk == null || tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.ACTIVE
                || (tk.getTrangThai() != null && !"hoat_dong".equalsIgnoreCase(tk.getTrangThai()))) {
            return null;
        }
        return kh;
    }

    private String checkRoleAndRedirect(HttpSession session) {
        String vaiTro = (String) session.getAttribute("vaiTro");
        if ("QL".equals(vaiTro)) {
            return "redirect:/admin/all";
        }
        if ("NV".equals(vaiTro)) {
            return "redirect:/admin/don-hang";
        }
        return null;
    }

    private void populateUserStats(KhachHang kh, Model model) {
        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);
    }

    // 1. Trang danh sách
    @GetMapping
    public String hienThiSoDiaChi(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);
        populateUserStats(kh, model);
        model.addAttribute("danhSachDiaChi", addressService.layDanhSachDiaChi(kh.getId()));
        return "dash-address-book";
    }

    // 2. Form thêm mới
    @GetMapping("/add")
    public String hienThiThemDiaChi(HttpSession session, Model model,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "token", required = false) String token) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);
        populateUserStats(kh, model);
        model.addAttribute("fromPage", from); // Truyền trang nguồn vào view
        model.addAttribute("checkoutToken", token);
        if (!model.containsAttribute("addressDto")) {
            model.addAttribute("addressDto", new UserAddressDto());
        }
        return "dash-address-add";
    }

    // 3. Xử lý thêm mới
    @PostMapping("/add")
    public String xuLyThemDiaChi(HttpSession session,
            @Valid @ModelAttribute("addressDto") UserAddressDto addressDto,
            BindingResult bindingResult,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "token", required = false) String token,
            Model model,
            RedirectAttributes redirectAttributes) {

        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        // Xác định trang đích sau khi thêm thành công
        String successRedirect;
        if ("checkout".equals(from) && token != null && !token.isBlank()) {
            successRedirect = "redirect:/checkout?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            successRedirect = "redirect:/user/address";
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            model.addAttribute("loi", errorMessage);
            model.addAttribute("fromPage", from);
            model.addAttribute("checkoutToken", token);
            return "dash-address-add";
        }

        try {
            addressService.themDiaChiMoi(kh, addressDto);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Đã thêm địa chỉ mới thành công!");
            return successRedirect;
        } catch (IllegalArgumentException e) {
            addFieldValidationError(bindingResult, e.getMessage());
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("fromPage", from);
            model.addAttribute("checkoutToken", token);
            return "dash-address-add";
        } catch (IllegalStateException e) {
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("fromPage", from);
            model.addAttribute("checkoutToken", token);
            return "dash-address-add";
        } catch (Exception e) {
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            model.addAttribute("loi", "Có lỗi xảy ra khi thêm địa chỉ.");
            model.addAttribute("fromPage", from);
            model.addAttribute("checkoutToken", token);
            return "dash-address-add";
        }
    }

    // 4. Form cập nhật
    @GetMapping("/edit/{id}")
    public String hienThiSuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        try {
            SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            model.addAttribute("dc", dc);

            if (!model.containsAttribute("addressDto")) {
                UserAddressDto addressDto = UserAddressDto.builder()
                        .id(dc.getId())
                        .hoNguoiNhan(dc.getHoNguoiNhan())
                        .tenNguoiNhan(dc.getTenNguoiNhan())
                        .sdtNguoiNhan(dc.getSdtNguoiNhan())
                        .diaChiCuThe(dc.getDiaChiCuThe())
                        .tinhThanh(dc.getTinhThanh())
                        .quocGia(dc.getQuocGia())
                        .ghnProvinceId(dc.getProvinceId())
                        .ghnDistrictId(dc.getDistrictId())
                        .ghnWardCode(dc.getWardCode())
                        .quanHuyen(dc.getQuanHuyen())
                        .phuongXa(dc.getPhuongXa())
                        .latitude(dc.getLatitude())
                        .longitude(dc.getLongitude())
                        .defaultAddress(dc.isDefaultShipping())
                        .build();
                model.addAttribute("addressDto", addressDto);
            }
            return "dash-address-edit";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Địa chỉ không tồn tại hoặc bạn không có quyền truy cập.");
            return "redirect:/user/address";
        }
    }

    // 5. Xử lý cập nhật
    @PostMapping("/edit/{id}")
    public String xuLySuaDiaChi(@PathVariable("id") Integer idDiaChi, HttpSession session,
            @Valid @ModelAttribute("addressDto") UserAddressDto addressDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        // FIRST: Validate ownership of the address before handling binding result or running validation!
        try {
            addressService.layDiaChiTheoId(idDiaChi, kh.getId());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", e.getMessage());
            return "redirect:/user/address";
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", errorMessage);
            return "dash-address-edit";
        }

        try {
            addressService.capNhatDiaChi(idDiaChi, kh.getId(), addressDto);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Cập nhật địa chỉ thành công!");
            return "redirect:/user/address";
        } catch (IllegalArgumentException e) {
            addFieldValidationError(bindingResult, e.getMessage());
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", e.getMessage());
            return "dash-address-edit";
        } catch (IllegalStateException e) {
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", e.getMessage());
            return "dash-address-edit";
        } catch (Exception e) {
            model.addAttribute("kh", kh);
            populateUserStats(kh, model);
            try {
                SoDiaChi dc = addressService.layDiaChiTheoId(idDiaChi, kh.getId());
                model.addAttribute("dc", dc);
            } catch (Exception ignored) {
            }
            model.addAttribute("loi", "Có lỗi xảy ra khi cập nhật địa chỉ.");
            return "dash-address-edit";
        }
    }

    private void addFieldValidationError(BindingResult bindingResult, String message) {
        if (message == null || message.isBlank()) return;
        String field = null;
        if (message.startsWith("Họ ")) field = "hoNguoiNhan";
        else if (message.startsWith("Tên ")) field = "tenNguoiNhan";
        else if (message.startsWith("Số điện thoại")) field = "sdtNguoiNhan";
        else if (message.startsWith("Địa chỉ cụ thể")) field = "diaChiCuThe";
        else if (message.startsWith("Phường/Xã")) field = "ghnWardCode";
        else if (message.startsWith("Quận/Huyện")) field = "ghnDistrictId";
        else if (message.startsWith("Tỉnh/Thành phố")) field = "ghnProvinceId";
        if (field != null && !bindingResult.hasFieldErrors(field)) {
            bindingResult.rejectValue(field, "address.invalid", message);
        }
    }

    // 6. Xử lý Đặt làm mặc định
    @GetMapping("/set-default/{id}")
    public String thietLapDiaChiMacDinh(@PathVariable("id") Integer idDiaChi, HttpSession session, RedirectAttributes redirectAttributes) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        try {
            addressService.datLamMacDinh(idDiaChi, kh.getId());
            redirectAttributes.addFlashAttribute("thongBaoThanhCong", "Đã thay đổi địa chỉ giao hàng mặc định.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Lỗi: Không thể thay đổi địa chỉ mặc định.");
        }
        return "redirect:/user/address";
    }

    // 7. API Xóa địa chỉ bằng AJAX
    @GetMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> xoaDiaChiAjax(@PathVariable("id") Integer idDiaChi, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);

        if (kh == null) {
            response.put("trangThai", "chuadangnhap");
            return ResponseEntity.ok(response);
        }

        try {
            addressService.xoaDiaChi(idDiaChi, kh.getId());
            response.put("trangThai", "ok");
        } catch (RuntimeException e) {
            response.put("trangThai", "loi");
            
            // Check if it's a data integrity / constraint violation / database exception
            Throwable cause = e;
            boolean isConstraintViolation = false;
            while (cause != null) {
                String name = cause.getClass().getName();
                if (name.contains("ConstraintViolationException") || name.contains("DataIntegrityViolationException") || name.contains("SQLServerException")) {
                    isConstraintViolation = true;
                    break;
                }
                cause = cause.getCause();
            }
            
            if (isConstraintViolation) {
                response.put("message", "Không thể xóa địa chỉ này vì đang được sử dụng cho các đơn hàng. Vui lòng giữ lại để lưu trữ lịch sử giao hàng!");
            } else {
                response.put("message", e.getMessage());
            }
        } catch (Exception e) {
            response.put("trangThai", "loi");
            response.put("message", "Đã xảy ra lỗi hệ thống khi xóa địa chỉ.");
        }
        return ResponseEntity.ok(response);
    }
}
