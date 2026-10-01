package com.smashvn.shop.dto.shipping;

import lombok.Data;

@Data
public class GhnOrderCreateRequestDTO {
    private String client_order_code;
    private Integer payment_type_id = 2; // 2 = Người mua trả
    private String note;
    private String required_note = "KHONGCHOXEMHANG"; // Không cho xem hàng
    private String from_name;
    private String from_phone;
    private String from_address;
    private String from_ward_name;
    private String from_district_name;
    private String from_province_name;
    private Integer from_district_id;
    private String from_ward_code;
    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_code;
    private Integer to_district_id;
    private Integer cod_amount;
    private Integer insurance_value;
    private Integer weight = 500;
    private Integer length = 70;
    private Integer width = 30;
    private Integer height = 10;
    private Integer service_id;
    private Integer service_type_id = 2;
    private java.util.List<GhnItemDTO> items;

    @Data
    public static class GhnItemDTO {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer length = 30;
        private Integer width = 15;
        private Integer height = 5;
        private Integer weight = 500;
    }
}
