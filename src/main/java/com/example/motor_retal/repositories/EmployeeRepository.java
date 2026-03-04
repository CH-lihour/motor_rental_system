package com.example.motor_retal.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.motor_retal.models.employees.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long>{
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findTopByOrderByIdDesc();
}
