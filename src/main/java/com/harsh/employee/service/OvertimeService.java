package com.harsh.employee.service;

import com.harsh.employee.Enums.SettlementStatus;
import com.harsh.employee.entity.response.OvertimeSummaryResponse;
import com.harsh.employee.exception.BusinessException;
import com.harsh.employee.exception.ResourceNotFoundException;
import com.harsh.employee.model.OvertimeEntry;
import com.harsh.employee.model.OvertimeSettledEvent;
import com.harsh.employee.model.Worker;
import com.harsh.employee.repository.OvertimeEntryRepository;
import com.harsh.employee.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeEntryRepository overtimeRepository;
    private final WorkerRepository workerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OvertimeSummaryResponse getMonthlySummary(Long workerId, String monthStr) {
        YearMonth targetMonth = YearMonth.parse(monthStr);

        List<OvertimeEntry> entries = overtimeRepository.findAllByWorkerIdAndDateBetween(
                workerId, targetMonth.atDay(1), targetMonth.atEndOfMonth());

        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OvertimeSummaryResponse.OvertimeEntryDetail> breakdown = entries.stream().map(entry -> {
            return new OvertimeSummaryResponse.OvertimeEntryDetail(
                    entry.getDate().toString(),
                    entry.getOvertimeHours(),
                    entry.getOvertimeRateApplied(),
                    entry.getAmount(),
                    entry.getSettlementStatus().name()
            );
        }).toList();

        for (OvertimeEntry entry : entries) {
            totalHours = totalHours.add(entry.getOvertimeHours());
            totalAmount = totalAmount.add(entry.getAmount());
        }

        return new OvertimeSummaryResponse(workerId, monthStr, totalHours, totalAmount, breakdown);
    }

    @Transactional
    public Map<String, Object> settleMonthlyOvertime(Long workerId, String monthStr) {
        YearMonth targetMonth = YearMonth.parse(monthStr);

        if (targetMonth.equals(YearMonth.now()) || targetMonth.isAfter(YearMonth.now())) {
            throw new BusinessException("INVALID_SETTLEMENT", "Cannot settle current or future months.");
        }

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        List<OvertimeEntry> pendingEntries = overtimeRepository.findPendingEntries(
                workerId, targetMonth.atDay(1), targetMonth.atEndOfMonth());

        if (pendingEntries.isEmpty()) {
            throw new BusinessException("NO_PENDING_OVERTIME", "No pending overtime entries found for settlement.");
        }

        BigDecimal aggregatePayout = BigDecimal.ZERO;
        for (OvertimeEntry entry : pendingEntries) {
            entry.setSettlementStatus(SettlementStatus.SETTLED);
            aggregatePayout = aggregatePayout.add(entry.getAmount());
            overtimeRepository.save(entry);
        }

        // Fire event AFTER commit to safely notify the worker (Fixes LF-204)
        eventPublisher.publishEvent(new OvertimeSettledEvent(
                workerId, worker.getPhone(), aggregatePayout, monthStr));

        return Map.of(
                "message", "Overtime settled successfully.",
                "totalSettledAmount", aggregatePayout,
                "entriesSettled", pendingEntries.size()
        );
    }
}