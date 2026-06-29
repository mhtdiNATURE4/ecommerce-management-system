package com.market.ecommerce.reports.config;

import com.market.ecommerce.reports.entity.ReportDefinition;
import com.market.ecommerce.reports.repository.ReportDefinitionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ReportDataInitializer implements ApplicationRunner {

    private final ReportDefinitionRepository repo;

    public ReportDataInitializer(ReportDefinitionRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Migrate legacy seeded definitions into meaningful MIS reports without creating duplicates.

        // 1) Migrate or update existing "Top Products" -> "Top Products Report" with type TOP_PRODUCTS
        var topOpt = repo.findByName("Top Products");
        if (topOpt.isPresent()) {
            ReportDefinition d = topOpt.get();
            d.setName("Top Products Report");
            d.setReportType("TOP_PRODUCTS");
            d.setDescription("Top selling products with quantity and revenue");
            d.setEnabled(true);
            repo.save(d);
        } else if (!repo.existsByReportType("TOP_PRODUCTS")) {
            repo.save(new ReportDefinition("Top Products Report", "Top selling products with quantity and revenue", "TOP_PRODUCTS", true));
        }

        // 2) Migrate or disable "Executive Summary"
        var execOpt = repo.findByName("Executive Summary");
        if (execOpt.isPresent()) {
            ReportDefinition d = execOpt.get();
            // Safe to migrate only if there is no existing WEEKLY_SALES reportType present
            if (!repo.existsByReportType("WEEKLY_SALES")) {
                d.setName("Weekly Sales Report");
                d.setReportType("WEEKLY_SALES");
                d.setDescription("Weekly sales metrics including top products");
                d.setEnabled(true);
                repo.save(d);
            } else {
                // Migration would collide; disable legacy Executive Summary to avoid duplicates
                d.setEnabled(false);
                repo.save(d);
            }
        } else if (!repo.existsByReportType("WEEKLY_SALES")) {
            repo.save(new ReportDefinition("Weekly Sales Report", "Weekly sales metrics including top products", "WEEKLY_SALES", true));
        }

        // 3) Ensure Customer Segmentation report exists
        if (!repo.existsByReportType("CUSTOMER_SEGMENTATION")) {
            repo.save(new ReportDefinition("Customer Segmentation Report", "Customers grouped into VIP/REGULAR/LOW_VALUE segments", "CUSTOMER_SEGMENTATION", true));
        }

        // 4) Ensure Low Stock Alert report exists
        if (!repo.existsByReportType("LOW_STOCK")) {
            repo.save(new ReportDefinition("Low Stock Alert Report", "Products with stock at or below threshold", "LOW_STOCK", true));
        }
    }
}
