package com.smashvn.shop.dto.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayTransactionDto {
    @JsonAlias({"id", "transactionId"})
    private String transactionId;

    private String transactionDate;
    private String accountNumber;
    private BigDecimal transferAmount;
    private String content;
    private String code;
    private String referenceCode;
    private String gateway;
    private String status;
}
