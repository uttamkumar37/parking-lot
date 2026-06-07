package com.parksmart.event;

import com.parksmart.entity.Booking;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Observer/Event Pattern: Domain event published when a booking changes state.
 * Consumed by Kafka listeners for notifications, analytics, cache invalidation.
 */
@Getter
@Setter
@NoArgsConstructor
public class BookingEvent {

    public enum Type {
        BOOKING_CREATED,
        BOOKING_COMPLETED,
        BOOKING_CANCELLED,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED
    }

    private UUID bookingId;
    private UUID userId;
    private UUID slotId;
    private UUID lotId;
    private Type eventType;
    private LocalDateTime occurredAt;

    // User details
    private String userEmail;
    private String userPhone;
    private String userName;

    // Vehicle/Booking details
    private String licensePlate;
    private String slotNumber;
    private String floorName;
    private String lotName;
    private LocalDateTime entryTime;
    private Long durationMinutes;

    // Payment details
    private String paymentLink;
    private BigDecimal totalAmount;

    private BookingEvent(Builder builder) {
        this.bookingId       = builder.bookingId;
        this.userId          = builder.userId;
        this.slotId          = builder.slotId;
        this.lotId           = builder.lotId;
        this.eventType       = builder.eventType;
        this.occurredAt      = builder.occurredAt;
        this.userEmail       = builder.userEmail;
        this.userPhone       = builder.userPhone;
        this.userName        = builder.userName;
        this.licensePlate    = builder.licensePlate;
        this.slotNumber      = builder.slotNumber;
        this.floorName       = builder.floorName;
        this.lotName         = builder.lotName;
        this.entryTime       = builder.entryTime;
        this.durationMinutes = builder.durationMinutes;
        this.paymentLink     = builder.paymentLink;
        this.totalAmount     = builder.totalAmount;
    }

    public static Builder from(Booking booking, Type type) {
        return new Builder()
            .bookingId(booking.getId())
            .userId(booking.getUser().getId())
            .slotId(booking.getSlot().getId())
            .lotId(booking.getSlot().getFloor().getParkingLot().getId())
            .userEmail(booking.getUser().getEmail())
            .userPhone(booking.getUser().getPhone())
            .userName(booking.getUser().getName())
            .licensePlate(booking.getVehicle().getLicensePlate())
            .slotNumber(booking.getSlot().getSlotNumber())
            .floorName(booking.getSlot().getFloor().getFloorName())
            .lotName(booking.getSlot().getFloor().getParkingLot().getName())
            .entryTime(booking.getEntryTime())
            .durationMinutes(booking.getDurationMinutes())
            .eventType(type)
            .occurredAt(LocalDateTime.now());
    }

    // Builder Pattern
    public static class Builder {
        private UUID bookingId, userId, slotId, lotId;
        private Type eventType;
        private LocalDateTime occurredAt;
        private String userEmail, userPhone, userName;
        private String licensePlate, slotNumber, floorName, lotName;
        private LocalDateTime entryTime;
        private Long durationMinutes;
        private String paymentLink;
        private BigDecimal totalAmount;

        public Builder bookingId(UUID v)           { this.bookingId = v;       return this; }
        public Builder userId(UUID v)              { this.userId = v;          return this; }
        public Builder slotId(UUID v)              { this.slotId = v;          return this; }
        public Builder lotId(UUID v)               { this.lotId = v;           return this; }
        public Builder eventType(Type v)           { this.eventType = v;       return this; }
        public Builder occurredAt(LocalDateTime v) { this.occurredAt = v;      return this; }
        public Builder userEmail(String v)         { this.userEmail = v;       return this; }
        public Builder userPhone(String v)         { this.userPhone = v;       return this; }
        public Builder userName(String v)          { this.userName = v;        return this; }
        public Builder licensePlate(String v)      { this.licensePlate = v;    return this; }
        public Builder slotNumber(String v)        { this.slotNumber = v;      return this; }
        public Builder floorName(String v)         { this.floorName = v;       return this; }
        public Builder lotName(String v)           { this.lotName = v;         return this; }
        public Builder entryTime(LocalDateTime v)  { this.entryTime = v;       return this; }
        public Builder durationMinutes(Long v)     { this.durationMinutes = v; return this; }
        public Builder paymentLink(String v)       { this.paymentLink = v;     return this; }
        public Builder totalAmount(BigDecimal v)   { this.totalAmount = v;     return this; }
        public BookingEvent build()                { return new BookingEvent(this); }
    }
}
