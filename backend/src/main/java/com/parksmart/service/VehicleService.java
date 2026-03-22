package com.parksmart.service;

import com.parksmart.entity.Vehicle;
import com.parksmart.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByUser(UUID userId) {
        return vehicleRepository.findByOwnerId(userId);
    }
}
