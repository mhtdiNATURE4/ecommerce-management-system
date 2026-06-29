package com.market.ecommerce;

import com.market.ecommerce.dto.AuthResponse;
import com.market.ecommerce.dto.CheckoutRequest;
import com.market.ecommerce.dto.LoginRequest;
import com.market.ecommerce.dto.PaymentRequest;
import com.market.ecommerce.entity.Address;
import com.market.ecommerce.entity.Category;
import com.market.ecommerce.entity.CartItem;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.entity.UserRole;
import com.market.ecommerce.repository.AddressRepository;
import com.market.ecommerce.repository.CartItemRepository;
import com.market.ecommerce.repository.CategoryRepository;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.PaymentRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PaymentEdgeCaseIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long addrAId;
    private Long addrBId;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(Category.builder().name("Test Category").build());
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stock(100)
                .category(category)
                .build());

        User userA = userRepository.save(User.builder()
                .name("User A")
                .email("a@example.com")
                .password(passwordEncoder.encode("passA"))
                .role(UserRole.CUSTOMER)
                .build());

        User userB = userRepository.save(User.builder()
                .name("User B")
                .email("b@example.com")
                .password(passwordEncoder.encode("passB"))
                .role(UserRole.CUSTOMER)
                .build());

        User admin = userRepository.save(User.builder()
                .name("Admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("adminPass"))
                .role(UserRole.ADMIN)
                .build());

        Address addrA = addressRepository.save(Address.builder()
                .user(userA)
                .street("123 A St")
                .city("City")
                .country("Country")
                .zipCode("1000")
                .build());

        Address addrB = addressRepository.save(Address.builder()
                .user(userB)
                .street("456 B Ave")
                .city("City")
                .country("Country")
                .zipCode("2000")
                .build());

        addrAId = addrA.getId();
        addrBId = addrB.getId();

        cartItemRepository.save(CartItem.builder().user(userA).product(product).quantity(1).build());
        cartItemRepository.save(CartItem.builder().user(userB).product(product).quantity(1).build());

        tokenA = login("a@example.com", "passA");
        tokenB = login("b@example.com", "passB");
        tokenAdmin = login("admin@example.com", "adminPass");
    }

    @Test
    void cannotPayCompletedOrder() {
        Long orderId = checkoutAs(tokenA, addrAId);
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        ResponseEntity<String> paymentResponse = requestPayment(orderId, "10.00", "CREDIT_CARD", tokenA);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paymentResponse.getBody()).contains("مدفوع بالفعل");
    }

    @Test
    void cannotPayCancelledOrder() {
        Long orderId = checkoutAs(tokenA, addrAId);
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        ResponseEntity<String> paymentResponse = requestPayment(orderId, "10.00", "CREDIT_CARD", tokenA);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paymentResponse.getBody()).contains("ملغي");
    }

    @Test
    void cannotPaySomeoneElsesOrderEndpointLevel() {
        Long orderId = checkoutAs(tokenA, addrAId);

        ResponseEntity<String> paymentResponse = requestPayment(orderId, "10.00", "CREDIT_CARD", tokenB);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanPayAnyOrder() {
        Long orderId = checkoutAs(tokenA, addrAId);

        ResponseEntity<String> paymentResponse = requestPayment(orderId, "10.00", "CREDIT_CARD", tokenAdmin);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void validPaymentFlowSucceeds() {
        Long orderId = checkoutAs(tokenA, addrAId);

        ResponseEntity<String> paymentResponse = requestPayment(orderId, "10.00", "CREDIT_CARD", tokenA);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(paymentResponse.getBody()).contains("id", "orderId");
    }

    @Test
    void concurrentPaymentAttemptsBlockedAfterFirst() throws InterruptedException {
        Long orderId = checkoutAs(tokenA, addrAId);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger forbiddenCount = new AtomicInteger(0);
        int threadCount = 3;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    ResponseEntity<String> response = requestPayment(orderId, "10.00", "CREDIT_CARD", tokenA);
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        forbiddenCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(forbiddenCount.get()).isEqualTo(2);
    }

    private String login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url("/api/auth/login"), entity, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private Long checkoutAs(String token, Long addressId) {
        CheckoutRequest request = new CheckoutRequest(addressId);
        HttpEntity<CheckoutRequest> entity = new HttpEntity<>(request, authHeaders(token));
        ResponseEntity<String> response = restTemplate.postForEntity(url("/api/orders/checkout"), entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("id");

        return extractOrderId(response.getBody());
    }

    private ResponseEntity<String> requestPayment(Long orderId, String amount, String method, String token) {
        PaymentRequest request = new PaymentRequest(orderId, amount, method);
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, authHeaders(token));
        return restTemplate.postForEntity(url("/api/payments"), entity, String.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Long extractOrderId(String body) {
        int idIndex = body.indexOf("\"id\":");
        assertThat(idIndex).isGreaterThanOrEqualTo(0);
        int start = body.indexOf(':', idIndex) + 1;
        int end = body.indexOf(',', start);
        if (end < 0) {
            end = body.indexOf('}', start);
        }
        return Long.valueOf(body.substring(start, end).trim());
    }
}
