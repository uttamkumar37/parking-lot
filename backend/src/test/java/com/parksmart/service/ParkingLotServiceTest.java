package com.parksmart.service;

import com.parksmart.dto.response.SlotMapResponse;
import com.parksmart.entity.ParkingFloor;
import com.parksmart.entity.ParkingLot;
import com.parksmart.entity.ParkingSlot;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.exception.NotFoundException;
import com.parksmart.repository.ParkingLotRepository;
import com.parksmart.repository.ParkingSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceTest {

    @Mock ParkingLotRepository lotRepository;
    @Mock ParkingSlotRepository slotRepository;

    @InjectMocks ParkingLotService parkingLotService;

    @Test
    void getSlotMap_returnsFloorsSectionsAndSlotStatuses() {
        UUID lotId = UUID.randomUUID();
        ParkingLot lot = ParkingLot.builder()
            .id(lotId)
            .name("Downtown")
            .address("123 Main")
            .city("San Francisco")
            .active(true)
            .totalFloors(1)
            .floors(new ArrayList<>())
            .build();
        ParkingFloor floor = ParkingFloor.builder()
            .id(UUID.randomUUID())
            .parkingLot(lot)
            .floorNumber(1)
            .floorName("Ground")
            .slots(new ArrayList<>())
            .build();
        lot.getFloors().add(floor);

        for (int i = 1; i <= 21; i++) {
            floor.getSlots().add(ParkingSlot.builder()
                .id(UUID.randomUUID())
                .floor(floor)
                .slotNumber("M-%02d".formatted(i))
                .slotType(i == 21 ? SlotType.EV : SlotType.MEDIUM)
                .status(i == 2 ? SlotStatus.OCCUPIED : SlotStatus.AVAILABLE)
                .hasEvCharger(i == 21)
                .build());
        }

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        SlotMapResponse response = parkingLotService.getSlotMap(lotId);

        assertThat(response.getLotId()).isEqualTo(lotId);
        assertThat(response.getFloors()).hasSize(1);
        SlotMapResponse.FloorResponse floorResponse = response.getFloors().getFirst();
        assertThat(floorResponse.getTotalSlots()).isEqualTo(21);
        assertThat(floorResponse.getAvailableSlots()).isEqualTo(20);
        assertThat(floorResponse.getOccupiedSlots()).isEqualTo(1);
        assertThat(floorResponse.getSections()).hasSize(2);
        assertThat(floorResponse.getSections().getFirst().getSectionName()).isEqualTo("A");
        assertThat(floorResponse.getSections().getFirst().getSlots().getFirst().getCode()).isEqualTo("A01");
        assertThat(floorResponse.getSections().get(1).getSlots().getFirst().getCode()).isEqualTo("B01");
        assertThat(floorResponse.getSections().get(1).getSlots().getFirst().isEvSupported()).isTrue();
        assertThat(floorResponse.getSections().get(1).getSlots().getFirst().getHourlyRate())
            .isEqualByComparingTo("5.00");
    }

    @Test
    void getSlotMap_withUnknownLot_throwsNotFound() {
        UUID lotId = UUID.randomUUID();
        when(lotRepository.findById(lotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingLotService.getSlotMap(lotId))
            .isInstanceOf(NotFoundException.class);
    }
}
