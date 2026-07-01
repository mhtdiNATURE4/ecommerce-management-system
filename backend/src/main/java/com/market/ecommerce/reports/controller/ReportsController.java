package com.market.ecommerce.reports.controller;

import com.market.ecommerce.dto.ProductResponse;
import com.market.ecommerce.reports.dto.*;
import jakarta.validation.Valid;
import com.market.ecommerce.reports.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportDefinitionResponse> listReports() {
        return reportService.listReports();
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> getLowStock(@RequestParam(defaultValue = "10") int threshold) {
        return reportService.getLowStockProducts(threshold);
    }

    @PostMapping("/{id}/run")
    public ReportExecutionResponse runReport(@PathVariable Long id) {
        return reportService.runReport(id);
    }

    @GetMapping("/{id}/history")
    public List<ReportExecutionResponse> history(@PathVariable Long id) {
        return reportService.getHistory(id);
    }

}
