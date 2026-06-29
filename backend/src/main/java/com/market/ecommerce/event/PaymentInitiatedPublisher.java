package com.market.ecommerce.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.ecommerce.entity.OutboxEvent;
import com.market.ecommerce.repository.OutboxEventRepository;
import com.market.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class PaymentInitiatedPublisher {
    private final OutboxEventRepository outboxRepo;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public PaymentInitiatedPublisher(OutboxEventRepository outboxRepo,
                                     PaymentRepository paymentRepository,
                                     ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(Long paymentId) {
        Optional<com.market.ecommerce.entity.Payment> opt = paymentRepository.findById(paymentId);
        if (opt.isEmpty()) return;
        com.market.ecommerce.entity.Payment p = opt.get();

        var payloadMap = new java.util.HashMap<String, Object>();
        payloadMap.put("paymentId", p.getId());
        payloadMap.put("orderId", p.getOrder() != null ? p.getOrder().getId() : null);
        payloadMap.put("amount", p.getAmount());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            payload = "{}";
        }

        OutboxEvent ev = OutboxEvent.builder()
                .aggregateType(OutboxEvent.AggregateType.PAYMENT)
                .aggregateId(p.getId())
                .eventType("PAYMENT_INITIATED")
                .payload(payload)
                .status(OutboxEvent.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .retryCount(0)
                .build();

        outboxRepo.save(ev);
    }
}
