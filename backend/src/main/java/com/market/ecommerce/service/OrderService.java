package com.market.ecommerce.service;

import com.market.ecommerce.dto.CheckoutRequest;
import com.market.ecommerce.dto.OrderItemResponse;
import com.market.ecommerce.dto.OrderResponse;
import com.market.ecommerce.entity.*;
import com.market.ecommerce.exception.BadRequestException;
import com.market.ecommerce.exception.ResourceNotFoundException;
import com.market.ecommerce.repository.*;
import com.market.ecommerce.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final NotificationService notificationService;
    private final com.market.ecommerce.service.inventory.InventoryService inventoryService;
    private final com.market.ecommerce.event.OrderCreatedPublisher orderCreatedPublisher;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        AddressRepository addressRepository,
                        NotificationService notificationService,
                        com.market.ecommerce.service.inventory.InventoryService inventoryService,
                        com.market.ecommerce.event.OrderCreatedPublisher orderCreatedPublisher) {

        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.orderCreatedPublisher = orderCreatedPublisher;
    }

    @Transactional
    public Order checkout(CheckoutRequest request, String idempotencyKey) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));

        // strict idempotency: if an order already exists for this user+key, return it
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Address shippingAddress = addressRepository.findById(request.shippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("عنوان الشحن غير موجود"));

        if (!shippingAddress.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("عنوان الشحن لا يخص المستخدم الحالي");
        }

        List<CartItem> cartItems = cartRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("سلة المشتريات فارغة");
        }

        Order order = Order.builder()
                .user(user)
                .shippingAddress(shippingAddress)
            .status(OrderStatus.CREATED)
            .idempotencyKey(idempotencyKey)
            .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // track successful decrements to allow compensation on failure
        java.util.Map<Long, Integer> decremented = new java.util.HashMap<>();

        for (CartItem item : cartItems) {
            Long productId = item.getProduct().getId();
            int qty = item.getQuantity();

            // load product entity for metadata (name, price)
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("المنتج غير موجود"));

            if (product.getStock() < qty) {
                throw new BadRequestException("المخزون غير كافٍ للمنتج: " + product.getName());
            }

            // Use atomic decrement at DB level to avoid oversell under concurrency
            boolean decrementedOk = inventoryService.decrementIfAvailable(productId, qty);
            if (!decrementedOk) {
                // compensate any prior successful decrements
                for (var e : decremented.entrySet()) {
                    try {
                        inventoryService.increment(e.getKey(), e.getValue());
                    } catch (Exception ex) {
                        // Log each failed compensation attempt with context (keep swallowing to preserve original flow)
                        logger.error("Failed to compensate inventory increment for productId={} qty={} orderId={} userEmail={} - error: {}",
                                e.getKey(), e.getValue(), (order != null ? order.getId() : null), (user != null ? user.getEmail() : "unknown"), ex.getMessage(), ex);
                    }
                }
                throw new BadRequestException("المخزون غير كافٍ للمنتج: " + product.getName());
            }

            // record successful decrement
            decremented.merge(productId, qty, Integer::sum);

            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(qty)
                .price(product.getPrice())
                .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // possible concurrent insert with same idempotency key - return existing
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                var existing = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);
                if (existing.isPresent()) return existing.get();
            }
            throw ex;
        }
        cartRepository.deleteByUserId(user.getId());

        // publish event to send notifications after commit
        orderCreatedPublisher.publish(savedOrder.getId());

        return savedOrder;
    }

    @Transactional
    public Order startProcessing(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getStatus() == OrderStatus.PROCESSING) {
            throw new BadRequestException("الطلب قيد المعالجة بالفعل");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("لا يمكن بدء معالجة طلب مكتمل");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("لا يمكن بدء معالجة طلب ملغي");
        }

        order.setStatus(OrderStatus.PROCESSING);
        return orderRepository.save(order);
    }

    @Transactional
    public Order complete(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("الطلب مكتمل بالفعل");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("لا يمكن إتمام طلب ملغي");
        }
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new BadRequestException("يمكن إكمال الطلبات التي في حالة المعالجة فقط");
        }

        order.setStatus(OrderStatus.COMPLETED);
        return orderRepository.save(order);
    }

    @Transactional
    public void cancel(Long id) {
        Order order = getOrderByIdAuthorized(id);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("لا يمكن إلغاء طلب مكتمل");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("الطلب ملغي بالفعل");
        }
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PROCESSING) {
            throw new BadRequestException("لا يمكن إلغاء الطلب في هذه الحالة");
        }

        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();
            int qty = item.getQuantity();
            inventoryService.increment(productId, qty);
        }

        orderRepository.save(order);
    }

    // New: return the Order entity by id with current user authorization
    public Order getOrderByIdAuthorized(Long id) {
        Order order = orderRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));
        authorizeOrderAccess(order);
        return order;
    }

    // New: return the Order entity by id
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));
    }

    // New: return the Order entity with user fetched
    public Order getOrderByIdWithUser(Long id) {
        return orderRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));
    }

    private void authorizeOrderAccess(Order order) {
        if (SecurityUtils.currentUserIsAdmin()) {
            return;
        }

        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || order.getUser() == null || !email.equals(order.getUser().getEmail())) {
            throw new AccessDeniedException("غير مصرح بالوصول إلى هذا الطلب");
        }
    }

    // New: return safe DTO for order detail
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdDto(Long id) {
        Order order = getOrderByIdAuthorized(id);

        var items = order.getItems().stream()
                .map(it -> new OrderItemResponse(
                        it.getId(),
                        it.getProduct().getId(),
                        it.getProduct().getName(),
                        it.getQuantity(),
                        it.getPrice().toString()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0.00",
                order.getStatus() != null ? order.getStatus().name() : "",
                order.getShippingAddress() != null ? order.getShippingAddress().getId() : null,
                order.getShippingAddress() != null ? new com.market.ecommerce.dto.AddressResponse(
                        order.getShippingAddress().getId(),
                        order.getShippingAddress().getStreet(),
                        order.getShippingAddress().getCity(),
                        order.getShippingAddress().getCountry(),
                        order.getShippingAddress().getZipCode()
                ) : null,
                order.getUser() != null ? order.getUser().getName() : null,
                order.getCreatedAt(),
                items
        );
    }

    // Map single order to DTO
    public OrderResponse toDto(Order order) {
        var items = order.getItems().stream()
                .map(it -> new OrderItemResponse(
                        it.getId(),
                        it.getProduct().getId(),
                        it.getProduct().getName(),
                        it.getQuantity(),
                        it.getPrice().toString()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0.00",
                order.getStatus() != null ? order.getStatus().name() : "",
                order.getShippingAddress() != null ? order.getShippingAddress().getId() : null,
                order.getShippingAddress() != null ? new com.market.ecommerce.dto.AddressResponse(
                        order.getShippingAddress().getId(),
                        order.getShippingAddress().getStreet(),
                        order.getShippingAddress().getCity(),
                        order.getShippingAddress().getCountry(),
                        order.getShippingAddress().getZipCode()
                ) : null,
                order.getUser() != null ? order.getUser().getName() : null,
                order.getCreatedAt(),
                items
        );
    }

    // New: return OrderResponse DTO after checkout
    @Transactional
    public OrderResponse checkoutDto(CheckoutRequest request, String idempotencyKey) {
        Order order = checkout(request, idempotencyKey);
        return toDto(order);
    }

    // New: return list of DTOs for current user's orders
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrdersDto() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));

        List<Order> orders = orderRepository.findByUserIdWithItems(user.getId());
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderResponse> getUserOrdersPaged(org.springframework.data.domain.Pageable pageable) {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));

        var page = orderRepository.findByUserId(user.getId(), pageable);
        var ids = page.map(Order::getId).getContent();
        var orders = ids.isEmpty() ? java.util.List.<Order>of() : orderRepository.findByIdInWithItems(ids);
        // reorder fetched orders to match the page id ordering
        var orderById = orders.stream().collect(Collectors.toMap(Order::getId, o -> o));
        var ordered = ids.stream().map(orderById::get).filter(java.util.Objects::nonNull).toList();
        var dtos = ordered.stream().map(this::toDto).collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // New: return list of DTOs for all orders (admin)
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersDto() {
        List<Order> orders = orderRepository.findAllWithItems();
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderResponse> getAllOrdersPaged(org.springframework.data.domain.Pageable pageable) {
        var page = orderRepository.findAll(pageable);
        var ids = page.map(Order::getId).getContent();
        var orders = ids.isEmpty() ? java.util.List.<Order>of() : orderRepository.findByIdInWithItems(ids);
        var orderById = orders.stream().collect(Collectors.toMap(Order::getId, o -> o));
        var ordered = ids.stream().map(orderById::get).filter(java.util.Objects::nonNull).toList();
        var dtos = ordered.stream().map(this::toDto).collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements());
    }
}
