package com.market.ecommerce.service;

import com.market.ecommerce.dto.DashboardResponse;
import com.market.ecommerce.entity.*;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public DashboardService(UserRepository userRepository,
                            OrderRepository orderRepository,
                            ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        List<User> users = userRepository.findAll();

        // تحسين الأداء: جلب الطلبات المكتملة مباشرة من قاعدة البيانات
        List<Order> completedOrders = orderRepository.findByStatusWithItems(OrderStatus.COMPLETED);

        // 1. حساب إجمالي الإيرادات
        BigDecimal totalRevenue = completedOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull) // ممارسة أفضل للتحقق من القيم الفارغة
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. تجميع مبيعات المنتجات
        Map<Long, Integer> productSales = completedOrders.stream()
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getProduct() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));

        // 3. تجميع مدفوعات العملاء
        Map<Long, BigDecimal> customerSpend = completedOrders.stream()
                .filter(order -> order.getUser() != null && order.getTotalAmount() != null)
                .collect(Collectors.toMap(
                        order -> order.getUser().getId(),
                        Order::getTotalAmount,
                        BigDecimal::add
                ));

        Long bestProductId = productSales.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Long topCustomerId = customerSpend.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String bestProductName = Optional.ofNullable(bestProductId)
                .flatMap(productRepository::findById)
                .map(Product::getName) // استخدام Method Reference
                .orElse("N/A");

        String topCustomerName = Optional.ofNullable(topCustomerId)
                .flatMap(userRepository::findById)
                .map(User::getName)
                .orElse("N/A");

        long customerCount = users.stream()
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .count();

        return new DashboardResponse(
                customerCount,
                orderRepository.count(), // استعلام مباشر لعدد الطلبات الكلي
                totalRevenue,
                productRepository.count(),
                bestProductName,
                topCustomerName
        );
    }
}