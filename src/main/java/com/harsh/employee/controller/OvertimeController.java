package com.harsh.employee.controller;

import com.harsh.employee.entity.response.OvertimeSummaryResponse;
import com.harsh.employee.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/summary/{workerId}")
    public ResponseEntity<OvertimeSummaryResponse> getSummary(
            @PathVariable Long workerId,
            @RequestParam String month) {
        return ResponseEntity.ok(overtimeService.getMonthlySummary(workerId, month));
    }

    @PostMapping("/settle/{workerId}")
    public ResponseEntity<Map<String, Object>> settleOvertime(
            @PathVariable Long workerId,
            @RequestParam String month) {
        return ResponseEntity.ok(overtimeService.settleMonthlyOvertime(workerId, month));
    }
}