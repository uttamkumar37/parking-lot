package com.parksmart.strategy;

import com.parksmart.entity.Booking;
import com.parksmart.entity.ParkingSlot.SlotType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Flat hourly pricing — different rates per slot type.
 * Minimum charge = 1 hour; partial hours rounded up.
 */
@Component("HOURLY")
public class HourlyPricingStrategy implements PricingStrategy {

    // Rates per hour in USD
    private static final BigDecimal SMALL_RATE    = new BigDecimal("2.00");
    private static final BigDecimal MEDIUM_RATE   = new BigDecimal("4.00");
    private static final BigDecimal LARGE_RATE    = new BigDecimal("6.00");
    private static final BigDecimal EV_RATE       = new BigDecimal("5.00");
    private static final BigDecimal OVERSIZED_RATE = new BigDecimal("10.00");

    @Override
    public BigDecimal calculateBase(Booking booking) {
        long minutes = booking.getDurationMinutes();
        // Minimum 1 hour; then ceil to next hour
        long hours = Math.max(1L, (long) Math.ceil(minutes / 60.0));
        BigDecimal hourlyRate = getRateForSlotType(booking.getSlot().getSlotType());
        return hourlyRate.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getRateForSlotType(SlotType slotType) {
        return switch (slotType) {
            case SMALL     -> SMALL_RATE;
            case MEDIUM    -> MEDIUM_RATE;
            case LARGE     -> LARGE_RATE;
            case EV        -> EV_RATE;
            case OVERSIZED -> OVERSIZED_RATE;
        };
    }

    @Override
    public String getStrategyName() { return "HOURLY_FLAT"; }
}
