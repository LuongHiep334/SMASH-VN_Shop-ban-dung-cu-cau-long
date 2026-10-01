package com.smashvn.shop.dto.user;

import jakarta.validation.constraints.NotBlank;
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
public class PosRegisterCustomerRequest {

    @NotBlank(message = "Họ tên không được để trống!")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống!")
    private String soDienThoai;
}
