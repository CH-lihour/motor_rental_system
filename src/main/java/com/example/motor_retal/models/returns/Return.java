package com.example.motor_retal.models.returns;

import com.example.motor_retal.models.bookings.Booking;
import com.example.motor_retal.models.employees.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One return → One booking (booking_id UNIQUE)
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // Many returns → One employee
    @ManyToOne
    @JoinColumn(name = "handled_by", nullable = false)
    private Employee handledBy;

    @Column(name = "return_time", nullable = false)
    private LocalDateTime returnTime;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "damage_fee", precision = 10, scale = 2)
    private BigDecimal damageFee = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
