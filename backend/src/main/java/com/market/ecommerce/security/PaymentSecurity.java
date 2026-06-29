package com.market.ecommerce.security;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class PaymentSecurity {

    private final OrderRepository orderRepository;

    public PaymentSecurity(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean canPayOrder(Long orderId) {
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
