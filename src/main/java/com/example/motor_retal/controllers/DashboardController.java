package com.example.motor_retal.controllers;

import com.example.motor_retal.models.bookings.Booking;
import com.example.motor_retal.models.bookings.BookingStatus;
import com.example.motor_retal.models.motorcycles.MotorcycleStatus;
import com.example.motor_retal.models.payments.PaymentStatus;
import com.example.motor_retal.repositories.BookingRepository;
import com.example.motor_retal.repositories.CustomerRepository;
import com.example.motor_retal.repositories.MotorcycleRepository;
import com.example.motor_retal.repositories.PaymentRepository;
import com.example.motor_retal.repositories.ReturnRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;


@Controller
public class DashboardController {
    private final CustomerRepository customerRepository;
    private final MotorcycleRepository motorcycleRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReturnRepository returnRepository;

    public DashboardController(
            CustomerRepository customerRepository,
            MotorcycleRepository motorcycleRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            ReturnRepository returnRepository
    ) {
        this.customerRepository = customerRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.returnRepository = returnRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        long totalCustomers = customerRepository.count();
        long totalMotorcycles = motorcycleRepository.count();
        long totalBookings = bookingRepository.count();
        long totalReturns = returnRepository.count();

        long availableMotorcycles = motorcycleRepository.findAll().stream()
                .filter(m -> m.getStatus() == MotorcycleStatus.AVAILABLE)
                .count();

        long activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.ACTIVE || b.getStatus() == BookingStatus.RESERVED)
                .count();

        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && p.getAmount() != null)
                .map(p -> p.getAmount().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Booking> recentBookings = bookingRepository.findAll().stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalMotorcycles", totalMotorcycles);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalReturns", totalReturns);
        model.addAttribute("availableMotorcycles", availableMotorcycles);
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBookings", recentBookings);

        return "index";
    }
    
}
