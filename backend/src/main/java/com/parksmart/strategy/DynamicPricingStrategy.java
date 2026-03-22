package com.parksmart.strategy;

import com.parksmart.entity.Booking;
import com.parksmart.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Dynamic / surge pricing strategy.
 * Applies a demand multiplier on top of the hourly base rate.
 *
 * Occupancy   → Multiplier
 * < 50%       → 1.0×  (normal)
 * 50–70%      → 1.2×  (+20%)
 * 70–85%      → 1.5×  (+50%)
 * > 85%       → 2.0×  (peak surge)
 */
@Component("DYNAMIC")
@RequiredArgsConstructor
public class DynamicPricingStrategy implements PricingStrategy {

    private final HourlyPricingStrategy baseStrategy;
    private final BookingRepository bookingRepository;

    @Override
    public BigDecimal calculateBase(Booking booking) {
        BigDecimal base = baseStrategy.calculateBase(booking);
        BigDecimal multiplier = resolveSurgeMultiplier(booking);
        return base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveSurgeMultiplier(Booking booking) {
        UUID lotId = booking.getSlot().getFloor().getParkingLot().getId();
        long activeCount = bookingRepository.countActiveBookingsByLot(lotId);
        // Note: total capacity would be fetched from ParkingLotRepository in production
        // Here we use a simplified heuristic
        double occupancyEstimate = Math.min(activeCount / 100.0, 1.0);

        if (occupancyEstimate >= 0.85) return new BigDecimal("2.00");
        if (occupancyEstimate >= 0.70) return new BigDecimal("1.50");
        if (occupancyEstimate >= 0.50) return new BigDecimal("1.20");
        return BigDecimal.ONE;
    }

    @Override
    public String getStrategyName() { return "DYNAMIC_SURGE"; }
}
