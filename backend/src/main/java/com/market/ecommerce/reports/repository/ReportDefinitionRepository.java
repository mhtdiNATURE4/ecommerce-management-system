package com.market.ecommerce.reports.repository;

import com.market.ecommerce.reports.entity.ReportDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {
	boolean existsByReportType(String reportType);
	java.util.Optional<ReportDefinition> findByName(String name);
}
