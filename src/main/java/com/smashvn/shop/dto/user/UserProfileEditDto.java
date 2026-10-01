package com.smashvn.shop.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEditDto {

    @NotBlank(message = "Họ không được để trống!")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự!")
    private String ho;

    @NotBlank(message = "Tên không được để trống!")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự!")
    private String ten;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^(\\+84|0)(3|5|7|8|9)[0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam!")
    private String sdt;
}
