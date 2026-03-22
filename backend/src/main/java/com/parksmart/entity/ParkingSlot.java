package com.parksmart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parking_slots", indexes = {
    @Index(name = "idx_slots_floor_status", columnList = "floor_id, status"),
    @Index(name = "idx_slots_type_status", columnList = "slot_type, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"slots", "parkingLot", "hibernateLazyInitializer"})
    private ParkingFloor floor;

    @Column(name = "slot_number", nullable = false, length = 10)
    private String slotNumber;  // e.g. "A-01", "B-15"

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false)
    private SlotType slotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Column(name = "has_ev_charger", nullable = false)
    @Builder.Default
    private boolean hasEvCharger = false;

    @Column(name = "is_handicap_accessible", nullable = false)
    @Builder.Default
    private boolean handicapAccessible = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SlotType {
        SMALL,       // Motorcycles
        MEDIUM,      // Cars, SUVs
        LARGE,       // Trucks
        EV,          // Electric vehicles (with charger)
        OVERSIZED    // Buses
    }

    public enum SlotStatus {
        AVAILABLE,
        OCCUPIED,
        RESERVED,
        MAINTENANCE
    }
}
