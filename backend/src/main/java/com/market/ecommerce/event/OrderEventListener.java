package com.market.ecommerce.event;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Optional;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderEventListener(OrderRepository orderRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            Optional<Order> opt = orderRepository.findByIdWithUser(event.getOrderId());
            if (opt.isPresent()) {
                notificationService.sendOrderConfirmationEmail(opt.get());
            } else {
                log.warn("Order not found for OrderCreatedEvent: {}", event.getOrderId());
            }
        } catch (Exception ex) {
            log.error("Failed to handle OrderCreatedEvent for order {}", event.getOrderId(), ex);
        }
    }
}
