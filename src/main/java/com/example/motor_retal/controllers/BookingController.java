package com.example.motor_retal.controllers;

import com.example.motor_retal.models.bookings.Booking;
import com.example.motor_retal.models.bookings.BookingStatus;
import com.example.motor_retal.models.customers.Customer;
import com.example.motor_retal.models.employees.Employee;
import com.example.motor_retal.models.motorcycles.Motorcycle;
import com.example.motor_retal.models.motorcycles.MotorcycleStatus;
import com.example.motor_retal.repositories.BookingRepository;
import com.example.motor_retal.repositories.CustomerRepository;
import com.example.motor_retal.repositories.EmployeeRepository;
import com.example.motor_retal.repositories.MotorcycleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class BookingController {
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final MotorcycleRepository motorcycleRepository;
    private final EmployeeRepository employeeRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public BookingController(
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            MotorcycleRepository motorcycleRepository,
            EmployeeRepository employeeRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/bookings")
    public String index(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "bookings/index";
    }

    @GetMapping("/bookings/add")
    public String create(Model model) {
        addFormDependencies(model);
        return "bookings/create";
    }

    @PostMapping("/bookings/add")
    public String store(
            @RequestParam Long customer_id,
            @RequestParam Long motorcycle_id,
            @RequestParam Long handled_by,
            @RequestParam String start_time,
            @RequestParam String end_time,
            @RequestParam(required = false) BigDecimal daily_price,
            @RequestParam(required = false) BigDecimal deposit,
            @RequestParam BookingStatus status,
            RedirectAttributes ra
    ) {
        Customer customer = customerRepository.findById(customer_id).orElse(null);
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycle_id).orElse(null);
        Employee handledBy = employeeRepository.findById(handled_by).orElse(null);

        if (customer == null || motorcycle == null || handledBy == null) {
            ra.addFlashAttribute("errorMessage", "Invalid customer, motorcycle, or employee.");
            return "redirect:/bookings/add";
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.parse(start_time, DATETIME_FORMATTER);
            endTime = LocalDateTime.parse(end_time, DATETIME_FORMATTER);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Invalid start or end time.");
            return "redirect:/bookings/add";
        }

        if (!endTime.isAfter(startTime)) {
            ra.addFlashAttribute("errorMessage", "End time must be after start time.");
            return "redirect:/bookings/add";
        }

        BigDecimal resolvedDailyPrice = daily_price != null ? daily_price : motorcycle.getDailyPrice();
        if (resolvedDailyPrice == null || resolvedDailyPrice.signum() <= 0) {
            ra.addFlashAttribute("errorMessage", "Daily price must be a positive number.");
            return "redirect:/bookings/add";
        }

        BigDecimal resolvedDeposit = deposit == null ? BigDecimal.ZERO : deposit;
        if (resolvedDeposit.signum() < 0) {
            ra.addFlashAttribute("errorMessage", "Deposit cannot be negative.");
            return "redirect:/bookings/add";
        }

        int totalDays = calculateTotalDays(startTime, endTime);
        BigDecimal totalPrice = resolvedDailyPrice
                .multiply(BigDecimal.valueOf(totalDays))
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setMotorcycle(motorcycle);
        booking.setHandledBy(handledBy);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDailyPrice(resolvedDailyPrice.setScale(2, RoundingMode.HALF_UP));
        booking.setTotalDays(totalDays);
        booking.setTotalPrice(totalPrice);
        booking.setDeposit(resolvedDeposit.setScale(2, RoundingMode.HALF_UP));
        booking.setStatus(status);

        try {
            bookingRepository.save(booking);
            updateMotorcycleStatusForBooking(motorcycle, status);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not save booking. Please verify your input.");
            return "redirect:/bookings/add";
        }

        ra.addFlashAttribute("successMessage", "Booking created successfully.");
        return "redirect:/bookings";
    }

    @GetMapping("/bookings/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        model.addAttribute("booking", booking);
        addFormDependencies(model);
        return "bookings/edit";
    }

    @PostMapping("/bookings/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam Long customer_id,
            @RequestParam Long motorcycle_id,
            @RequestParam Long handled_by,
            @RequestParam String start_time,
            @RequestParam String end_time,
            @RequestParam(required = false) BigDecimal daily_price,
            @RequestParam(required = false) BigDecimal deposit,
            @RequestParam BookingStatus status,
            RedirectAttributes ra
    ) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Customer customer = customerRepository.findById(customer_id).orElse(null);
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycle_id).orElse(null);
        Employee handledBy = employeeRepository.findById(handled_by).orElse(null);

        if (customer == null || motorcycle == null || handledBy == null) {
            ra.addFlashAttribute("errorMessage", "Invalid customer, motorcycle, or employee.");
            return "redirect:/bookings/edit/" + id;
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.parse(start_time, DATETIME_FORMATTER);
            endTime = LocalDateTime.parse(end_time, DATETIME_FORMATTER);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Invalid start or end time.");
            return "redirect:/bookings/edit/" + id;
        }

        if (!endTime.isAfter(startTime)) {
            ra.addFlashAttribute("errorMessage", "End time must be after start time.");
            return "redirect:/bookings/edit/" + id;
        }

        BigDecimal resolvedDailyPrice = daily_price != null ? daily_price : motorcycle.getDailyPrice();
        if (resolvedDailyPrice == null || resolvedDailyPrice.signum() <= 0) {
            ra.addFlashAttribute("errorMessage", "Daily price must be a positive number.");
            return "redirect:/bookings/edit/" + id;
        }

        BigDecimal resolvedDeposit = deposit == null ? BigDecimal.ZERO : deposit;
        if (resolvedDeposit.signum() < 0) {
            ra.addFlashAttribute("errorMessage", "Deposit cannot be negative.");
            return "redirect:/bookings/edit/" + id;
        }

        int totalDays = calculateTotalDays(startTime, endTime);
        BigDecimal totalPrice = resolvedDailyPrice
                .multiply(BigDecimal.valueOf(totalDays))
                .setScale(2, RoundingMode.HALF_UP);

        booking.setCustomer(customer);
        booking.setMotorcycle(motorcycle);
        booking.setHandledBy(handledBy);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDailyPrice(resolvedDailyPrice.setScale(2, RoundingMode.HALF_UP));
        booking.setTotalDays(totalDays);
        booking.setTotalPrice(totalPrice);
        booking.setDeposit(resolvedDeposit.setScale(2, RoundingMode.HALF_UP));
        booking.setStatus(status);

        try {
            bookingRepository.save(booking);
            updateMotorcycleStatusForBooking(motorcycle, status);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not update booking. Please verify your input.");
            return "redirect:/bookings/edit/" + id;
        }

        ra.addFlashAttribute("successMessage", "Booking updated successfully.");
        return "redirect:/bookings";
    }

    @GetMapping("/bookings/delete/{id}")
    public String destroy(@PathVariable Long id, RedirectAttributes ra) {
        if (!bookingRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        bookingRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Booking deleted successfully.");
        return "redirect:/bookings";
    }

    private void addFormDependencies(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("motorcycles", motorcycleRepository.findAll());
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("statuses", BookingStatus.values());
    }

    private int calculateTotalDays(LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        int days = (int) Math.ceil(minutes / (24d * 60d));
        return Math.max(days, 1);
    }

    private void updateMotorcycleStatusForBooking(Motorcycle motorcycle, BookingStatus bookingStatus) {
        if (bookingStatus == BookingStatus.RESERVED || bookingStatus == BookingStatus.ACTIVE) {
            motorcycle.setStatus(MotorcycleStatus.RENTED);
        } else {
            motorcycle.setStatus(MotorcycleStatus.AVAILABLE);
        }
        motorcycleRepository.save(motorcycle);
    }
}
