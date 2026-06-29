package com.market.ecommerce.reports.service.impl;

import com.market.ecommerce.reports.dto.*;
import com.market.ecommerce.reports.entity.ReportDefinition;
import com.market.ecommerce.reports.entity.ReportExecution;
import com.market.ecommerce.reports.entity.ReportSchedule;
import com.market.ecommerce.reports.repository.ReportDefinitionRepository;
import com.market.ecommerce.reports.repository.ReportExecutionRepository;
import com.market.ecommerce.reports.repository.ReportScheduleRepository;
import com.market.ecommerce.reports.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;

import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.OrderItemRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderItem;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.service.CustomerSegmentationService;
import com.market.ecommerce.service.ProductService;
import com.market.ecommerce.dto.CustomerSegmentResponse;
import com.market.ecommerce.dto.ProductResponse;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportDefinitionRepository definitionRepository;
    private final ReportScheduleRepository scheduleRepository;
    private final ReportExecutionRepository executionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerSegmentationService segmentationService;
    private final ProductService productService;

    public ReportServiceImpl(ReportDefinitionRepository definitionRepository,
                             ReportScheduleRepository scheduleRepository,
                             ReportExecutionRepository executionRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             ProductRepository productRepository,
                             CustomerSegmentationService segmentationService,
                             ProductService productService) {
        this.definitionRepository = definitionRepository;
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.segmentationService = segmentationService;
        this.productService = productService;
    }

    @Override
    public List<ReportDefinitionResponse> listReports() {
        return definitionRepository.findAll().stream()
                .map(d -> new ReportDefinitionResponse(d.getId(), d.getName(), d.getDescription(), d.getReportType(), d.isEnabled()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReportExecutionResponse runReport(Long id) {
        ReportDefinition def = definitionRepository.findById(id).orElseThrow();

        ReportExecution exec = new ReportExecution();
        exec.setReport(def);
        exec.setStatus("RUNNING");
        exec.setCreatedAt(LocalDateTime.now());
        executionRepository.save(exec);

        try {
            Path reportsDir = Path.of(System.getProperty("user.dir"), "reports");
            Files.createDirectories(reportsDir);
            String fileName = String.format("%s-%d-%d.csv", def.getReportType().toLowerCase(), id, System.currentTimeMillis());
            Path out = reportsDir.resolve(fileName);

            StringBuilder sb = new StringBuilder();

            switch (def.getReportType()) {
                case "WEEKLY_SALES": {
                    LocalDate today = LocalDate.now();
                    LocalDate startOfLastWeek = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
                    LocalDateTime startDt = startOfLastWeek.atStartOfDay();
                    LocalDateTime endDt = endOfLastWeek.atTime(23, 59, 59);

                    List<OrderItem> items = orderItemRepository.findByOrderStatus(OrderStatus.COMPLETED).stream()
                            .filter(oi -> {
                                LocalDateTime created = oi.getOrder().getCreatedAt();
                                return created != null && !created.isBefore(startDt) && !created.isAfter(endDt);
                            })
                            .collect(Collectors.toList());

                    Set<Order> orders = items.stream().map(OrderItem::getOrder).collect(Collectors.toSet());
                    int totalOrders = orders.size();
                    BigDecimal totalRevenue = orders.stream()
                            .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avgOrderValue = totalOrders == 0 ? BigDecimal.ZERO : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
                    long numCustomers = orders.stream().map(o -> o.getUser() == null ? null : o.getUser().getId()).filter(Objects::nonNull).distinct().count();

                    Map<Product, Integer> productQty = new HashMap<>();
                    for (OrderItem oi : items) {
                        Product p = oi.getProduct();
                        if (p == null) continue;
                        productQty.merge(p, oi.getQuantity(), Integer::sum);
                    }

                    List<Map.Entry<Product, Integer>> topProducts = productQty.entrySet().stream()
                            .sorted(Map.Entry.<Product, Integer>comparingByValue(Comparator.reverseOrder()))
                            .limit(5)
                            .collect(Collectors.toList());

                    sb.append("Week Start,Week End,Total Orders,Total Revenue,Average Order Value,Number of Customers\n");
                    sb.append(startOfLastWeek).append(',').append(endOfLastWeek).append(',')
                            .append(totalOrders).append(',')
                            .append(totalRevenue.setScale(2, RoundingMode.HALF_UP)).append(',')
                            .append(avgOrderValue.setScale(2, RoundingMode.HALF_UP)).append(',')
                            .append(numCustomers).append('\n');

                    sb.append('\n');
                    sb.append("Top 5 Selling Products\n");
                    sb.append("Product Name,Quantity Sold\n");
                    for (Map.Entry<Product, Integer> e : topProducts) {
                        sb.append(e.getKey().getName()).append(',').append(e.getValue()).append('\n');
                    }
                    break;
                }
                case "TOP_PRODUCTS": {
                    // Use repository aggregation to compute top products across completed orders
                    List<Object[]> rows = orderItemRepository.findTopProductsByStatus(OrderStatus.COMPLETED, PageRequest.of(0, 100));
                    sb.append("Rank,Product Name,Quantity Sold,Revenue\n");
                    int rank = 1;
                    for (Object[] r : rows) {
                        // r: [productId(Long), productName(String), qty(Long), revenue(BigDecimal)]
                        String name = r[1] == null ? "" : r[1].toString();
                        String qty = r[2] == null ? "0" : r[2].toString();
                        String revenue = r[3] == null ? "0" : r[3].toString();
                        sb.append(rank++).append(',').append(name).append(',').append(qty).append(',').append(revenue).append('\n');
                    }
                    break;
                }
                case "CUSTOMER_SEGMENTATION": {
                    List<CustomerSegmentResponse> segments = segmentationService.segmentCustomers();
                    Map<String, Integer> counts = new HashMap<>();
                    Map<String, BigDecimal> totals = new HashMap<>();
                    for (CustomerSegmentResponse r : segments) {
                        String seg = r.segment();
                        counts.put(seg, counts.getOrDefault(seg, 0) + 1);
                        totals.put(seg, totals.getOrDefault(seg, BigDecimal.ZERO).add(r.totalSpent()));
                    }

                    sb.append("Segment,Number of Customers,Total Spending\n");
                    for (String seg : List.of("VIP", "REGULAR", "LOW_VALUE")) {
                        int c = counts.getOrDefault(seg, 0);
                        BigDecimal t = totals.getOrDefault(seg, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                        sb.append(seg).append(',').append(c).append(',').append(t).append('\n');
                    }
                    break;
                }
                case "LOW_STOCK": {
                    int threshold = 10;
                    List<Product> lowStockProducts = productRepository.findByStockLessThanEqualOrderByStockAsc(threshold);
                    sb.append("Product Id,Product Name,Stock,Category\n");
                    for (Product p : lowStockProducts) {
                        String categoryName = p.getCategory() != null ? p.getCategory().getName() : "";
                        sb.append(p.getId()).append(',')
                                .append(p.getName()).append(',')
                                .append(p.getStock()).append(',')
                                .append(categoryName).append('\n');
                    }
                    break;
                }
                default: {
                    sb.append("id,name,value\n1,Sample,100\n");
                    break;
                }
            }

            Files.writeString(out, sb.toString());

            exec.setFileName(fileName);
            exec.setFileSize(Files.size(out));
            exec.setStoragePath(out.toString());
            exec.setStatus("SUCCESS");
            exec.setCompletedAt(LocalDateTime.now());
            executionRepository.save(exec);

            return new ReportExecutionResponse(exec.getId(), def.getId(), exec.getStatus(), exec.getCreatedAt(), exec.getCompletedAt(), exec.getFileName(), exec.getFileSize());
        } catch (Exception e) {
            exec.setStatus("FAILED");
            exec.setCompletedAt(LocalDateTime.now());
            executionRepository.save(exec);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ReportExecutionResponse> getHistory(Long reportId) {
        return executionRepository.findByReportIdOrderByCreatedAtDesc(reportId).stream()
                .map(e -> new ReportExecutionResponse(e.getId(), e.getReport().getId(), e.getStatus(), e.getCreatedAt(), e.getCompletedAt(), e.getFileName(), e.getFileSize()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getLowStockProducts(int threshold) {
        return productService.getLowStockProductsDto(threshold);
    }

    @Override
    public byte[] downloadExecution(Long executionId) {
        ReportExecution exec = executionRepository.findById(executionId).orElseThrow();
        try {
            Path p = Path.of(exec.getStoragePath());
            return Files.readAllBytes(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ReportExecutionResponse getExecution(Long executionId) {
        ReportExecution exec = executionRepository.findById(executionId).orElseThrow();
        return new ReportExecutionResponse(exec.getId(), exec.getReport().getId(), exec.getStatus(), exec.getCreatedAt(), exec.getCompletedAt(), exec.getFileName(), exec.getFileSize());
    }

    @Override
    public List<ReportScheduleResponse> listSchedules() {
        return scheduleRepository.findAll().stream()
            .map(s -> new ReportScheduleResponse(
                s.getId(),
                s.getReport().getId(),
                s.getCronExpression(),
                s.getEmail(),
                s.isActive(),
                s.getNextRun(),
                s.getLastRun(),
                s.getLastStatus(),
                s.getExecutionCount(),
                s.getLastError()
            ))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReportScheduleResponse createSchedule(Long reportId, String cron, String email) {
        ReportDefinition def = definitionRepository.findById(reportId).orElseThrow();
        // validate cron
        try {
            org.springframework.scheduling.support.CronExpression.parse(cron);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid cron expression: " + ex.getMessage());
        }

        ReportSchedule s = new ReportSchedule();
        s.setReport(def);
        s.setCronExpression(cron);
        s.setEmail(email);
        s.setActive(true);
        // compute nextRun
        try {
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault());
            java.time.ZonedDateTime next = org.springframework.scheduling.support.CronExpression.parse(cron).next(now.plusSeconds(1));
            if (next != null) s.setNextRun(next.toLocalDateTime());
        } catch (Exception ex) {
            // ignore, nextRun will be null
        }

        scheduleRepository.save(s);
        return new ReportScheduleResponse(s.getId(), def.getId(), s.getCronExpression(), s.getEmail(), s.isActive(), s.getNextRun(), s.getLastRun(), s.getLastStatus(), s.getExecutionCount(), s.getLastError());
    }

    @Override
    @Transactional
    public void pauseSchedule(Long id) {
        ReportSchedule s = scheduleRepository.findById(id).orElseThrow();
        s.setActive(false);
        scheduleRepository.save(s);
    }

    @Override
    @Transactional
    public void resumeSchedule(Long id) {
        ReportSchedule s = scheduleRepository.findById(id).orElseThrow();
        s.setActive(true);
        scheduleRepository.save(s);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    @Override
    public ReportSummaryResponse summary() {
        long totalScheduled = scheduleRepository.count();
        long successful = executionRepository.findAll().stream().filter(e -> "SUCCESS".equals(e.getStatus())).count();
        long failed = executionRepository.findAll().stream().filter(e -> "FAILED".equals(e.getStatus())).count();
        LocalDateTime last = executionRepository.findAll().stream().map(ReportExecution::getCompletedAt).filter(x -> x != null).max(LocalDateTime::compareTo).orElse(null);
        LocalDateTime next = scheduleRepository.findByActiveTrue().stream().map(ReportSchedule::getNextRun).filter(x -> x != null).min(LocalDateTime::compareTo).orElse(null);
        double successRate = (successful + failed) == 0 ? 0.0 : (double) successful / (successful + failed);
        return new ReportSummaryResponse(totalScheduled, successful, failed, last, next, successRate);
    }
}
