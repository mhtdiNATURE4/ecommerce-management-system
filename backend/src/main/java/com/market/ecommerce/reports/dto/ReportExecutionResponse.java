package com.market.ecommerce.reports.dto;

import java.time.LocalDateTime;

public record ReportExecutionResponse(
        Long id,
        Long reportId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String fileName,
        Long fileSize
) {}
