package com.market.ecommerce.reports.dto;

import java.time.LocalDateTime;

public record ReportSummaryResponse(
        Long totalScheduled,
        Long successful,
        Long failed,
        LocalDateTime lastGenerated,
        LocalDateTime nextScheduled,
        Double successRate
) {}
