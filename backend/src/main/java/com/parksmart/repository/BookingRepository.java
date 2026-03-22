package com.parksmart.repository;

import com.parksmart.entity.Booking;
import com.parksmart.entity.Booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    Page<Booking> findAll(Pageable pageable);

    Optional<Booking> findBySlotIdAndStatus(UUID slotId, BookingStatus status);

    /**
     * Check if a vehicle currently has an active booking.
     */
    boolean existsByVehicleIdAndStatus(UUID vehicleId, BookingStatus status);

    /**
     * Admin dashboard: total active bookings per lot.
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        JOIN b.slot s
        JOIN s.floor f
        JOIN f.parkingLot l
        WHERE l.id = :lotId AND b.status = 'ACTIVE'
        """)
    long countActiveBookingsByLot(@Param("lotId") UUID lotId);

    /**
     * Revenue query: sum of bills in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(bi.totalAmount), 0)
        FROM Bill bi
        JOIN bi.booking b
        WHERE b.status = 'COMPLETED'
          AND b.exitTime BETWEEN :from AND :to
        """)
    java.math.BigDecimal sumRevenueInPeriod(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * Occupancy history for AI demand prediction.
     */
    @Query(value = """
        SELECT DATE_TRUNC('hour', entry_time) AS hour,
               COUNT(*) AS count
        FROM bookings
        WHERE entry_time >= :since
        GROUP BY 1
        ORDER BY 1
        """, nativeQuery = true)
    List<Object[]> getHourlyOccupancyHistory(@Param("since") LocalDateTime since);
}
