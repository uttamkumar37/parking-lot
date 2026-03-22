package com.parksmart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "parking_floors",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_floor_number_lot",
        columnNames = {"lot_id", "floor_number"}
    ))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParkingFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"floors", "hibernateLazyInitializer"})
    private ParkingLot parkingLot;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "floor_name", length = 50)
    private String floorName;   // e.g. "Ground", "Level 1", "Basement"

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ParkingSlot> slots = new ArrayList<>();

    public int getAvailableSlotCount() {
        return (int) slots.stream()
            .filter(s -> s.getStatus() == ParkingSlot.SlotStatus.AVAILABLE)
            .count();
    }
}
