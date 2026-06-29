package com.market.ecommerce.concurrency;

import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.Category;
import java.util.UUID;
import com.market.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CheckoutConcurrencyTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.market.ecommerce.repository.CategoryRepository categoryRepository;

    private Product product;

    @BeforeEach
    public void setup() {
        productRepository.deleteAll();

        String categoryName = "Test Cat " + UUID.randomUUID();
        Category category = categoryRepository.save(Category.builder().name(categoryName).build());

        product = Product.builder()
            .name("Concurrent Product")
            .description("desc")
            .price(java.math.BigDecimal.TEN)
            .stock(5)
            .imageUrl("")
            .category(category)
            .build();

        product = productRepository.save(product);
    }

    @Test
    public void concurrentPurchasesDoNotOversell() throws InterruptedException, ExecutionException {
        int threads = 10;
        ExecutorService svc = Executors.newFixedThreadPool(threads);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                try {
                    // each task loads product, decrements if available
                    Product p = productRepository.findById(product.getId()).orElseThrow();
                    if (p.getStock() > 0) {
                        p.setStock(p.getStock() - 1);
                        productRepository.save(p);
                        return true;
                    }
                    return false;
                } catch (Exception ex) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> results = svc.invokeAll(tasks);
        svc.shutdown();
        svc.awaitTermination(10, TimeUnit.SECONDS);

        long success = results.stream().filter(r -> {
            try { return r.get(); } catch (Exception e) { return false; }
        }).count();

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();

        assertTrue(success <= 5, "At most initial stock units should be purchased");
        assertTrue(refreshed.getStock() >= 0, "Stock should never be negative");
        assertEquals(5 - success, refreshed.getStock().longValue());
    }
}
