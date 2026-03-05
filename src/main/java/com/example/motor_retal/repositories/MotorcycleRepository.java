package com.example.motor_retal.repositories;

import com.example.motor_retal.models.motorcycles.Motorcycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MotorcycleRepository extends JpaRepository<Motorcycle, Long> {
    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    Optional<Motorcycle> findByPlateNumberIgnoreCase(String plateNumber);
}
