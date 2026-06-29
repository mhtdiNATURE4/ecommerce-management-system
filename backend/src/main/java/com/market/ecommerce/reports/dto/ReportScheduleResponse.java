package com.market.ecommerce.reports.dto;

import java.time.LocalDateTime;

public record ReportScheduleResponse(
        Long id,
        Long reportId,
        String cronExpression,
        String email,
        boolean active,
        LocalDateTime nextRun,
        LocalDateTime lastRun,
        String status,
        Integer executionCount,
        String lastError
) {}
