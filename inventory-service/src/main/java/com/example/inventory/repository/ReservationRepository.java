package com.example.inventory.repository;

import com.example.inventory.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByOrderItemId(Long orderItemId);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'RESERVED' AND r.reservedAt < :cutoff")
    List<Reservation> findExpiredReservations(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT r FROM Reservation r WHERE r.variantId = :variantId AND r.status = 'RESERVED'")
    List<Reservation> findReservedByVariantId(@Param("variantId") Long variantId);
}