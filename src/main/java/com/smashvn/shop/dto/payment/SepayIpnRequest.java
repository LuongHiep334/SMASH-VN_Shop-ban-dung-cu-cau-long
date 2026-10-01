package com.smashvn.shop.dto.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayIpnRequest extends SepayTransactionDto {
    @JsonAlias({"transaction", "data"})
    private SepayTransactionDto transaction;

    public SepayTransactionDto getTransactionData() {
        return transaction != null ? transaction : this;
    }
}
