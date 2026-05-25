package com.harsh.employee.entity.request;

import jakarta.validation.constraints.NotNull;

public record ClockInRequest(

        @NotNull(message = "Worker ID cannot be null")
        Long workerId,

        @NotNull(message = "Site ID cannot be null")
        Long siteId

) {
}