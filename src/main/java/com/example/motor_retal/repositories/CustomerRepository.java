package com.example.motor_retal.repositories;

import com.example.motor_retal.models.customers.Customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long>{
    
}
