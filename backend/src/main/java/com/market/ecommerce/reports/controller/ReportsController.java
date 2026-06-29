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

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ReportExecutionResponse exec = reportService.getExecution(id);
        byte[] data = reportService.downloadExecution(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = exec.fileName() != null ? exec.fileName() : "report-download";
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @GetMapping("/schedules")
    public List<ReportScheduleResponse> listSchedules() {
        return reportService.listSchedules();
    }

    @PostMapping("/schedules")
    public ResponseEntity<ReportScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        var schedule = reportService.createSchedule(request.reportId(), request.cron(), request.email());
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/schedules/{id}/pause")
    public ResponseEntity<Void> pause(@PathVariable Long id) {
        reportService.pauseSchedule(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/schedules/{id}/resume")
    public ResponseEntity<Void> resume(@PathVariable Long id) {
        reportService.resumeSchedule(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        reportService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ReportSummaryResponse summary() {
        return reportService.summary();
    }
}
