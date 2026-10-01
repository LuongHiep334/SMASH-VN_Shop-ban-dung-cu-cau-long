package com.smashvn.shop.controller.admin;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smashvn.shop.service.admin.AdminThongKeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/thong-ke")
@RequiredArgsConstructor
public class AdminThongKeController {

    private final AdminThongKeService adminThongKeService;

    // 1. Hiển thị Trang HTML Thống kê
    @GetMapping
    public String hienThiTrangThongKe(
            @RequestParam(value = "preset", required = false, defaultValue = "last_30_days") String preset,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            Model model) {
        
        // Lấy khoảng thời gian để hiển thị mặc định trên trang
        Map<String, LocalDateTime> range = adminThongKeService.getDateRange(preset, startDate, endDate);
        
        model.addAttribute("preset", preset);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("resolvedStart", range.get("start"));
        model.addAttribute("resolvedEnd", range.get("end"));
        
        return "admin/thongke"; // Trả về templates/admin/thongke.html
    }

    // 2. API lấy dữ liệu JSON (Trả về dữ liệu thống kê qua AJAX)
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layApiThongKe(
            @RequestParam(value = "preset", required = false, defaultValue = "last_30_days") String preset,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        Map<String, LocalDateTime> range = adminThongKeService.getDateRange(preset, startDate, endDate);
        Map<String, Object> statsData = adminThongKeService.getStatisticsData(preset, range.get("start"), range.get("end"));
        return ResponseEntity.ok(statsData);
    }

    // 3. API Xuất Báo cáo Excel
    @GetMapping("/export")
    public ResponseEntity<byte[]> xuatExcelThongKe(
            @RequestParam(value = "preset", required = false, defaultValue = "last_30_days") String preset,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        try {
            Map<String, LocalDateTime> range = adminThongKeService.getDateRange(preset, startDate, endDate);
            byte[] excelBytes = adminThongKeService.exportToExcel(range.get("start"), range.get("end"));

            String fileName = "SmashVN_BaoCaoThongKe_" + 
                    range.get("start").format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_to_" + 
                    range.get("end").format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
