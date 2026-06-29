package com.market.ecommerce.config;

import com.market.ecommerce.entity.Category;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.entity.UserRole;
import com.market.ecommerce.repository.CategoryRepository;
import com.market.ecommerce.repository.ProductRepository;
import com.market.ecommerce.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@Profile("dev")
public class DataLoader {

    @Bean
    CommandLineRunner initData(UserRepository userRepo,
                               ProductRepository productRepo,
                               CategoryRepository categoryRepo,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            String demoEmail = "demo@example.com";
            if (!userRepo.existsByEmail(demoEmail)) {
                User user = User.builder()
                        .name("Demo User")
                        .email(demoEmail)
                        .password(passwordEncoder.encode("password"))
                        .role(UserRole.CUSTOMER)
                        .build();

                userRepo.save(user);
            }

            Category sampleCategory;
            if (categoryRepo.count() == 0) {
                var cat = new Category();
                cat.setName("Sample Category");
                sampleCategory = categoryRepo.save(cat);
            } else {
                sampleCategory = categoryRepo.findAll().stream().findFirst().orElse(null);
            }

            if (productRepo.count() == 0 && sampleCategory != null) {
                var p = new Product();
                p.setName("Sample Product A");
                p.setDescription("Demo product");
                p.setPrice(new BigDecimal("9.99"));
                p.setStock(10);
                p.setCategory(sampleCategory);
                productRepo.save(p);

                var p2 = new Product();
                p2.setName("Sample Product B");
                p2.setDescription("Demo product B");
                p2.setPrice(new BigDecimal("19.99"));
                p2.setStock(5);
                p2.setCategory(sampleCategory);
                productRepo.save(p2);
            }
        };
    }
}
