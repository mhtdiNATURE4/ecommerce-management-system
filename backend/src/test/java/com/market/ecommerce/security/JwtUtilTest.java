package com.market.ecommerce.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void generateAndValidateToken() {
        String token = jwtUtil.generateToken("user@example.com", "CUSTOMER");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
        assertEquals("CUSTOMER", jwtUtil.extractRole(token));
    }

    @Test
    public void invalidTokenIsRejected() {
        String token = jwtUtil.generateToken("user2@example.com", "ADMIN");
        String tampered = token + "x";
        assertFalse(jwtUtil.validateToken(tampered));
    }
}
