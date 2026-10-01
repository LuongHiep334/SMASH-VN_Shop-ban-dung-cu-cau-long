package com.smashvn.shop.service.admin;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.DonViVanChuyen;

import lombok.RequiredArgsConstructor;

/**
 * Cung cấp danh sách đơn vị vận chuyển cho các luồng đặt hàng và tính phí.
 *
 * Phí GHN được lấy trực tiếp qua tích hợp GHN; service này không còn cung cấp
 * chức năng quản trị để cấu hình lại giá hoặc thông tin kết nối GHN.
 */
@Service
@RequiredArgsConstructor
public class AdminShippingService {

    private final DonViVanChuyenDAO donViVanChuyenDAO;

    @Cacheable(value = "shipping-carriers")
    public List<DonViVanChuyen> getAllCarriers() {
        return donViVanChuyenDAO.findAll();
    }
}
