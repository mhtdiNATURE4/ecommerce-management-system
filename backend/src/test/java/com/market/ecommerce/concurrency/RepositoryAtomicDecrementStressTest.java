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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RepositoryAtomicDecrementStressTest {

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
                .name("Stress Product")
                .description("desc")
                .price(java.math.BigDecimal.TEN)
                .stock(50)
                .imageUrl("")
                .category(category)
                .build();

        product = productRepository.save(product);
    }

    @Test
    public void atomicDecrementDoesNotOversell() throws InterruptedException {
        int initial = product.getStock();
        int threads = 200;
        int poolSize = 50;
        ExecutorService svc = Executors.newFixedThreadPool(poolSize);
        CountDownLatch ready = new CountDownLatch(poolSize);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                try {
                    ready.countDown();
                    start.await();
                    int rows = productRepository.decrementStockIfAvailable(product.getId(), 1);
                    return rows;
                } catch (Exception ex) {
                    return 0;
                }
            });
        }

        List<Future<Integer>> futures = new ArrayList<>();
        for (Callable<Integer> t : tasks) futures.add(svc.submit(t));

        // wait for a bounded time for worker threads to start to avoid deadlock
        boolean readyAll = ready.await(10, TimeUnit.SECONDS);
        if (!readyAll) {
            System.err.println("Warning: not all tasks reported ready within timeout. Proceeding to start anyway. remaining=" + ready.getCount());
        }
        start.countDown();

        svc.shutdown();
        boolean terminated = svc.awaitTermination(30, TimeUnit.SECONDS);
        if (!terminated) {
            System.err.println("Executor did not terminate within timeout, attempting shutdownNow");
            svc.shutdownNow();
        }

        int success = 0;
        for (Future<Integer> f : futures) {
            try {
                Integer r = f.get(5, TimeUnit.SECONDS);
                if (r == 1) success++;
            } catch (TimeoutException te) {
                System.err.println("A worker future timed out waiting for result; cancelling.");
                f.cancel(true);
            } catch (CancellationException ce) {
                System.err.println("A worker future was cancelled.");
            } catch (Exception ignored) {
            }
        }

        Integer finalStock = productRepository.findStockById(product.getId());
        assertNotNull(finalStock);
        assertTrue(success <= initial, "Should not sell more than initial stock");
        assertEquals(initial - success, finalStock.intValue());
        assertTrue(finalStock >= 0, "Final stock must not be negative");
    }
}
