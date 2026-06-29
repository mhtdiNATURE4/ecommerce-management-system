package com.market.ecommerce;

import com.market.ecommerce.dto.LoginRequest;
import com.market.ecommerce.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void loginAndAccessProtectedEndpoint() {
        // Register a new user then login
        String email = "testuser@example.com";
        String password = "password";

        var reg = new com.market.ecommerce.dto.RegisterRequest("Test User", email, password);
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        LoginRequest request = new LoginRequest(email, password);

        ResponseEntity<AuthResponse> res = restTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        String token = res.getBody().token();
        assertThat(token).isNotEmpty();

        // Use token to access protected endpoint (profile)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> profile = restTemplate.exchange("/api/user/profile", HttpMethod.GET, entity, String.class);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
