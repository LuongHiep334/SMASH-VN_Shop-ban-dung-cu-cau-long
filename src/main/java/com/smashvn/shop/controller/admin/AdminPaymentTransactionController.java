package com.smashvn.shop.controller.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.repository.PaymentTransactionRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentTransactionController {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @GetMapping("/admin/transactions")
    public String viewTransactions(
            @RequestParam(value = "orderCode", required = false) String orderCode,
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("orderCode", orderCode);
        redirectAttributes.addAttribute("transactionId", transactionId);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("startDate", startDate);
        redirectAttributes.addAttribute("endDate", endDate);
        redirectAttributes.addAttribute("activeTab", "transactions");

        return "redirect:/admin/don-hang";
    }

    @GetMapping("/admin/transactions/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(value = "orderCode", required = false) String orderCode,
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            HttpSession session) {

        // 1. Role verification: Only Manager (QL) can export
        String role = (String) session.getAttribute("vaiTro");
        if (!"QL".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        List<PaymentTransaction> list = paymentTransactionRepository.filterTransactions(
                cleanParam(orderCode),
                cleanParam(transactionId),
                cleanParam(status),
                start,
                end
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("SePay Transactions");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row headers
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Transaction ID", "Order Code", "Amount (VND)", "Gateway", "Status", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Currency format
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            // Populate rows
            int rowIdx = 1;
            for (PaymentTransaction tx : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getId());
                row.createCell(1).setCellValue(tx.getTransactionId());
                row.createCell(2).setCellValue(tx.getOrder() != null ? tx.getOrder().getMaDonHang() : "N/A");

                Cell amountCell = row.createCell(3);
                amountCell.setCellValue(tx.getAmount().doubleValue());
                amountCell.setCellStyle(currencyStyle);

                row.createCell(4).setCellValue(tx.getGateway());
                row.createCell(5).setCellValue(tx.getStatus());
                row.createCell(6).setCellValue(tx.getCreatedAt().format(formatter));
            }

            // Resize columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "sepay_transactions.xlsx");

            return new ResponseEntity<>(bos.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("SePay Export Excel Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/transactions/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(value = "orderCode", required = false) String orderCode,
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            HttpSession session) {

        // 1. Role verification: Only Manager (QL) can export
        String role = (String) session.getAttribute("vaiTro");
        if (!"QL".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        List<PaymentTransaction> list = paymentTransactionRepository.filterTransactions(
                cleanParam(orderCode),
                cleanParam(transactionId),
                cleanParam(status),
                start,
                end
        );

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for correct character rendering in Excel
        sb.append("\uFEFF");
        sb.append("ID,Transaction ID,Order Code,Amount (VND),Gateway,Status,Created At\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (PaymentTransaction tx : list) {
            sb.append(tx.getId()).append(",")
                    .append(escapeCsv(tx.getTransactionId())).append(",")
                    .append(escapeCsv(tx.getOrder() != null ? tx.getOrder().getMaDonHang() : "N/A")).append(",")
                    .append(tx.getAmount().setScale(0).toString()).append(",")
                    .append(escapeCsv(tx.getGateway())).append(",")
                    .append(escapeCsv(tx.getStatus())).append(",")
                    .append(tx.getCreatedAt().format(formatter)).append("\n");
        }

        byte[] csvBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "sepay_transactions.csv");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr).atTime(java.time.LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanParam(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return val.trim();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
