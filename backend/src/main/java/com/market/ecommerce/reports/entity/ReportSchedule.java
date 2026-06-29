package com.market.ecommerce.reports.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_schedules")
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ReportDefinition report;

    @Column(nullable = false)
    private String cronExpression;

    private String email;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime nextRun;
    private LocalDateTime lastRun;

    private String lastStatus;
    private Integer executionCount = 0;
    @Column(length = 2000)
    private String lastError;

    @Version
    private Long version;

    public ReportSchedule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReportDefinition getReport() { return report; }
    public void setReport(ReportDefinition report) { this.report = report; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getNextRun() { return nextRun; }
    public void setNextRun(LocalDateTime nextRun) { this.nextRun = nextRun; }
    public LocalDateTime getLastRun() { return lastRun; }
    public void setLastRun(LocalDateTime lastRun) { this.lastRun = lastRun; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public Integer getExecutionCount() { return executionCount; }
    public void setExecutionCount(Integer executionCount) { this.executionCount = executionCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
