package com.market.ecommerce.service;

import com.market.ecommerce.dto.CustomerSegmentResponse;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.entity.UserRole;
import com.market.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerSegmentationService {

    private final OrderRepository orderRepository;

    public CustomerSegmentationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerSegmentResponse> segmentCustomers() {
        // تحسين الأداء: جلب الطلبات المكتملة مباشرة من قاعدة البيانات
        List<Order> completedOrders = orderRepository.findByStatusWithItems(OrderStatus.COMPLETED);

        // تجميع المبالغ المدفوعة لكل مستخدم
        Map<User, Double> userSpentMap = completedOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getUser,
                        Collectors.summingDouble(o -> o.getTotalAmount().doubleValue())
                ));

        return userSpentMap.entrySet().stream()
                .filter(entry -> entry.getKey().getRole() != UserRole.ADMIN)
                .map(entry -> {
                    User user = entry.getKey();
                    double totalSpent = entry.getValue();
                    String segment = totalSpent >= 3000 ? "VIP" : totalSpent >= 1000 ? "REGULAR" : "LOW_VALUE";

                    return new CustomerSegmentResponse(
                            user.getId(),
                            user.getName(),
                            BigDecimal.valueOf(totalSpent),
                            segment
                    );
                })
                .sorted((a, b) -> b.totalSpent().compareTo(a.totalSpent()))
                .toList();
    }
}