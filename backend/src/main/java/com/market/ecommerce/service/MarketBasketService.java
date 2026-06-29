package com.market.ecommerce.service;

import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.repository.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketBasketService {

    private final OrderItemRepository orderItemRepository;

    public MarketBasketService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public Map<String, Double> analyze() {

        List<Object[]> data = orderItemRepository.findOrderProductPairs(OrderStatus.COMPLETED);
        Map<Long, List<Long>> orderMap = new HashMap<>();

        for (Object[] row : data) {
            Long orderId = (Long) row[0];
            Long productId = (Long) row[1];
            orderMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(productId);
        }

        Map<String, Integer> pairCount = new HashMap<>();
        Map<Long, Integer> productCount = new HashMap<>();

        for (List<Long> products : orderMap.values()) {
            // نستخدم Set لضمان عدم احتساب المنتج مرتين في نفس الطلب
            Set<Long> uniqueProducts = new HashSet<>(products);

            for (Long p1 : uniqueProducts) {
                productCount.put(p1, productCount.getOrDefault(p1, 0) + 1);

                for (Long p2 : uniqueProducts) {
                    if (!p1.equals(p2)) {
                        // ترتيب المٌعرفات لضمان أن (1-2) هي نفسها (2-1)
                        Long first = Math.min(p1, p2);
                        Long second = Math.max(p1, p2);
                        String key = first + "-" + second;

                        // تمت إضافة هذا الشرط لتفادي التكرار المزدوج في الحلقة
                        if(p1.equals(first)){
                            pairCount.put(key, pairCount.getOrDefault(key, 0) + 1);
                        }
                    }
                }
            }
        }

        Map<String, Double> confidenceMap = new HashMap<>();

        for (String key : pairCount.keySet()) {
            String[] parts = key.split("-");
            Long p1 = Long.parseLong(parts[0]);
            Long p2 = Long.parseLong(parts[1]);

            int pair = pairCount.get(key);

            // حساب الثقة في الاتجاهين
            double conf1 = (double) pair / productCount.get(p1);
            double conf2 = (double) pair / productCount.get(p2);

            confidenceMap.put(p1 + " -> " + p2, conf1);
            confidenceMap.put(p2 + " -> " + p1, conf2);
        }

        return confidenceMap;
    }
}