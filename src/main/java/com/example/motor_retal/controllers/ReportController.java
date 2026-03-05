package com.example.motor_retal.controllers;

import com.example.motor_retal.models.bookings.Booking;
import com.example.motor_retal.models.bookings.BookingStatus;
import com.example.motor_retal.models.payments.Payment;
import com.example.motor_retal.models.payments.PaymentStatus;
import com.example.motor_retal.repositories.BookingRepository;
import com.example.motor_retal.repositories.CustomerRepository;
import com.example.motor_retal.repositories.PaymentRepository;
import com.example.motor_retal.repositories.ReturnRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class ReportController {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final ReturnRepository returnRepository;

    public ReportController(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            ReturnRepository returnRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.returnRepository = returnRepository;
    }

    @GetMapping("/reports")
    public String index(Model model) {
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        BigDecimal totalBookingValue = bookings.stream()
                .map(Booking::getTotalPrice)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal outstanding = totalBookingValue.subtract(totalPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        long completedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long activeBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.ACTIVE || b.getStatus() == BookingStatus.RESERVED).count();

        Map<Long, BigDecimal> bookingPaidMap = new HashMap<>();
        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.PAID || payment.getBooking() == null || payment.getBooking().getId() == null || payment.getAmount() == null) {
                continue;
            }
            bookingPaidMap.merge(payment.getBooking().getId(), payment.getAmount(), BigDecimal::add);
        }

        List<BookingReportRow> bookingRows = bookings.stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(b -> {
                    BigDecimal total = b.getTotalPrice() == null ? BigDecimal.ZERO : b.getTotalPrice();
                    BigDecimal paid = bookingPaidMap.getOrDefault(b.getId(), BigDecimal.ZERO);
                    BigDecimal balance = total.subtract(paid).max(BigDecimal.ZERO);
                    return new BookingReportRow(
                            b.getId(),
                            b.getCustomer() != null ? b.getCustomer().getName() : "",
                            b.getMotorcycle() != null ? b.getMotorcycle().getPlateNumber() : "",
                            total.setScale(2, RoundingMode.HALF_UP),
                            paid.setScale(2, RoundingMode.HALF_UP),
                            balance.setScale(2, RoundingMode.HALF_UP),
                            b.getStatus() != null ? b.getStatus().name() : "",
                            b.getCreatedAt()
                    );
                })
                .toList();

        int currentYear = LocalDateTime.now().getYear();
        Map<Integer, Long> monthlyBookingCount = new HashMap<>();
        for (Booking booking : bookings) {
            if (booking.getCreatedAt() != null && booking.getCreatedAt().getYear() == currentYear) {
                int month = booking.getCreatedAt().getMonthValue();
                monthlyBookingCount.merge(month, 1L, Long::sum);
            }
        }

        List<MonthlyCountRow> monthlyRows = Month.values().length > 0
                ? java.util.Arrays.stream(Month.values())
                .map(m -> new MonthlyCountRow(
                        m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        monthlyBookingCount.getOrDefault(m.getValue(), 0L)
                ))
                .toList()
                : List.of();

        model.addAttribute("totalCustomers", customerRepository.count());
        model.addAttribute("totalBookings", bookingRepository.count());
        model.addAttribute("totalReturns", returnRepository.count());
        model.addAttribute("totalBookingValue", totalBookingValue);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("outstanding", outstanding);
        model.addAttribute("completedBookings", completedBookings);
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("monthlyRows", monthlyRows);
        model.addAttribute("bookingRows", bookingRows);
        return "reports/index";
    }

    public static class BookingReportRow {
        private final Long bookingId;
        private final String customerName;
        private final String plateNumber;
        private final BigDecimal totalPrice;
        private final BigDecimal paidAmount;
        private final BigDecimal balance;
        private final String status;
        private final LocalDateTime createdAt;

        public BookingReportRow(Long bookingId, String customerName, String plateNumber, BigDecimal totalPrice, BigDecimal paidAmount, BigDecimal balance, String status, LocalDateTime createdAt) {
            this.bookingId = bookingId;
            this.customerName = customerName;
            this.plateNumber = plateNumber;
            this.totalPrice = totalPrice;
            this.paidAmount = paidAmount;
            this.balance = balance;
            this.status = status;
            this.createdAt = createdAt;
        }

        public Long getBookingId() { return bookingId; }
        public String getCustomerName() { return customerName; }
        public String getPlateNumber() { return plateNumber; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public BigDecimal getBalance() { return balance; }
        public String getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class MonthlyCountRow {
        private final String month;
        private final Long count;

        public MonthlyCountRow(String month, Long count) {
            this.month = month;
            this.count = count;
        }

        public String getMonth() { return month; }
        public Long getCount() { return count; }
    }
}
