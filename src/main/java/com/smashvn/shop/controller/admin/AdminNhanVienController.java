package com.smashvn.shop.controller.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.service.admin.AdminNhanVienService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Controller
@RequestMapping("/admin/nhan-vien")
@RequiredArgsConstructor
public class AdminNhanVienController {

    private final AdminNhanVienService adminNhanVienService;

    private String friendlyErrorMessage(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                message += " " + cause.getMessage();
            }
            cause = cause.getCause();
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("ux_nhanvien_sodienthoai")
                || lowerMessage.contains("so_dien_thoai")
                || lowerMessage.contains("sodienthoai")) {
            return "Số điện thoại nhân viên đã tồn tại. Vui lòng nhập số khác.";
        }
        if (lowerMessage.contains("uk_email")
                || lowerMessage.contains("duplicate key")
                && lowerMessage.contains("email")) {
            return "Email đã được sử dụng.";
        }
        if (lowerMessage.contains("uk_tendangnhap")
                || lowerMessage.contains("ten_dang_nhap")
                || lowerMessage.contains("tendangnhap")) {
            return "Tên đăng nhập đã tồn tại.";
        }
        if (lowerMessage.contains("id_tai_khoan")
                || lowerMessage.contains("tai_khoan")
                || lowerMessage.contains("taikhoan")) {
            return "Tài khoản nhân viên đã tồn tại.";
        }
        if (lowerMessage.contains("cannot insert duplicate key")
                || lowerMessage.contains("duplicate key")
                || lowerMessage.contains("constraint")
                || lowerMessage.contains("hibernate")
                || lowerMessage.contains("sql")) {
            return "Dữ liệu đã tồn tại hoặc không hợp lệ. Vui lòng kiểm tra lại thông tin.";
        }
        return message.isBlank() ? "Không thể lưu nhân viên. Vui lòng kiểm tra lại thông tin." : ex.getMessage();
    }

    @GetMapping
    public String hienThiDanhSach(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {
        List<NhanVien> ds = adminNhanVienService.searchNhanVien(keyword);
        model.addAttribute("danhSachNhanVien", ds);
        model.addAttribute("keyword", keyword);
        return "admin/nhanvien-list";
    }

    @GetMapping("/them")
    public String hienThiFormThem() {
        return "admin/nhanvien-add";
    }

    @PostMapping("/them")
    public String xuLyThemNhanVien(
            @RequestParam("email") String email,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("hoTenNv") String hoTenNv,
            @RequestParam("chucVu") String chucVu,
            @RequestParam("soDienThoaiNv") String soDienThoaiNv,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminNhanVienService.createNhanVien(email, matKhau, hoTenNv, chucVu, soDienThoaiNv, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/nhan-vien?themThanhCong";
        } catch (Exception e) {
            String errorMessage = friendlyErrorMessage(e);
            model.addAttribute("error", errorMessage);
            model.addAttribute("loi", errorMessage);
            model.addAttribute("email", email);
            model.addAttribute("hoTenNv", hoTenNv);
            model.addAttribute("chucVu", chucVu);
            model.addAttribute("soDienThoaiNv", soDienThoaiNv);
            return "admin/nhanvien-add";
        }
    }

    @GetMapping("/sua/{id}")
    public String hienThiFormSua(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            NhanVien nv = adminNhanVienService.findById(id);
            model.addAttribute("nv", nv);
            return "admin/nhanvien-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", friendlyErrorMessage(e));
            return "redirect:/admin/nhan-vien";
        }
    }

    @PostMapping("/sua/{id}")
    public String xuLySuaNhanVien(
            @PathVariable("id") Integer id,
            @RequestParam("hoTenNv") String hoTenNv,
            @RequestParam("chucVu") String chucVu,
            @RequestParam("soDienThoaiNv") String soDienThoaiNv,
            @RequestParam("trangThai") String trangThai,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            HttpSession session,
            HttpServletRequest request,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminNhanVienService.updateNhanVien(id, hoTenNv, chucVu, soDienThoaiNv, trangThai, newPassword, actingTaiKhoanId, ipAddress);
            return "redirect:/admin/nhan-vien?suaThanhCong";
        } catch (Exception e) {
            NhanVien nv = adminNhanVienService.findById(id);
            nv.setHoTenNv(hoTenNv);
            nv.setChucVu(chucVu);
            nv.setSoDienThoaiNv(soDienThoaiNv);
            if (nv.getTaiKhoan() != null) {
                nv.getTaiKhoan().setTrangThai(trangThai);
            }
            String errorMessage = friendlyErrorMessage(e);
            model.addAttribute("nv", nv);
            model.addAttribute("error", errorMessage);
            model.addAttribute("loi", errorMessage);
            return "admin/nhanvien-edit";
        }
    }

    @PostMapping("/toggle/{id}")
    public String xuLyToggleTrangThai(
            @PathVariable("id") Integer id,
            HttpSession session,
            HttpServletRequest request) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();
            String appUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;

            adminNhanVienService.toggleStatus(id, actingTaiKhoanId, ipAddress, appUrl);
            return "redirect:/admin/nhan-vien?toggleThanhCong";
        } catch (Exception e) {
            return "redirect:/admin/nhan-vien?loi=" + java.net.URLEncoder.encode(friendlyErrorMessage(e), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private HttpStatus resolveLockActionErrorStatus(Exception ex) {
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }
        if (ex instanceof IllegalStateException) {
            return HttpStatus.CONFLICT;
        }
        if (ex.getMessage() != null && ex.getMessage().contains("Không tìm thấy")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String showLockConfirmation(
            Integer id,
            String token,
            String actionType,
            HttpSession session,
            Model model) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            NhanVien nv = adminNhanVienService.getPendingLockForConfirmation(id, token, actingTaiKhoanId);
            boolean approve = "approve".equals(actionType);

            model.addAttribute("nv", nv);
            model.addAttribute("token", token == null ? "" : token);
            model.addAttribute("actionType", actionType);
            model.addAttribute("actionUrl", "/admin/nhan-vien/" + (approve ? "approve-lock/" : "reject-lock/") + id);
            model.addAttribute("title", approve ? "Xác Nhận Khóa Tài Khoản" : "Xác Nhận Từ Chối Khóa");
            model.addAttribute("message", approve
                    ? "Thao tác này sẽ khóa tài khoản nhân viên và chặn truy cập hệ thống."
                    : "Thao tác này sẽ hủy yêu cầu khóa và giữ tài khoản nhân viên hoạt động.");
            return "admin/lock-confirm";
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("title", "Liên Kết Xác Nhận Không Hợp Lệ");
            model.addAttribute("message", friendlyErrorMessage(e));
            return "admin/confirm-result";
        }
    }

    @GetMapping("/approve-lock/{id}")
    public String hienThiXacNhanPheDuyetKhoa(
            @PathVariable("id") Integer id,
            @RequestParam(value = "token", required = false) String token,
            HttpSession session,
            Model model) {
        return showLockConfirmation(id, token, "approve", session, model);
    }

    @GetMapping("/reject-lock/{id}")
    public String hienThiXacNhanTuChoiKhoa(
            @PathVariable("id") Integer id,
            @RequestParam(value = "token", required = false) String token,
            HttpSession session,
            Model model) {
        return showLockConfirmation(id, token, "reject", session, model);
    }

    @PostMapping("/approve-lock/{id}")
    public Object xuLyPheDuyetKhoa(
            @PathVariable("id") Integer id,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "ajax", required = false) Boolean ajax,
            @RequestParam(value = "returnToList", defaultValue = "false") boolean returnToList,
            HttpSession session,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            TaiKhoan updatedAccount = adminNhanVienService.approveLock(id, token, actingTaiKhoanId, ipAddress);
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đã phê duyệt khóa tài khoản thành công!",
                        "accountId", updatedAccount.getId(),
                        "status", updatedAccount.getTrangThai(),
                        "statusLabel", "Ngừng hoạt động"));
            }
            if (returnToList && actingTaiKhoanId != null) {
                redirectAttributes.addFlashAttribute("success", "Đã phê duyệt khóa tài khoản thành công!");
                return "redirect:/admin/nhan-vien";
            }
            model.addAttribute("success", true);
            model.addAttribute("title", "Phê Duyệt Khóa Thành Công");
            model.addAttribute("message", "Yêu cầu khóa tài khoản nhân viên đã được duyệt thành công. Tài khoản này hiện đã bị khóa.");
            return "admin/confirm-result";
        } catch (Exception e) {
            String errorMsg = friendlyErrorMessage(e);
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.status(resolveLockActionErrorStatus(e))
                        .body(Map.of("success", false, "message", errorMsg));
            }
            if (returnToList && session.getAttribute("idNguoiDung") != null) {
                redirectAttributes.addFlashAttribute("error", errorMsg);
                redirectAttributes.addFlashAttribute("loi", errorMsg);
                return "redirect:/admin/nhan-vien";
            }
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Thao Tác");
            model.addAttribute("message", errorMsg);
            return "admin/confirm-result";
        }
    }

    @PostMapping("/reject-lock/{id}")
    public Object xuLyTuChoiKhoa(
            @PathVariable("id") Integer id,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "ajax", required = false) Boolean ajax,
            @RequestParam(value = "returnToList", defaultValue = "false") boolean returnToList,
            HttpSession session,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            TaiKhoan updatedAccount = adminNhanVienService.rejectLock(id, token, actingTaiKhoanId, ipAddress);
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đã từ chối khóa tài khoản nhân viên.",
                        "accountId", updatedAccount.getId(),
                        "status", updatedAccount.getTrangThai(),
                        "statusLabel", "Hoạt động"));
            }
            if (returnToList && actingTaiKhoanId != null) {
                redirectAttributes.addFlashAttribute("success", "Đã từ chối khóa tài khoản nhân viên. Trạng thái hoạt động của nhân viên được giữ nguyên.");
                return "redirect:/admin/nhan-vien";
            }
            model.addAttribute("success", true);
            model.addAttribute("title", "Từ Chối Khóa Thành Công");
            model.addAttribute("message", "Đã từ chối khóa tài khoản nhân viên. Trạng thái hoạt động của nhân viên được giữ nguyên.");
            return "admin/confirm-result";
        } catch (Exception e) {
            String errorMsg = friendlyErrorMessage(e);
            if (Boolean.TRUE.equals(ajax)) {
                return ResponseEntity.status(resolveLockActionErrorStatus(e))
                        .body(Map.of("success", false, "message", errorMsg));
            }
            if (returnToList && session.getAttribute("idNguoiDung") != null) {
                redirectAttributes.addFlashAttribute("error", errorMsg);
                redirectAttributes.addFlashAttribute("loi", errorMsg);
                return "redirect:/admin/nhan-vien";
            }
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Thao Tác");
            model.addAttribute("message", errorMsg);
            return "admin/confirm-result";
        }
    }
}
