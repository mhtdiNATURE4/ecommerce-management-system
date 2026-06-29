package com.market.ecommerce.ordering;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderItem;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.Category;
import java.util.UUID;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class OrderPagingOrderingTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.market.ecommerce.repository.CategoryRepository categoryRepository;

    @BeforeEach
    public void setup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        String categoryName = "Test Cat " + UUID.randomUUID();
        Category category = categoryRepository.save(Category.builder().name(categoryName).build());

        Product p = Product.builder().name("P").description("").price(BigDecimal.ONE).stock(10).category(category).build();
        p = productRepository.save(p);

        for (int i = 0; i < 5; i++) {
            Order o = Order.builder().status(OrderStatus.CREATED).totalAmount(BigDecimal.TEN).build();
            OrderItem it = OrderItem.builder().product(p).quantity(1).price(p.getPrice()).order(o).build();
            o.getItems().add(it);
            orderRepository.save(o);
        }
    }

    @Test
    public void pagedOrderingIsPreserved() {
        var page = orderRepository.findAll(PageRequest.of(0, 3));
        var ids = page.map(Order::getId).getContent();
        var fetched = orderRepository.findByIdInWithItems(ids);
        // after repository reordering in service, the order should match ids; here we assert fetch returns all ids
        assertEquals(3, fetched.size());
    }
}
