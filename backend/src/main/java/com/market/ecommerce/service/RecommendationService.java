package com.market.ecommerce.service;

import com.market.ecommerce.dto.ProductRecommendationResponse;
import com.market.ecommerce.dto.RecommendationItemResponse;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.repository.OrderItemRepository;
import com.market.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public RecommendationService(OrderItemRepository orderItemRepository,
                                 ProductRepository productRepository) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    public List<ProductRecommendationResponse> buildRecommendations() {

        List<Object[]> data = orderItemRepository.findOrderProductPairs(OrderStatus.COMPLETED);

        // استخدام Set لضمان عدم تكرار نفس المنتج داخل نفس الطلب
        Map<Long, Set<Long>> orderMap = new HashMap<>();

        for (Object[] row : data) {
            Long orderId = (Long) row[0];
            Long productId = (Long) row[1];
            orderMap.computeIfAbsent(orderId, k -> new HashSet<>()).add(productId);
        }

        Map<String, Integer> pairCount = new HashMap<>();
        Map<Long, Integer> productCount = new HashMap<>();
        Set<Long> allProductIds = new HashSet<>(); // لتجميع جميع معرفات المنتجات المطلوبة

        for (Set<Long> products : orderMap.values()) {
            for (Long p1 : products) {
                productCount.put(p1, productCount.getOrDefault(p1, 0) + 1);
                allProductIds.add(p1);

                for (Long p2 : products) {
                    if (!p1.equals(p2)) {
                        String key = p1 + "-" + p2;
                        pairCount.put(key, pairCount.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }

        // تحسين الأداء الجوهري: جلب جميع المنتجات دفعة واحدة لتجنب مشكلة N+1 Query
        Map<Long, Product> productMap = productRepository.findAllById(allProductIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, List<RecommendationItemResponse>> finalMap = new HashMap<>();

        for (Map.Entry<String, Integer> entry : pairCount.entrySet()) {
            String[] parts = entry.getKey().split("-");
            Long p1 = Long.parseLong(parts[0]);
            Long p2 = Long.parseLong(parts[1]);

            int pair = entry.getValue();
            int total = productCount.get(p1);
            double confidence = (double) pair / total;

            // جلب المنتج من الذاكرة بدلاً من قاعدة البيانات
            Product recommendedProduct = productMap.get(p2);

            if (recommendedProduct == null) {
                continue;
            }

            RecommendationItemResponse item = new RecommendationItemResponse(
                    recommendedProduct.getId(),
                    recommendedProduct.getName(),
                    BigDecimal.valueOf(confidence)
            );

            finalMap.computeIfAbsent(p1, k -> new ArrayList<>()).add(item);
        }

        List<ProductRecommendationResponse> response = new ArrayList<>();

        for (Map.Entry<Long, List<RecommendationItemResponse>> entry : finalMap.entrySet()) {
            Long productId = entry.getKey();
            Product product = productMap.get(productId);

            if (product == null) {
                continue;
            }

            List<RecommendationItemResponse> recommendations = entry.getValue();

            // استخدام compareTo للحفاظ على دقة ترتيب الـ BigDecimal
            recommendations.sort((a, b) -> b.score().compareTo(a.score()));

            response.add(new ProductRecommendationResponse(
                    product.getId(),
                    product.getName(),
                    recommendations
            ));
        }

        return response;
    }

    public ProductRecommendationResponse getRecommendationsForProduct(Long productId) {
        return buildRecommendations()
                .stream()
                .filter(r -> r.productId().equals(productId))
                .findFirst()
                .orElse(null);
    }
}