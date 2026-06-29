package com.market.ecommerce.reports.repository;

import com.market.ecommerce.reports.entity.ReportExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportExecutionRepository extends JpaRepository<ReportExecution, Long> {
    List<ReportExecution> findByReportIdOrderByCreatedAtDesc(Long reportId);

    @Query("select r from ReportExecution r where r.report.id = :reportId order by r.createdAt desc")
    List<ReportExecution> findByReportId(@Param("reportId") Long reportId);
}
