package com.harsh.employee.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs", indexes = {
        @Index(name = "idx_attendance_worker_time", columnList = "worker_id, clock_in_time")
})
@Setter @Getter
public class AttendanceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "clock_in_time", nullable = false)
    private LocalDateTime clockInTime;

    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;

    @Column(name = "total_hours", precision = 5, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    private BigDecimal overtimeHours;

    @Column(nullable = false)
    private boolean flagged = false;

    // Getters, Setters, Constructors
}