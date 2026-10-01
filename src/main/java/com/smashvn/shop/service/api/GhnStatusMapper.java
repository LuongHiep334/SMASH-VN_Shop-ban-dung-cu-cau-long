package com.smashvn.shop.service.api;

import org.springframework.stereotype.Component;

/**
 * Lớp tiện ích ánh xạ trạng thái GHN sang trạng thái đơn hàng nội bộ. Được dùng
 * chung bởi cả Webhook (GhnRestController) và Scheduler (GhnPollingScheduler).
 */
@Component
public class GhnStatusMapper {

    /**
     * Ánh xạ trạng thái GHN sang trạng thái đơn hàng nội bộ.
     *
     * @param ghnStatus mã trạng thái từ GHN (ví dụ: "picked", "delivering",
     * "delivered"...)
     * @return trạng thái nội bộ tương ứng ("cho_xac_nhan", "dang_giao",
     * "da_giao", "da_huy") hoặc null nếu trạng thái không được nhận diện
     */
    public String mapToInternalStatus(String ghnStatus) {
        if (ghnStatus == null) {
            return null;
        }

        switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick":
                return "da_tao_van_don_ghn";

            case "picking":
            case "money_collect_picking":
                return "dang_lay_hang";

            case "picked":
                return "da_ban_giao_ghn";

            case "storing":
            case "sorting":
            case "transporting":
            case "delivering":
            case "money_collect_delivering":
                return "dang_giao";
            case "delivery_fail":
                return "giao_that_bai";

            case "delivered":
                return "da_giao";

            case "cancel":
            case "exception":
            case "lost":
            case "damage":
            case "return":
            case "returned":
            case "waiting_to_return":
                return "da_huy";

            default:
                return null;
        }
    }

    /**
     * Trả về nhãn tiếng Việt tương ứng cho mã trạng thái GHN
     */
    public String getGhnStatusLabel(String ghnStatus) {
        if (ghnStatus == null || ghnStatus.isBlank()) {
            return "Chưa có trạng thái GHN";
        }

        switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick":
                return "Sẵn sàng lấy hàng";
            case "picking":
                return "Đang lấy hàng";
            case "money_collect_picking":
                return "Đang lấy hàng & thu tiền người gửi";
            case "picked":
                return "Đã lấy hàng";
            case "storing":
                return "Đã nhập kho GHN";
            case "sorting":
                return "Đang phân loại tại kho GHN";
            case "transporting":
                return "Đang luân chuyển kho";
            case "delivering":
                return "Đang giao hàng";
            case "money_collect_delivering":
                return "Đang giao hàng & thu tiền COD";
            case "delivered":
                return "Giao hàng thành công";
            case "delivery_fail":
                return "Giao hàng thất bại";
            case "waiting_to_return":
                return "Chờ trả hàng";
            case "return":
            case "returning":
                return "Đang trả hàng về shop";
            case "returned":
            case "returned_to_sender":
                return "Đã trả hàng về shop";
            case "cancel":
                return "Đã hủy vận đơn GHN";
            case "exception":
                return "Sự cố vận chuyển";
            case "lost":
                return "Hàng bị thất lạc";
            case "damage":
                return "Hàng bị hư hỏng";
            default:
                return ghnStatus;
        }
    }

    /**
     * Kiểm tra xem trạng thái GHN có phải là trạng thái kết thúc (terminal) hay
     * không. Trạng thái kết thúc: delivered, cancel, exception, lost, damage,
     * return, returned.
     */
    public boolean isTerminalGhnStatus(String ghnStatus) {
        if (ghnStatus == null) {
            return false;
        }

        switch (ghnStatus.toLowerCase()) {
            case "delivered":
            case "cancel":
            case "exception":
            case "lost":
            case "damage":
            case "return":
            case "returned":
                return true;
            default:
                return false;
        }
    }

    /**
     * Ánh xạ trạng thái GHN sang ReturnStatus (State Machine 8 bước trả hàng)
     */
    public com.smashvn.shop.entity.ReturnStatus mapToReturnStatus(String ghnStatus) {
        if (ghnStatus == null) return null;
        switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick":
            case "waiting_to_return":
                return com.smashvn.shop.entity.ReturnStatus.WAITING_FOR_PICKUP;
            case "picking":
            case "picked":
                return com.smashvn.shop.entity.ReturnStatus.PICKED_UP;
            case "storing":
            case "sorting":
            case "transporting":
            case "returning":
            case "delivery_fail":
                return com.smashvn.shop.entity.ReturnStatus.RETURNING;
            case "return":
            case "returned":
            case "returned_to_sender":
            case "delivered":
                return com.smashvn.shop.entity.ReturnStatus.DELIVERED_TO_SHOP;
            case "lost":
                return com.smashvn.shop.entity.ReturnStatus.LOST;
            case "damage":
                return com.smashvn.shop.entity.ReturnStatus.DAMAGED;
            default:
                return null;
        }
    }
}
