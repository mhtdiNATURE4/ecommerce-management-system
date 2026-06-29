package com.market.ecommerce.service;

import com.market.ecommerce.dto.OrderResponse;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.event.OrderCreatedPublisher;
import com.market.ecommerce.repository.AddressRepository;
import com.market.ecommerce.repository.CartItemRepository;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.repository.UserRepository;
import com.market.ecommerce.service.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderServiceTest {

    @Test
    void toDtoIncludesCustomerNameFromUser() {
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        CartItemRepository cartItemRepository = Mockito.mock(CartItemRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AddressRepository addressRepository = Mockito.mock(AddressRepository.class);
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        OrderCreatedPublisher orderCreatedPublisher = Mockito.mock(OrderCreatedPublisher.class);

        OrderService orderService = new OrderService(
                orderRepository,
                cartItemRepository,
                productRepository,
                userRepository,
                addressRepository,
                notificationService,
                inventoryService,
                orderCreatedPublisher
        );

        User user = User.builder()
                .id(42L)
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        Order order = Order.builder()
                .id(7L)
                .user(user)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("55.50"))
                .build();

        OrderResponse dto = orderService.toDto(order);

        assertEquals("Jane Doe", dto.customerName());
    }
}
