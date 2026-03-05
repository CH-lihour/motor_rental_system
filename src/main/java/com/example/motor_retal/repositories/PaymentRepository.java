package com.example.motor_retal.repositories;

import com.example.motor_retal.models.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
