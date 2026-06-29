package com.market.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.ecommerce.dto.CheckoutRequest;
import com.market.ecommerce.dto.LoginRequest;
import com.market.ecommerce.entity.*;
import com.market.ecommerce.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ConcurrentCheckoutIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long productId;
    private Long addrAId;
    private Long addrBId;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setup() {
        // clean up DB
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        productRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // create product with stock 1
        Category category = categoryRepository.save(Category.builder().name("Test Category").build());
        Product p = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stock(1)
                .category(category)
                .build();
        p = productRepository.save(p);
        productId = p.getId();

        // create users, addresses and cart items via repositories for speed
        User a = User.builder().name("User A").email("a@example.com").password(passwordEncoder.encode("passA")).build();
        User b = User.builder().name("User B").email("b@example.com").password(passwordEncoder.encode("passB")).build();
        a = userRepository.save(a);
        b = userRepository.save(b);

        Address addrA = Address.builder().user(a).street("addr A").city("C").country("Country").zipCode("1000").build();
        Address addrB = Address.builder().user(b).street("addr B").city("C").country("Country").zipCode("1001").build();
        addrA = addressRepository.save(addrA);
        addrB = addressRepository.save(addrB);
        addrAId = addrA.getId();
        addrBId = addrB.getId();

        CartItem ca = CartItem.builder().user(a).product(p).quantity(1).build();
        CartItem cb = CartItem.builder().user(b).product(p).quantity(1).build();
        cartItemRepository.save(ca);
        cartItemRepository.save(cb);

        // login both users to obtain tokens
        LoginRequest loginA = new LoginRequest("a@example.com", "passA");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> requestA = new HttpEntity<>(loginA, headers);
        ResponseEntity<String> respA = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", requestA, String.class);
        // The response body contains the token; extract from JSON manually
        tokenA = extractTokenFromResponse(respA);

        LoginRequest loginB = new LoginRequest("b@example.com", "passB");
        HttpEntity<LoginRequest> requestB = new HttpEntity<>(loginB, headers);
        ResponseEntity<String> respB = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", requestB, String.class);
        tokenB = extractTokenFromResponse(respB);
    }

    private String extractTokenFromResponse(ResponseEntity<String> resp) {
        try {
            String body = resp.getBody();
            if (body == null) return null;
            return OBJECT_MAPPER.readTree(body).path("token").asText(null);
        } catch (Exception ex) {
            throw new AssertionError("Unable to parse login response: " + resp.getBody(), ex);
        }
    }

    @Test
    void concurrentCheckoutsRespectStock() throws InterruptedException {
        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        Runnable r1 = () -> {
            ready.countDown();
            try { start.await(); } catch (InterruptedException e) { }

            CheckoutRequest checkoutRequest = new CheckoutRequest(addrAId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenA);
            HttpEntity<CheckoutRequest> entity = new HttpEntity<>(checkoutRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/api/orders/checkout", entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) success.incrementAndGet(); else fail.incrementAndGet();
        };

        Runnable r2 = () -> {
            ready.countDown();
            try { start.await(); } catch (InterruptedException e) { }

            CheckoutRequest checkoutRequest = new CheckoutRequest(addrBId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenB);
            HttpEntity<CheckoutRequest> entity = new HttpEntity<>(checkoutRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/api/orders/checkout", entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) success.incrementAndGet(); else fail.incrementAndGet();
        };

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);
        t1.start(); t2.start();
        ready.await();
        start.countDown();
        t1.join(); t2.join();

        Assertions.assertEquals(1, success.get());
        Assertions.assertEquals(1, fail.get());
    }
}
