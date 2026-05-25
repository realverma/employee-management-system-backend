package com.harsh.employee.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OvertimeSettledEvent {
    private final Long workerId;
    private final String phoneNumber;
    private final BigDecimal totalSettledAmount;
    private final String billingMonth;
}