package com.example.motor_retal.repositories;

import com.example.motor_retal.models.returns.Return;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRepository extends JpaRepository<Return, Long> {
    boolean existsByBookingId(Long bookingId);
    Optional<Return> findByBookingId(Long bookingId);
}
