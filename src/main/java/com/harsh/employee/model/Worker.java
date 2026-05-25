package com.harsh.employee.model;

import com.harsh.employee.Enums.Designation;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "workers", indexes = {
        @Index(name = "idx_worker_phone", columnList = "phone"),
        @Index(name = "idx_worker_status", columnList = "is_active")
})
@Data
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Designation designation;

    @Column(name = "daily_wage_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyWageRate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Getters, Setters, Constructors
}
