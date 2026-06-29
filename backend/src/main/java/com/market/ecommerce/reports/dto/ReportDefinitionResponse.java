package com.market.ecommerce.reports.dto;

public record ReportDefinitionResponse(
        Long id,
        String name,
        String description,
        String reportType,
        boolean enabled
) {}
