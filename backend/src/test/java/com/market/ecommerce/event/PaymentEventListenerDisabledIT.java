package com.market.ecommerce.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentEventListenerDisabledIT {

    @Autowired
    ApplicationContext ctx;

    @Test
    void paymentEventListenerBeanIsNotPresentWhenFeatureDisabled() {
        String[] names = ctx.getBeanNamesForType(com.market.ecommerce.event.PaymentEventListener.class);
        assertThat(names).isEmpty();
    }
}
