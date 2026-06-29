package com.market.ecommerce.concurrency;

import com.market.ecommerce.entity.Category;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.repository.CategoryRepository;
import com.market.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NegativeStockGuardTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product product;

    @BeforeEach
    public void setup() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(Category.builder().name("Cat " + UUID.randomUUID()).build());

        product = Product.builder()
                .name("NegGuard Product")
                .description("desc")
                .price(java.math.BigDecimal.ONE)
                .stock(1)
                .imageUrl("")
                .category(category)
                .build();

        product = productRepository.save(product);
    }

    @Test
    public void guardPreventsNegativeStock() throws InterruptedException {
        int initial = product.getStock();
        int threads = 10;
        ExecutorService svc = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        Runnable task = () -> {
            try {
                ready.countDown();
                start.await();
                // attempt to decrement more than available
                productRepository.decrementStockIfAvailable(product.getId(), 2);
            } catch (Exception ignored) {}
        };

        for (int i = 0; i < threads; i++) svc.submit(task);

        ready.await();
        start.countDown();

        svc.shutdown();
        svc.awaitTermination(10, TimeUnit.SECONDS);

        Integer finalStock = productRepository.findStockById(product.getId());
        assertNotNull(finalStock);
        assertEquals(initial, finalStock.intValue(), "Stock should remain unchanged when all attempts request more than available");
        assertTrue(finalStock >= 0, "Final stock must not be negative");
    }
}
