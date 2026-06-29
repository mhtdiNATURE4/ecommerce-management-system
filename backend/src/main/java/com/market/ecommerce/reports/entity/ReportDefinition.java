package com.market.ecommerce.reports.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "report_definitions")
public class ReportDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private boolean enabled = true;

    public ReportDefinition() {}

    public ReportDefinition(String name, String description, String reportType, boolean enabled) {
        this.name = name;
        this.description = description;
        this.reportType = reportType;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
