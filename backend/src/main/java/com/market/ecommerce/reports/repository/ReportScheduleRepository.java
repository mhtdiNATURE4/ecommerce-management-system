package com.market.ecommerce.reports.repository;

import com.market.ecommerce.reports.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {
    List<ReportSchedule> findByActiveTrue();
}
