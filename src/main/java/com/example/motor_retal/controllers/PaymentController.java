package com.example.motor_retal.controllers;

import com.example.motor_retal.models.bookings.Booking;
import com.example.motor_retal.models.payments.Payment;
import com.example.motor_retal.models.payments.PaymentMethod;
import com.example.motor_retal.models.payments.PaymentStatus;
import com.example.motor_retal.models.payments.PaymentType;
import com.example.motor_retal.repositories.BookingRepository;
import com.example.motor_retal.repositories.PaymentRepository;
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

@Controller
public class PaymentController {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentController(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/payments")
    public String index(Model model) {
        model.addAttribute("payments", paymentRepository.findAll());
        return "payments/index";
    }

    @GetMapping("/payments/add")
    public String create(Model model) {
        addFormDependencies(model);
        return "payments/create";
    }

    @PostMapping("/payments/add")
    public String store(
            @RequestParam Long booking_id,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentType payment_type,
            @RequestParam PaymentMethod method,
            @RequestParam(defaultValue = "PAID") PaymentStatus status,
            RedirectAttributes ra
    ) {
        Booking booking = bookingRepository.findById(booking_id).orElse(null);
        if (booking == null) {
            ra.addFlashAttribute("errorMessage", "Invalid booking.");
            return "redirect:/payments/add";
        }

        if (amount == null || amount.signum() <= 0) {
            ra.addFlashAttribute("errorMessage", "Amount must be a positive number.");
            return "redirect:/payments/add";
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setPaymentType(payment_type);
        payment.setMethod(method);
        payment.setStatus(status);

        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not save payment. Please verify your input.");
            return "redirect:/payments/add";
        }

        ra.addFlashAttribute("successMessage", "Payment created successfully.");
        return "redirect:/payments";
    }

    @GetMapping("/payments/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        model.addAttribute("payment", payment);
        addFormDependencies(model);
        return "payments/edit";
    }

    @PostMapping("/payments/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam Long booking_id,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentType payment_type,
            @RequestParam PaymentMethod method,
            @RequestParam(defaultValue = "PAID") PaymentStatus status,
            RedirectAttributes ra
    ) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Booking booking = bookingRepository.findById(booking_id).orElse(null);
        if (booking == null) {
            ra.addFlashAttribute("errorMessage", "Invalid booking.");
            return "redirect:/payments/edit/" + id;
        }

        if (amount == null || amount.signum() <= 0) {
            ra.addFlashAttribute("errorMessage", "Amount must be a positive number.");
            return "redirect:/payments/edit/" + id;
        }

        payment.setBooking(booking);
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setPaymentType(payment_type);
        payment.setMethod(method);
        payment.setStatus(status);

        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not update payment. Please verify your input.");
            return "redirect:/payments/edit/" + id;
        }

        ra.addFlashAttribute("successMessage", "Payment updated successfully.");
        return "redirect:/payments";
    }

    @GetMapping("/payments/delete/{id}")
    public String destroy(@PathVariable Long id, RedirectAttributes ra) {
        if (!paymentRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "Payment not found.");
            return "redirect:/payments";
        }

        paymentRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Payment deleted successfully.");
        return "redirect:/payments";
    }

    private void addFormDependencies(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        model.addAttribute("types", PaymentType.values());
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("statuses", PaymentStatus.values());
    }
}
