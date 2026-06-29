package com.market.ecommerce.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String message,
        Map<String, String> errors // خاص بأخطاء التحقق من المدخلات (Validation)
) {}