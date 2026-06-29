package com.market.ecommerce.reports.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScheduleRequest(
        @NotNull Long reportId,
        @NotBlank String cron,
        @Email(message = "Invalid email") String email
) {}
