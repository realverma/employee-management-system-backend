package com.harsh.employee.repository;

import com.harsh.employee.model.AttendanceLog;
import com.harsh.employee.model.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    // Checks if a worker is currently clocked in anywhere
    Optional<AttendanceLog> findByWorkerAndClockOutTimeIsNull(Worker worker);

    // Fetches history while JOINing worker and site to prevent N+1 queries
    @EntityGraph(attributePaths = {"worker", "site"})
    Page<AttendanceLog> findByWorkerIdAndClockInTimeBetween(
            Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}