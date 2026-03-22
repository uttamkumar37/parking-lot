package com.parksmart.factory;

import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.entity.Vehicle.VehicleType;
import org.springframework.stereotype.Component;

/**
 * Factory Pattern: Maps VehicleType → compatible SlotType.
 * Applies a slot compatibility matrix (e.g., TRUCK needs LARGE).
 */
@Component
public class VehicleSlotFactory {

    /**
     * Returns the appropriate SlotType for the given VehicleType.
     * Smaller vehicles can occupy bigger slots if needed (handled at allocation level).
     */
    public SlotType resolveSlotType(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MOTORCYCLE              -> SlotType.SMALL;
            case CAR, SUV               -> SlotType.MEDIUM;
            case ELECTRIC_CAR           -> SlotType.EV;
            case TRUCK                  -> SlotType.LARGE;
            case BUS                    -> SlotType.OVERSIZED;
        };
    }

    /**
     * Returns fallback SlotTypes in priority order when preferred is unavailable.
     * E.g., a CAR can fall back to LARGE if no MEDIUM is available.
     */
    public SlotType[] getFallbackSlotTypes(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MOTORCYCLE  -> new SlotType[]{ SlotType.SMALL,  SlotType.MEDIUM };
            case CAR, SUV    -> new SlotType[]{ SlotType.MEDIUM, SlotType.LARGE  };
            case ELECTRIC_CAR-> new SlotType[]{ SlotType.EV,     SlotType.MEDIUM };
            case TRUCK       -> new SlotType[]{ SlotType.LARGE,  SlotType.OVERSIZED };
            case BUS         -> new SlotType[]{ SlotType.OVERSIZED };
        };
    }
}
