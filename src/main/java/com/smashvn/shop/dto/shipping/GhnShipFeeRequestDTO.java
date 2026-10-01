package com.smashvn.shop.dto.shipping;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnShipFeeRequestDTO {
    @JsonProperty("service_type_id")
    private Integer serviceTypeId = 2; // 2 = E-Commerce

    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("from_ward_code")
    private String fromWardCode;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    private Integer weight = 500; // grams, default 500g per racket

    private Integer length = 70; // cm

    private Integer width = 30; // cm

    private Integer height = 10; // cm

    @JsonProperty("insurance_value")
    private Integer insuranceValue = 0;
}
