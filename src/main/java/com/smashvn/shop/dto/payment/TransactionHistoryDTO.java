package com.smashvn.shop.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryDTO(
    Long id,
    String invoiceCode,
    String customerName,
    LocalDateTime transactionTime,
    String paymentMethod,
    String paymentStatus,
    String paymentStatusLabel,
    String transactionId,
    BigDecimal amount
) {}
