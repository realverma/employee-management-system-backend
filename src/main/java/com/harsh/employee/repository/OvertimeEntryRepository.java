package com.harsh.employee.repository;

import com.harsh.employee.model.OvertimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OvertimeEntryRepository extends JpaRepository<OvertimeEntry, Long> {

    // Aggregates total overtime hours for a worker in a specific month to enforce the 60-hour cap
    @Query("SELECT SUM(o.overtimeHours) FROM OvertimeEntry o WHERE o.worker.id = :workerId AND o.date >= :startDate AND o.date <= :endDate")
    BigDecimal getSumOfOvertimeHoursForMonth(
            @Param("workerId") Long workerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Fetches all pending entries for a specific month for the settlement batch
    @Query("SELECT o FROM OvertimeEntry o WHERE o.worker.id = :workerId AND o.date >= :startDate AND o.date <= :endDate AND o.settlementStatus = 'PENDING'")
    List<OvertimeEntry> findPendingEntries(
            @Param("workerId") Long workerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<OvertimeEntry> findAllByWorkerIdAndDateBetween(Long workerId, LocalDate start, LocalDate end);
}