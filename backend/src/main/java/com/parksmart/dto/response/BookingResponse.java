package com.parksmart.dto.response;

import com.parksmart.entity.Booking.BookingStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.entity.Vehicle.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class BookingResponse {
    private UUID bookingId;
    private String slotNumber;
    private String floorName;
    private String lotName;
    private SlotType slotType;
    private String licensePlate;
    private VehicleType vehicleType;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Long durationMinutes;
    private BookingStatus status;
    private BillInfo bill;

    @Data @Builder
    public static class BillInfo {
        private UUID billId;
        private BigDecimal baseAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String pricingStrategy;
        private String paymentLink;
        private String paymentStatus;
    }
}
