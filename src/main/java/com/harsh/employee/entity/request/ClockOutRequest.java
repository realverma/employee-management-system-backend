package com.harsh.employee.entity.request;
import jakarta.validation.constraints.NotNull;

public record ClockOutRequest(

        @NotNull(message = "Worker ID cannot be null")
        Long workerId

) {
}