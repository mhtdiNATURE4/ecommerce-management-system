package com.market.imageproxy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class HmacAuthFilter extends OncePerRequestFilter {

    @Value("${image.proxy.hmac.secret:}")
    private String hmacSecret;

    // header names
    private static final String SIG_HEADER = "X-Internal-Signature";
    private static final String TS_HEADER = "X-Internal-Timestamp";

    private static final long ALLOWED_SKEW_SECONDS = 120; // 2 minutes

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("missing_hmac_secret");
            return;
        }

        String sig = request.getHeader(SIG_HEADER);
        String ts = request.getHeader(TS_HEADER);
        if (sig == null || ts == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("missing_signature");
            return;
        }

        try {
            long timestamp = Long.parseLong(ts);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - timestamp) > ALLOWED_SKEW_SECONDS) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("stale_timestamp");
                return;
            }
        } catch (NumberFormatException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("bad_timestamp");
            return;
        }

        // Compute HMAC from request body: we require clients to sign the canonical string placed in header X-Internal-Payload
        String payload = request.getHeader("X-Internal-Payload");
        if (payload == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("missing_payload_header");
            return;
        }

        byte[] hmac = HmacUtils.hmacSha256(hmacSecret, payload);
        String expected = Hex.encodeHexString(hmac);
        if (!expected.equalsIgnoreCase(sig)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("invalid_signature");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
