package com.market.ecommerce.event;

public class PaymentInitiatedEvent {
    private final Long paymentId;

    public PaymentInitiatedEvent(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
