package com.market.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.entity.UserRole;
import com.market.ecommerce.repository.AddressRepository;
import com.market.ecommerce.repository.CartItemRepository;
import com.market.ecommerce.repository.CategoryRepository;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EcommerceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    public void ensureAdminExists() {
        final String adminEmail = "admin@example.com";
        final String adminPassword = "admin123";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
        } else {
            userRepository.findByEmail(adminEmail).ifPresent(u -> {
                u.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(u);
            });
        }
    }

    @Test
    @Transactional
    public void authFlow_shouldRegisterAndLoginSuccessfully() throws Exception {
        var user = registerUser();
        String token = loginUser(user.email(), user.password());
        assertThat(token).isNotBlank();
    }

    @Test
    @Transactional
    public void productFlow_shouldCreateAndListProducts() throws Exception {
        String adminToken = loginAdmin();
        long productId = createProduct(adminToken);

        // verify product appears in list
        MvcResult products = mockMvc.perform(get("/api/products")).andExpect(status().isOk()).andReturn();
        JsonNode productsNode = objectMapper.readTree(products.getResponse().getContentAsString());
        boolean found = false;
        for (JsonNode p : productsNode) {
            if (p.get("id").asLong() == productId) { found = true; break; }
        }
        assertThat(found).isTrue();
    }

    @Test
    @Transactional
    public void checkoutFlow_shouldCreateOrderSuccessfully() throws Exception {
        var user = registerUser();
        String userToken = loginUser(user.email(), user.password());

        String adminToken = loginAdmin();
        long productId = createProduct(adminToken);

        long addressId = createAddress(userToken);

        // add to cart
        String addCart = String.format("{\"productId\":%d,\"quantity\":1}", productId);
        MvcResult cartRes = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(addCart))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode cartNode = objectMapper.readTree(cartRes.getResponse().getContentAsString());
        long cartItemId = cartNode.get("cartItemId").asLong();
        assertThat(cartItemId).isGreaterThan(0);

        // checkout
        String checkoutReq = String.format("{\"shippingAddressId\":%d}", addressId);
        MvcResult checkoutRes = mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(checkoutReq))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode orderNode = objectMapper.readTree(checkoutRes.getResponse().getContentAsString());
        long orderId = orderNode.get("id").asLong();
        assertThat(orderId).isGreaterThan(0);
    }

    // --- helpers ---
    private record UserCred(String name, String email, String password) {}

    private UserCred registerUser() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0,6);
        String email = "it+" + unique + "@example.com";
        String name = "it-user-" + unique;
        String password = "pass1234";

        String userJson = String.format("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", name, email, password);

        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        return new UserCred(name, email, password);
    }

    private String loginUser(String email, String password) throws Exception {
        String loginJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(login.getResponse().getContentAsString());
        return loginNode.get("token").asText();
    }

    private String loginAdmin() throws Exception {
        String adminLogin = "{\"email\":\"admin@example.com\",\"password\":\"admin123\"}";
        MvcResult adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLogin))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(adminLoginRes.getResponse().getContentAsString()).get("token").asText();
    }

    private long createProduct(String adminToken) throws Exception {
        return createProduct(adminToken, 10);
    }

    private long createProduct(String adminToken, int stock) throws Exception {
        MvcResult cats = mockMvc.perform(get("/api/categories")).andExpect(status().isOk()).andReturn();
        JsonNode catsNode = objectMapper.readTree(cats.getResponse().getContentAsString());
        long categoryId;
        if (catsNode.isEmpty()) {
            String categoryReq = "{\"name\":\"IT Category\"}";
            MvcResult createCategory = mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + adminToken)
                            .content(categoryReq))
                    .andExpect(status().isCreated())
                    .andReturn();
            JsonNode categoryNode = objectMapper.readTree(createCategory.getResponse().getContentAsString());
            categoryId = categoryNode.get("id").asLong();
        } else {
            categoryId = catsNode.get(0).get("id").asLong();
        }

        return createProduct(adminToken, stock, categoryId);
    }

    private long createProduct(String adminToken, int stock, long categoryId) throws Exception {
        String productReq = String.format("{\"name\":\"IT Product %s\",\"description\":\"desc\",\"price\":9.99,\"stock\":%d,\"imageUrl\":\"\",\"categoryId\":%d}", UUID.randomUUID().toString().substring(0,6), stock, categoryId);

        MvcResult createProduct = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(productReq))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode prodNode = objectMapper.readTree(createProduct.getResponse().getContentAsString());
        return prodNode.get("id").asLong();
    }

    @Test
    @Transactional
    public void cartUpdate_shouldAcceptJsonBodyQuantity() throws Exception {
        var user = registerUser();
        String userToken = loginUser(user.email(), user.password());

        String adminToken = loginAdmin();
        long productId = createProduct(adminToken, 5);

        String addCart = String.format("{\"productId\":%d,\"quantity\":1}", productId);
        MvcResult addCartRes = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(addCart))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode cartNode = objectMapper.readTree(addCartRes.getResponse().getContentAsString());
        long cartItemId = cartNode.get("cartItemId").asLong();

        MvcResult updateRes = mockMvc.perform(put("/api/cart/" + cartItemId + "/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updatedItem = objectMapper.readTree(updateRes.getResponse().getContentAsString());
        assertThat(updatedItem.get("quantity").asInt()).isEqualTo(3);
    }

    @Test
    @Transactional
    public void customerJourney_endToEndShouldSucceed() throws Exception {
        var user = registerUser();
        String userToken = loginUser(user.email(), user.password());
        assertThat(userToken).isNotBlank();

        // ensure protected endpoint requires auth
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());

        String adminToken = loginAdmin();
        long productA = createProduct(adminToken, 10);
        long productB = createProduct(adminToken, 10);

        MvcResult browse = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode productsList = objectMapper.readTree(browse.getResponse().getContentAsString());
        Set<Long> productIds = new HashSet<>();
        for (JsonNode p : productsList) {
            productIds.add(p.get("id").asLong());
        }
        assertThat(productIds).contains(productA, productB);

        MvcResult detailsA = mockMvc.perform(get("/api/products/" + productA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode productAData = objectMapper.readTree(detailsA.getResponse().getContentAsString());
        assertThat(productAData.get("id").asLong()).isEqualTo(productA);
        assertThat(productAData.get("stock").asInt()).isEqualTo(10);

        String addA = String.format("{\"productId\":%d,\"quantity\":2}", productA);
        MvcResult cartARes = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(addA))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cartA = objectMapper.readTree(cartARes.getResponse().getContentAsString());
        long cartItemAId = cartA.get("cartItemId").asLong();
        assertThat(cartA.get("productId").asLong()).isEqualTo(productA);
        assertThat(cartA.get("quantity").asInt()).isEqualTo(2);

        String addB = String.format("{\"productId\":%d,\"quantity\":1}", productB);
        MvcResult cartBRes = mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(addB))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cartB = objectMapper.readTree(cartBRes.getResponse().getContentAsString());
        long cartItemBId = cartB.get("cartItemId").asLong();
        assertThat(cartB.get("productId").asLong()).isEqualTo(productB);
        assertThat(cartB.get("quantity").asInt()).isEqualTo(1);

        MvcResult cartBeforeUpdate = mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cartList = objectMapper.readTree(cartBeforeUpdate.getResponse().getContentAsString());
        assertThat(cartList.size()).isEqualTo(2);

        MvcResult updateRes = mockMvc.perform(put("/api/cart/" + cartItemAId + "/quantity")
                        .param("quantity", "3")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updatedItem = objectMapper.readTree(updateRes.getResponse().getContentAsString());
        assertThat(updatedItem.get("quantity").asInt()).isEqualTo(3);

        mockMvc.perform(delete("/api/cart/" + cartItemBId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        MvcResult cartAfterRemove = mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode cartAfter = objectMapper.readTree(cartAfterRemove.getResponse().getContentAsString());
        assertThat(cartAfter.size()).isEqualTo(1);
        assertThat(cartAfter.get(0).get("productId").asLong()).isEqualTo(productA);
        assertThat(cartAfter.get(0).get("quantity").asInt()).isEqualTo(3);

        long addressId = createAddress(userToken);
        String checkoutReq = String.format("{\"shippingAddressId\":%d}", addressId);
        MvcResult checkoutRes = mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(checkoutReq))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode orderNode = objectMapper.readTree(checkoutRes.getResponse().getContentAsString());
        long orderId = orderNode.get("id").asLong();
        assertThat(orderNode.get("status").asText()).isEqualTo("CREATED");
        assertThat(orderNode.get("items").size()).isEqualTo(1);
        assertThat(orderNode.get("items").get(0).get("productId").asLong()).isEqualTo(productA);
        assertThat(orderNode.get("items").get(0).get("quantity").asInt()).isEqualTo(3);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getUser().getEmail()).isEqualTo(user.email());

        int stockA = productRepository.findById(productA).orElseThrow().getStock();
        int stockB = productRepository.findById(productB).orElseThrow().getStock();
        assertThat(stockA).isEqualTo(7);
        assertThat(stockB).isEqualTo(10);

        MvcResult orderHistory = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode orders = objectMapper.readTree(orderHistory.getResponse().getContentAsString());
        assertThat(orders).isNotEmpty();
        boolean foundOrder = false;
        for (JsonNode item : orders) {
            if (item.get("id").asLong() == orderId) {
                foundOrder = true;
                break;
            }
        }
        assertThat(foundOrder).isTrue();

        MvcResult orderDetailRes = mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode orderDetail = objectMapper.readTree(orderDetailRes.getResponse().getContentAsString());
        assertThat(orderDetail.get("id").asLong()).isEqualTo(orderId);

        // Verify recommendation endpoint after purchase.
        MvcResult recommendationRes = mockMvc.perform(get("/api/recommendations/product/" + productA)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode recommendationNode = objectMapper.readTree(recommendationRes.getResponse().getContentAsString());
        if (!recommendationNode.isNull() && !recommendationNode.isMissingNode()) {
            assertThat(recommendationNode.get("productId").asLong()).isEqualTo(productA);
            JsonNode recommendations = recommendationNode.get("recommendations");
            assertThat(recommendations).isNotNull();
            assertThat(recommendations.isArray()).isTrue();
            if (recommendations.size() > 0) {
                assertThat(recommendations.size()).isGreaterThanOrEqualTo(1);
            }
        }

        // Logout endpoint is not implemented in this stateless JWT backend.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    private long createAddress(String token) throws Exception {
        String addressReq = "{\"street\":\"Main St 1\",\"city\":\"City\",\"country\":\"Country\",\"zipCode\":\"12345\"}";
        MvcResult addrRes = mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(addressReq))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(addrRes.getResponse().getContentAsString()).get("id").asLong();
    }
}
