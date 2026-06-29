package com.market.imageproxy.security;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class HmacAuthFilterTest {

    private HmacAuthFilter filter;
    private final String secret = "test-secret-1234567890";

    @BeforeEach
    public void setup() throws Exception {
        filter = new HmacAuthFilter();
        // inject secret via reflection
        var f = HmacAuthFilter.class.getDeclaredField("hmacSecret");
        f.setAccessible(true);
        f.set(filter, secret);
    }

    private String computeSig(String payload) {
        byte[] h = org.apache.commons.codec.digest.HmacUtils.hmacSha256(secret, payload);
        return Hex.encodeHexString(h);
    }

    @Test
    public void correctSignaturePasses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        long ts = Instant.now().getEpochSecond();
        String payload = "payload-example";
        req.addHeader("X-Internal-Timestamp", String.valueOf(ts));
        req.addHeader("X-Internal-Payload", payload);
        req.addHeader("X-Internal-Signature", computeSig(payload));

        filter.doFilter(req, res, chain);
        assertEquals(200, res.getStatus());
    }

    @Test
    public void wrongSignatureRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        long ts = Instant.now().getEpochSecond();
        req.addHeader("X-Internal-Timestamp", String.valueOf(ts));
        req.addHeader("X-Internal-Payload", "payload-example");
        req.addHeader("X-Internal-Signature", "deadbeef");

        filter.doFilter(req, res, chain);
        assertEquals(401, res.getStatus());
    }

    @Test
    public void missingTimestampRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        String payload = "payload-example";
        req.addHeader("X-Internal-Payload", payload);
        req.addHeader("X-Internal-Signature", computeSig(payload));

        filter.doFilter(req, res, chain);
        assertEquals(401, res.getStatus());
    }

    @Test
    public void replayTimestampRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        long ts = Instant.now().minusSeconds(10000).getEpochSecond();
        String payload = "payload-example";
        req.addHeader("X-Internal-Timestamp", String.valueOf(ts));
        req.addHeader("X-Internal-Payload", payload);
        req.addHeader("X-Internal-Signature", computeSig(payload));

        filter.doFilter(req, res, chain);
        assertEquals(401, res.getStatus());
    }
}
