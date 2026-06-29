package com.market.ecommerce.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.ecommerce.entity.OutboxEvent;
import com.market.ecommerce.repository.OutboxEventRepository;
import com.market.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class OrderCreatedPublisher {
    private final OutboxEventRepository outboxRepo;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderCreatedPublisher(OutboxEventRepository outboxRepo,
                                 OrderRepository orderRepository,
                                 ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(Long orderId) {
        // Load order details to build payload (minimal change to callers)
        Optional<com.market.ecommerce.entity.Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) return;
        com.market.ecommerce.entity.Order order = opt.get();

        var payloadMap = new java.util.HashMap<String, Object>();
        payloadMap.put("orderId", order.getId());
        payloadMap.put("userId", order.getUser() != null ? order.getUser().getId() : null);
        payloadMap.put("total", order.getTotalAmount());
        payloadMap.put("timestamp", OffsetDateTime.now().toString());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payload = "{}";
        }

        OutboxEvent ev = OutboxEvent.builder()
                .aggregateType(OutboxEvent.AggregateType.ORDER)
                .aggregateId(order.getId())
                .eventType("ORDER_CREATED")
                .payload(payload)
                .status(OutboxEvent.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .retryCount(0)
                .build();

        outboxRepo.save(ev);
    }
}
