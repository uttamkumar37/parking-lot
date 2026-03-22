package com.parksmart.strategy;

import com.parksmart.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;

/**
 * Weekend/Holiday premium pricing — 1.3× on weekends.
 */
@Component("WEEKEND")
@RequiredArgsConstructor
public class WeekendPricingStrategy implements PricingStrategy {

    private final HourlyPricingStrategy baseStrategy;

    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.30");

    @Override
    public BigDecimal calculateBase(Booking booking) {
        BigDecimal base = baseStrategy.calculateBase(booking);
        DayOfWeek dayOfWeek = booking.getEntryTime().getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
        if (isWeekend) {
            return base.multiply(WEEKEND_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
        }
        return base;
    }

    @Override
    public String getStrategyName() { return "WEEKEND_PREMIUM"; }
}
