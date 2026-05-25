package com.harsh.employee.model;

import com.harsh.employee.Enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "overtime_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_overtime", columnNames = "attendance_id")
}, indexes = {
        @Index(name = "idx_overtime_worker_date", columnList = "worker_id, date")
})
@Data
public class OvertimeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceLog attendance;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "overtime_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeHours;

    @Column(name = "overtime_rate_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeRateApplied;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 20)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    // Getters, Setters, Constructors
}
