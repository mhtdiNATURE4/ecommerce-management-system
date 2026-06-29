package com.market.ecommerce.reports.service;

import com.market.ecommerce.dto.ProductResponse;
import com.market.ecommerce.reports.dto.*;
import com.market.ecommerce.reports.entity.ReportDefinition;
import com.market.ecommerce.reports.entity.ReportExecution;
import com.market.ecommerce.reports.entity.ReportSchedule;

import java.io.InputStream;
import java.util.List;

public interface ReportService {
    List<ReportDefinitionResponse> listReports();
    ReportExecutionResponse runReport(Long id);
    List<ReportExecutionResponse> getHistory(Long reportId);
    byte[] downloadExecution(Long executionId);
    ReportExecutionResponse getExecution(Long executionId);
    List<ProductResponse> getLowStockProducts(int threshold);
    List<ReportScheduleResponse> listSchedules();
    ReportScheduleResponse createSchedule(Long reportId, String cron, String email);
    void pauseSchedule(Long id);
    void resumeSchedule(Long id);
    void deleteSchedule(Long id);
    ReportSummaryResponse summary();
}
