package com.parksmart.repository;

import com.parksmart.entity.ParkingSlot;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, UUID> {

    /**
     * Find first available slot of given type in a specific lot.
     * Ordered by floor number (ground floor preferred) then slot number.
     */
    @Query("""
        SELECT s FROM ParkingSlot s
        JOIN s.floor f
        JOIN f.parkingLot l
        WHERE l.id = :lotId
          AND s.slotType = :slotType
          AND s.status = 'AVAILABLE'
        ORDER BY f.floorNumber ASC, s.slotNumber ASC
        LIMIT 1
        """)
    Optional<ParkingSlot> findFirstAvailableSlot(
        @Param("lotId") UUID lotId,
        @Param("slotType") SlotType slotType
    );

    /**
     * Count available slots by type within a lot — used for dashboard/cache.
     */
    @Query("""
        SELECT s.slotType, COUNT(s)
        FROM ParkingSlot s
        JOIN s.floor f
        JOIN f.parkingLot l
        WHERE l.id = :lotId AND s.status = 'AVAILABLE'
        GROUP BY s.slotType
        """)
    List<Object[]> countAvailableSlotsByTypeLot(@Param("lotId") UUID lotId);

    /**
     * Atomically update slot status — called with @Lock at service level.
     */
    @Modifying
    @Query("UPDATE ParkingSlot s SET s.status = :status WHERE s.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") SlotStatus status);

    List<ParkingSlot> findByFloorId(UUID floorId);

    List<ParkingSlot> findByFloorIdAndStatus(UUID floorId, SlotStatus status);
}
