package com.example.order.repository;

import com.example.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByOrderIdAndStatus(Long orderId, OrderItem.OrderItemStatus status);

    @Query("SELECT i FROM OrderItem i WHERE i.status = 'RESERVED' AND i.reservedAt < :cutoff")
    List<OrderItem> findExpiredReservations(@Param("cutoff") LocalDateTime cutoff);
}