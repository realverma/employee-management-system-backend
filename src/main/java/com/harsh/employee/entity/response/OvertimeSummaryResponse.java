package com.harsh.employee.entity.response;
import java.math.BigDecimal;
import java.util.List;

public record OvertimeSummaryResponse(
        Long workerId,
        String month,
        BigDecimal totalOvertimeHours,
        BigDecimal totalPayoutAmount,
        List<OvertimeEntryDetail> breakdown
) {
    public record OvertimeEntryDetail(
            String date,
            BigDecimal hours,
            BigDecimal rateApplied,
            BigDecimal amount,
            String status
    ) {}
}