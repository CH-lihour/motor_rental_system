package com.example.motor_retal.repositories;

import com.example.motor_retal.models.bookings.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
