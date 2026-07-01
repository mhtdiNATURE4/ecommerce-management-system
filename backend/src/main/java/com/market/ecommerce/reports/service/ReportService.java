package com.market.ecommerce.reports.service;

import com.market.ecommerce.dto.ProductResponse;
import com.market.ecommerce.reports.dto.*;

import java.util.List;

public interface ReportService {
    List<ReportDefinitionResponse> listReports();
    ReportExecutionResponse runReport(Long id);
    List<ReportExecutionResponse> getHistory(Long reportId);
    List<ProductResponse> getLowStockProducts(int threshold);
}
