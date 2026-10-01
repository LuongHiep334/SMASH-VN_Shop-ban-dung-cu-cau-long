package com.smashvn.shop.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosCustomerResponse {
    private boolean success;
    private String message;
    private boolean created;
    private boolean requiresConfirmation;
    private CustomerDto customer;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerDto {
        private Integer id;
        private String hoTen;
        private String sdt;
    }
}
