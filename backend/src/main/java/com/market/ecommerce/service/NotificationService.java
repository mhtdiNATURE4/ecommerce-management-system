package com.market.ecommerce.service;

import com.market.ecommerce.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final boolean mailEnabled;
    private final ExecutorService notificationExecutor;
    private final MeterRegistry meterRegistry;
    private final boolean notificationEnabled;
    private final int notificationQueueCapacity;
    private final String threadNamePrefix;

    public NotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${spring.mail.from:no-reply@example.com}") String mailFrom,
                               @Value("${notification.enabled:true}") boolean notificationEnabled,
                               @Value("${notification.executor.queue-capacity:100}") int notificationQueueCapacity,
                               @Value("${notification.executor.thread-name-prefix:notification-service-worker}") String threadNamePrefix,
                               MeterRegistry meterRegistry) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailFrom = mailFrom;
        this.mailEnabled = this.mailSender != null;
        this.notificationEnabled = notificationEnabled;
        this.notificationQueueCapacity = notificationQueueCapacity;
        this.threadNamePrefix = threadNamePrefix != null ? threadNamePrefix : "notification-service-worker";
        this.meterRegistry = meterRegistry;

        if (!this.notificationEnabled) {
            this.notificationExecutor = null;
            log.info("NotificationService disabled by configuration (notification.enabled=false)");
            return;
        }

        // single-threaded bounded executor to ensure notifications are best-effort and non-blocking
        int queueCapacity = Math.max(1, this.notificationQueueCapacity);
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, this.threadNamePrefix);
            t.setDaemon(true);
            return t;
        };
        this.notificationExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity), tf, (r, executor) -> {
            log.warn("Notification queue full; dropping notification task");
        });
    }

    // يمكنك لاحقاً دمج spring-boot-starter-mail هنا لإرسال بريد حقيقي
    public void sendOrderConfirmationEmail(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null || order.getUser().getEmail().isBlank()) {
            log.warn("Unable to send order confirmation email because order or recipient email is missing. orderId={}",
                    order != null ? order.getId() : null);
            return;
        }

        String email = order.getUser().getEmail().trim();
        log.info("تم تأكيد الطلب رقم: {} - جاري إرسال إشعار إلى البريد: {}", order.getId(), email);

        if (!mailEnabled) {
            log.warn("Mail sender is not configured; skipping actual email send for order {}. Configure spring.mail host and credentials to enable notifications.", order.getId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("تأكيد الطلب رقم " + order.getId());
        message.setText(buildEmailBody(order));

        try {
            mailSender.send(message);
            log.info("Order confirmation email sent for order {} to {}", order.getId(), email);
        } catch (MailException ex) {
            log.error("Failed to send order confirmation email for order {} to {}", order.getId(), email, ex);
        }
    }

    // Notify operators about an outbox event that moved to DEAD (minimal, safe)
    public void notifyDeadLetter(String eventType, Long aggregateId, Integer retryCount, Long eventId) {
        // Quick structured log immediately; notification itself is best-effort and async
        log.warn("deadletter.outbox eventType={} aggregateId={} retryCount={} eventId={}", eventType, aggregateId, retryCount, eventId);

        if (!mailEnabled) {
            log.warn("Mail sender not configured; skipping dead-letter email for outbox event {}", eventId);
            return;
        }

        if (!this.notificationEnabled) {
            log.debug("Notifications disabled; skipping dead-letter submission for event {}", eventId);
            return;
        }

        // Build message content outside executor to keep submit fast
        final String subject = "Outbox DEAD event: " + eventType;
        final StringBuilder body = new StringBuilder();
        body.append("Outbox event moved to DEAD:\n");
        body.append("eventId: ").append(eventId).append("\n");
        body.append("eventType: ").append(eventType).append("\n");
        body.append("aggregateId: ").append(aggregateId).append("\n");
        body.append("retryCount: ").append(retryCount).append("\n");

        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(mailFrom);
        message.setSubject(subject);
        message.setText(body.toString());

        try {
            notificationExecutor.execute(() -> {
                try {
                    mailSender.send(message);
                    log.info("Dead-letter notification email sent for outbox event {}", eventId);
                } catch (Exception ex) {
                    // swallow and log any exception from notification
                    log.error("Failed to send dead-letter notification for outbox event {}: {}", eventId, ex.toString());
                }
            });
        } catch (RejectedExecutionException rex) {
            log.warn("Notification executor rejected task for outbox event {}: {}", eventId, rex.toString());
            try {
                if (this.meterRegistry != null) {
                    String et = eventType == null ? "unknown" : eventType;
                    this.meterRegistry.counter("notification.queue.dropped", "eventType", et, "reason", "queue_full").increment();
                }
            } catch (Exception mEx) {
                log.warn("Failed to record notification.queue.dropped metric: {}", mEx.toString());
            }
        } catch (Exception ex) {
            // ensure no exception bubbles to caller
            log.warn("Unexpected error while submitting dead-letter notification for outbox event {}: {}", eventId, ex.toString());
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        try {
            notificationExecutor.shutdown();
            notificationExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("Error shutting down notification executor: {}", ex.toString());
        }
    }

    private String buildEmailBody(Order order) {
        var builder = new StringBuilder();
        builder.append("مرحباً،\n\n");
        builder.append("شكراً لتسوقك معنا! تم تأكيد طلبك رقم ");
        builder.append(order.getId());
        builder.append(".\n\n");
        builder.append("قيمة الطلب الإجمالية: ");
        builder.append(order.getTotalAmount());
        builder.append("\n");
        builder.append("حالة الطلب: ");
        builder.append(order.getStatus());
        builder.append("\n\n");

        if (order.getShippingAddress() != null) {
            builder.append("عنوان الشحن:\n");
            builder.append(order.getShippingAddress().getStreet()).append("\n");
            builder.append(order.getShippingAddress().getCity()).append(", ");
            builder.append(order.getShippingAddress().getCountry()).append("\n");
            builder.append(order.getShippingAddress().getZipCode()).append("\n\n");
        }

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            builder.append("عناصر الطلب:\n");
            order.getItems().forEach(item -> builder.append("- ")
                    .append(item.getProduct().getName())
                    .append(" x")
                    .append(item.getQuantity())
                    .append(" = ")
                    .append(item.getPrice())
                    .append("\n"));
            builder.append("\n");
        }

        builder.append("إذا كانت لديك أي أسئلة، فلا تتردد بالرد على هذا البريد.\n\n");
        builder.append("مع تحيات فريق المتجر.");
        return builder.toString();
    }
}