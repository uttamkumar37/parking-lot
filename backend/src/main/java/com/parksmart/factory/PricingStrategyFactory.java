package com.parksmart.factory;

import com.parksmart.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Factory Pattern: Selects the appropriate PricingStrategy based on context.
 * Pricing type resolved via Spring bean name ("HOURLY", "DYNAMIC", "WEEKEND").
 */
@Component
@RequiredArgsConstructor
public class PricingStrategyFactory {

    private final ApplicationContext applicationContext;

    /**
     * Resolve strategy bean by name.
     */
    public PricingStrategy getStrategy(String strategyName) {
        return applicationContext.getBean(strategyName, PricingStrategy.class);
    }

    /**
     * Auto-resolve strategy for a given entry time.
     * Uses DYNAMIC during business hours (Mon-Fri 8am-8pm),
     * WEEKEND on Saturdays and Sundays, HOURLY otherwise.
     */
    public PricingStrategy resolveForTime(LocalDateTime entryTime) {
        var dow = entryTime.getDayOfWeek();
        var hour = entryTime.getHour();
        boolean isWeekend = (dow.getValue() >= 6);
        boolean isPeakHour = (hour >= 8 && hour < 20);

        if (isWeekend) return getStrategy("WEEKEND");
        if (isPeakHour) return getStrategy("DYNAMIC");
        return getStrategy("HOURLY");
    }
}
