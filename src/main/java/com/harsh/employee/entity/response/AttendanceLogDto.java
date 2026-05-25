package com.harsh.employee.entity.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AttendanceLogDto(
        Long attendanceId,
        String workerName,
        String siteName,
        LocalDateTime clockInTime,
        LocalDateTime clockOutTime,
        BigDecimal totalHours,
        BigDecimal overtimeHours,
        boolean isFlagged
) {}