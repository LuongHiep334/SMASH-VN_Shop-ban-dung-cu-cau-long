package com.smashvn.shop.repository;

import com.smashvn.shop.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findByOrder_Id(Integer orderId);

    List<PaymentTransaction> findByOrder_IdAndStatus(Integer orderId, String status);

    boolean existsByOrder_IdAndStatus(Integer orderId, String status);

    /**
     * Các bút toán hoàn tiền thành công phát sinh trong kỳ báo cáo.
     * Projection giữ cho tầng thống kê không phải truy cập lazy relation HoaDon.
     */
    @Query("SELECT pt.order.id, pt.amount, pt.createdAt FROM PaymentTransaction pt " +
           "WHERE pt.order IS NOT NULL " +
           "AND UPPER(pt.status) = 'REFUND_SUCCESS' " +
           "AND pt.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pt.createdAt ASC")
    List<Object[]> findSuccessfulRefundEventsInPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT pt FROM PaymentTransaction pt LEFT JOIN pt.order o WHERE " +
           "(:orderCode IS NULL OR CAST(o.id AS string) LIKE %:orderCode% OR :orderCode LIKE CONCAT('%', o.id, '%')) AND " +
           "(:transactionId IS NULL OR pt.transactionId LIKE %:transactionId%) AND " +
           "(:status IS NULL OR pt.status = :status) AND " +
           "(:startDate IS NULL OR pt.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR pt.createdAt <= :endDate) " +
           "ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> filterTransactions(
        @Param("orderCode") String orderCode,
        @Param("transactionId") String transactionId,
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
