package com.market.ecommerce.ordering;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderItem;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.exception.BadRequestException;
import com.market.ecommerce.repository.*;
import com.market.ecommerce.service.OrderService;
import com.market.ecommerce.service.NotificationService;
import com.market.ecommerce.service.inventory.InventoryService;
import com.market.ecommerce.event.OrderCreatedPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderLifecycleServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CartItemRepository cartRepository = mock(CartItemRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AddressRepository addressRepository = mock(AddressRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final InventoryService inventoryService = mock(InventoryService.class);
    private final OrderCreatedPublisher orderCreatedPublisher = mock(OrderCreatedPublisher.class);

    private final OrderService orderService = new OrderService(
            orderRepository,
            cartRepository,
            productRepository,
            userRepository,
            addressRepository,
            notificationService,
            inventoryService,
            orderCreatedPublisher
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startProcessingMovesCreatedOrderToProcessing() {
        Order order = Order.builder().id(10L).status(OrderStatus.CREATED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.startProcessing(10L);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void completeMovesProcessingOrderToCompleted() {
        Order order = Order.builder().id(20L).status(OrderStatus.PROCESSING).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.complete(20L);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void cancelRestoresStockForCreatedOrderAndMarksCancelled() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin", java.util.List.of(() -> "ROLE_ADMIN")));

        Order order = Order.builder().id(30L).status(OrderStatus.CREATED).totalAmount(BigDecimal.TEN).build();
        order.setUser(new User());
        order.getUser().setEmail("admin@example.com");
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(new Product());
        orderItem.getProduct().setId(99L);
        orderItem.setQuantity(2);
        order.setItems(java.util.List.of(orderItem));

        when(orderRepository.findByIdWithUser(30L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.cancel(30L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).increment(anyLong(), anyInt());
    }

    @Test
    void completedOrderCannotBeTransitionedAgain() {
        Order order = Order.builder().id(40L).status(OrderStatus.COMPLETED).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(40L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.complete(40L))
                .isInstanceOf(BadRequestException.class);
    }
}
