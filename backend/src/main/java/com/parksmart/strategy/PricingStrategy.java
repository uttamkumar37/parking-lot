package com.parksmart.strategy;

import com.parksmart.entity.Booking;

import java.math.BigDecimal;

/**
 * Strategy Pattern: Pluggable pricing algorithm.
 * Each implementation can be swapped at runtime based on config/time/demand.
 */
public interface PricingStrategy {

    /**
     * Calculate the base charge for a booking.
     *
     * @param booking the completed booking with duration
     * @return base amount before tax
     */
    BigDecimal calculateBase(Booking booking);

    /**
     * Human-readable strategy name stored in the bill record.
     */
    String getStrategyName();
}
