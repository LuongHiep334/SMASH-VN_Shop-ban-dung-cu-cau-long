package com.smashvn.shop.service.payment;

import java.util.Map;

import com.smashvn.shop.dto.payment.SepayIpnRequest;

public interface PaymentGatewayService {

    Map<String, Object> handleIpn(SepayIpnRequest request, String rawPayload) throws Exception;
}
