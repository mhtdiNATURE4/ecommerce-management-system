package com.market.ecommerce.entity;

public enum PaymentStatus {
    PENDING,    // قيد الانتظار
    COMPLETED,  // مكتمل
    FAILED,     // فشل الدفع
    REFUNDED    // تم الاسترجاع
}