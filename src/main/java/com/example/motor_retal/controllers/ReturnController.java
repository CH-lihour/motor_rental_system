package com.example.motor_retal.controllers;

import com.example.motor_retal.models.bookings.Booking;
import com.example.motor_retal.models.bookings.BookingStatus;
import com.example.motor_retal.models.employees.Employee;
import com.example.motor_retal.models.motorcycles.Motorcycle;
import com.example.motor_retal.models.motorcycles.MotorcycleStatus;
import com.example.motor_retal.models.returns.Return;
import com.example.motor_retal.repositories.BookingRepository;
import com.example.motor_retal.repositories.EmployeeRepository;
import com.example.motor_retal.repositories.MotorcycleRepository;
import com.example.motor_retal.repositories.ReturnRepository;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ReturnController {
    private final ReturnRepository returnRepository;
    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final MotorcycleRepository motorcycleRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public ReturnController(
            ReturnRepository returnRepository,
            BookingRepository bookingRepository,
            EmployeeRepository employeeRepository,
            MotorcycleRepository motorcycleRepository
    ) {
        this.returnRepository = returnRepository;
        this.bookingRepository = bookingRepository;
        this.employeeRepository = employeeRepository;
        this.motorcycleRepository = motorcycleRepository;
    }

    @GetMapping("/returns")
    public String index(Model model) {
        model.addAttribute("returns", returnRepository.findAll());
        return "returns/index";
    }

    @GetMapping("/returns/add")
    public String create(Model model) {
        addFormDependencies(model, null);
        return "returns/create";
    }

    @PostMapping("/returns/add")
    public String store(
            @RequestParam Long booking_id,
            @RequestParam Long handled_by,
            @RequestParam String return_time,
            @RequestParam(required = false) BigDecimal late_fee,
            @RequestParam(required = false) BigDecimal damage_fee,
            @RequestParam(required = false) String note,
            RedirectAttributes ra
    ) {
        Booking booking = bookingRepository.findById(booking_id).orElse(null);
        Employee handledBy = employeeRepository.findById(handled_by).orElse(null);

        if (booking == null || handledBy == null) {
            ra.addFlashAttribute("errorMessage", "Invalid booking or employee.");
            return "redirect:/returns/add";
        }

        if (returnRepository.existsByBookingId(booking_id)) {
            ra.addFlashAttribute("errorMessage", "This booking already has a return record.");
            return "redirect:/returns/add";
        }

        LocalDateTime returnTime;
        try {
            returnTime = LocalDateTime.parse(return_time, DATETIME_FORMATTER);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Invalid return time.");
            return "redirect:/returns/add";
        }

        BigDecimal resolvedLateFee = late_fee == null ? BigDecimal.ZERO : late_fee;
        BigDecimal resolvedDamageFee = damage_fee == null ? BigDecimal.ZERO : damage_fee;
        if (resolvedLateFee.signum() < 0 || resolvedDamageFee.signum() < 0) {
            ra.addFlashAttribute("errorMessage", "Fees cannot be negative.");
            return "redirect:/returns/add";
        }

        String resolvedNote = (note != null && !note.isBlank()) ? note.trim() : null;

        Return returnRecord = new Return();
        returnRecord.setBooking(booking);
        returnRecord.setHandledBy(handledBy);
        returnRecord.setReturnTime(returnTime);
        returnRecord.setLateFee(resolvedLateFee.setScale(2, RoundingMode.HALF_UP));
        returnRecord.setDamageFee(resolvedDamageFee.setScale(2, RoundingMode.HALF_UP));
        returnRecord.setNote(resolvedNote);

        try {
            returnRepository.save(returnRecord);
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            Motorcycle motorcycle = booking.getMotorcycle();
            if (motorcycle != null) {
                motorcycle.setStatus(MotorcycleStatus.AVAILABLE);
                motorcycleRepository.save(motorcycle);
            }
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not save return. Please verify your input.");
            return "redirect:/returns/add";
        }

        ra.addFlashAttribute("successMessage", "Return created successfully.");
        return "redirect:/returns";
    }

    @GetMapping("/returns/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Return returnRecord = returnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Return not found"));
        model.addAttribute("returnRecord", returnRecord);
        addFormDependencies(model, returnRecord.getBooking().getId());
        return "returns/edit";
    }

    @PostMapping("/returns/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam Long booking_id,
            @RequestParam Long handled_by,
            @RequestParam String return_time,
            @RequestParam(required = false) BigDecimal late_fee,
            @RequestParam(required = false) BigDecimal damage_fee,
            @RequestParam(required = false) String note,
            RedirectAttributes ra
    ) {
        Return returnRecord = returnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Return not found"));

        Booking booking = bookingRepository.findById(booking_id).orElse(null);
        Employee handledBy = employeeRepository.findById(handled_by).orElse(null);

        if (booking == null || handledBy == null) {
            ra.addFlashAttribute("errorMessage", "Invalid booking or employee.");
            return "redirect:/returns/edit/" + id;
        }

        if (returnRepository.findByBookingId(booking_id).filter(existing -> !existing.getId().equals(id)).isPresent()) {
            ra.addFlashAttribute("errorMessage", "This booking already has another return record.");
            return "redirect:/returns/edit/" + id;
        }

        LocalDateTime returnTime;
        try {
            returnTime = LocalDateTime.parse(return_time, DATETIME_FORMATTER);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Invalid return time.");
            return "redirect:/returns/edit/" + id;
        }

        BigDecimal resolvedLateFee = late_fee == null ? BigDecimal.ZERO : late_fee;
        BigDecimal resolvedDamageFee = damage_fee == null ? BigDecimal.ZERO : damage_fee;
        if (resolvedLateFee.signum() < 0 || resolvedDamageFee.signum() < 0) {
            ra.addFlashAttribute("errorMessage", "Fees cannot be negative.");
            return "redirect:/returns/edit/" + id;
        }

        String resolvedNote = (note != null && !note.isBlank()) ? note.trim() : null;

        returnRecord.setBooking(booking);
        returnRecord.setHandledBy(handledBy);
        returnRecord.setReturnTime(returnTime);
        returnRecord.setLateFee(resolvedLateFee.setScale(2, RoundingMode.HALF_UP));
        returnRecord.setDamageFee(resolvedDamageFee.setScale(2, RoundingMode.HALF_UP));
        returnRecord.setNote(resolvedNote);

        try {
            returnRepository.save(returnRecord);
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            Motorcycle motorcycle = booking.getMotorcycle();
            if (motorcycle != null) {
                motorcycle.setStatus(MotorcycleStatus.AVAILABLE);
                motorcycleRepository.save(motorcycle);
            }
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not update return. Please verify your input.");
            return "redirect:/returns/edit/" + id;
        }

        ra.addFlashAttribute("successMessage", "Return updated successfully.");
        return "redirect:/returns";
    }

    @GetMapping("/returns/delete/{id}")
    public String destroy(@PathVariable Long id, RedirectAttributes ra) {
        if (!returnRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "Return not found.");
            return "redirect:/returns";
        }

        returnRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Return deleted successfully.");
        return "redirect:/returns";
    }

    private void addFormDependencies(Model model, Long includeBookingId) {
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> availableBookings = allBookings.stream()
                .filter(b -> includeBookingId != null && b.getId().equals(includeBookingId)
                        || !returnRepository.existsByBookingId(b.getId()))
                .toList();

        model.addAttribute("bookings", availableBookings);
        model.addAttribute("employees", employeeRepository.findAll());
    }
}
