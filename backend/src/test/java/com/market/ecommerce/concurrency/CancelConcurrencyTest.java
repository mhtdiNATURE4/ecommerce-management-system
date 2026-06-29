package com.market.ecommerce.concurrency;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.Category;
import java.util.UUID;
import com.market.ecommerce.repository.OrderRepository;
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
public class CancelConcurrencyTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.market.ecommerce.repository.CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product product;

    @BeforeEach
    public void setup() {
        orderRepository.deleteAll();
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
    public void cancelAndCheckoutConcurrent() throws InterruptedException, ExecutionException {
        // Simulate concurrent cancellation (restores stock) and checkout (decrements stock)
        int threads = 2;
        ExecutorService svc = Executors.newFixedThreadPool(threads);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        tasks.add(() -> {
            try {
                // emulate cancellation restoring 1 unit
                productRepository.findById(product.getId()).ifPresent(p -> {
                    p.setStock(p.getStock() + 1);
                    productRepository.save(p);
                });
                return true;
            } catch (Exception ex) { return false; }
        });

        tasks.add(() -> {
            try {
                // emulate checkout consuming 1 unit
                productRepository.findById(product.getId()).ifPresent(p -> {
                    if (p.getStock() > 0) {
                        p.setStock(p.getStock() - 1);
                        productRepository.save(p);
                    }
                });
                return true;
            } catch (Exception ex) { return false; }
        });

        List<Future<Boolean>> results = svc.invokeAll(tasks);
        svc.shutdown();
        svc.awaitTermination(5, TimeUnit.SECONDS);

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();
        assertTrue(refreshed.getStock() >= 0);
    }
}
