package com.market.ecommerce.reports.scheduler;

import com.market.ecommerce.reports.entity.ReportSchedule;
import com.market.ecommerce.reports.repository.ReportScheduleRepository;
import com.market.ecommerce.reports.service.ReportService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportScheduleRepository scheduleRepository;
    private final ReportService reportService;
    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<Long, Boolean> running = new ConcurrentHashMap<>();
    private final LockProvider lockProvider;

    private final Timer runTimer;

    public ReportScheduler(ReportScheduleRepository scheduleRepository, ReportService reportService, MeterRegistry meterRegistry, LockProvider lockProvider) {
        this.scheduleRepository = scheduleRepository;
        this.reportService = reportService;
        this.meterRegistry = meterRegistry;
        this.lockProvider = lockProvider;
        this.runTimer = meterRegistry.timer("reports.scheduler.run.duration");
    }

    // Poll every 30 seconds to check schedules
    @Scheduled(cron = "*/30 * * * * *")
    public void pollAndRun() {
        List<ReportSchedule> active = scheduleRepository.findByActiveTrue();
        ZonedDateTime nowZ = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDateTime now = nowZ.toLocalDateTime();

        for (ReportSchedule s : active) {
            try {
                boolean shouldRun = false;
                if (s.getNextRun() == null) {
                    // compute next from cron
                    try {
                        ZonedDateTime next = org.springframework.scheduling.support.CronExpression.parse(s.getCronExpression()).next(nowZ.plusSeconds(1));
                        if (next != null) {
                            s.setNextRun(next.toLocalDateTime());
                            scheduleRepository.save(s);
                        }
                    } catch (Exception ex) {
                        log.warn("Invalid cron for schedule {}: {}", s.getId(), ex.getMessage());
                        s.setLastStatus("INVALID_CRON");
                        s.setLastError(ex.getMessage());
                        scheduleRepository.save(s);
                        continue;
                    }
                }

                if (s.getNextRun() != null && !s.getNextRun().isAfter(now)) {
                    shouldRun = true;
                }

                if (!shouldRun) continue;

                // Distributed lock per schedule using ShedLock
                String lockName = "report-schedule-" + s.getId();
                java.time.Instant lockAtMost = java.time.Instant.now().plusSeconds(60 * 10); // 10 minutes max
                LockConfiguration lockConfig = new LockConfiguration(lockName, lockAtMost);
                SimpleLock lock = null;
                try {
                    lock = lockProvider.lock(lockConfig).orElse(null);
                } catch (Exception le) {
                    log.warn("Failed to acquire distributed lock for schedule {}: {}", s.getId(), le.getMessage());
                    lock = null;
                }

                if (lock == null) {
                    log.info("Could not acquire lock for schedule {}, skipping", s.getId());
                    continue;
                }

                try {
                    long start = System.nanoTime();
                    log.info("Acquired lock and starting schedule {} for report {}", s.getId(), s.getReport().getId());
                    var execResp = reportService.runReport(s.getReport().getId());
                    long duration = System.nanoTime() - start;
                    runTimer.record(duration, TimeUnit.NANOSECONDS);
                    meterRegistry.counter("reports.scheduler.runs.success").increment();

                    s.setLastRun(now);
                    s.setLastStatus(execResp.status());
                    s.setExecutionCount((s.getExecutionCount() == null ? 0 : s.getExecutionCount()) + 1);
                    s.setLastError(null);

                    // compute next
                    try {
                        ZonedDateTime next = org.springframework.scheduling.support.CronExpression.parse(s.getCronExpression()).next(nowZ.plusSeconds(1));
                        if (next != null) s.setNextRun(next.toLocalDateTime());
                        else s.setNextRun(null);
                    } catch (Exception ex) {
                        log.warn("Failed computing nextRun for schedule {}: {}", s.getId(), ex.getMessage());
                        s.setNextRun(null);
                    }

                    scheduleRepository.save(s);
                    log.info("Completed schedule {} in {} ms", s.getId(), TimeUnit.NANOSECONDS.toMillis(duration));
                } catch (Exception ex) {
                    long duration = 0L;
                    runTimer.record(duration, TimeUnit.NANOSECONDS);
                    meterRegistry.counter("reports.scheduler.runs.failed").increment();
                    log.error("Schedule {} failed: {}", s.getId(), ex.getMessage());
                    s.setLastRun(now);
                    s.setLastStatus("FAILED");
                    s.setExecutionCount((s.getExecutionCount() == null ? 0 : s.getExecutionCount()) + 1);
                    s.setLastError(ex.getMessage());
                    // compute next to avoid stuck schedule
                    try {
                        ZonedDateTime next = org.springframework.scheduling.support.CronExpression.parse(s.getCronExpression()).next(nowZ.plusSeconds(1));
                        if (next != null) s.setNextRun(next.toLocalDateTime());
                        else s.setNextRun(null);
                    } catch (Exception ex2) {
                        s.setNextRun(null);
                    }
                    scheduleRepository.save(s);
                } finally {
                    try {
                        lock.unlock();
                        log.info("Released lock for schedule {}", s.getId());
                    } catch (Exception ue) {
                        log.warn("Failed to release lock for schedule {}: {}", s.getId(), ue.getMessage());
                    }
                }
            } catch (Exception outer) {
                log.error("Error processing schedule {}: {}", s.getId(), outer.getMessage());
            }
        }
    }
}
