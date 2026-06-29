package com.market.ecommerce.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        Long totalUsers,
        Long totalOrders,
        BigDecimal totalRevenue,
        Long totalProducts,
        String bestSellingProduct,
        String topCustomer
) {}