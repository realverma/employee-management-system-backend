package com.harsh.employee.controller;

import com.harsh.employee.entity.response.AttendanceLogDto;
import com.harsh.employee.entity.response.PaginatedResponse;
import com.harsh.employee.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<?> clockIn(@RequestBody Map<String, Long> payload) {
        attendanceService.clockIn(payload.get("workerId"), payload.get("siteId"));
        return ResponseEntity.ok(Map.of("message", "Clock-in successful."));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<?> clockOut(@RequestBody Map<String, Long> payload) {
        attendanceService.clockOut(payload.get("workerId"));
        return ResponseEntity.ok(Map.of("message", "Clock-out registered successfully."));
    }

    @GetMapping("/active")
    public ResponseEntity<List<Object>> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getActiveWorkers());
    }

    @GetMapping("/log")
    public ResponseEntity<PaginatedResponse<AttendanceLogDto>> getAttendanceLog(
            @RequestParam Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(attendanceService.getAttendanceHistory(workerId, from, to, page, size));
    }
}