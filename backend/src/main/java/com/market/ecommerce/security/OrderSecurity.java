package com.market.ecommerce.security;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderSecurity {

    private final OrderRepository orderRepository;

    public OrderSecurity(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean isOrderOwner(Long orderId) {
        if (SecurityUtils.currentUserIsAdmin()) {
            return true;
        }

        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            return false;
        }

        return orderRepository.findByIdWithUser(orderId)
                .map(Order::getUser)
                .map(user -> user.getEmail() != null && user.getEmail().equals(email))
                .orElse(false);
    }
}
