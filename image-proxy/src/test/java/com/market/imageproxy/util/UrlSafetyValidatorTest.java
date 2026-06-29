package com.market.imageproxy.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class UrlSafetyValidatorTest {

    private final UrlSafetyValidator validator = new UrlSafetyValidator();

    @Test
    public void blocksRfc1918PrivateIpv4() {
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://10.0.0.1/image.png")));
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://192.168.1.1/image.png")));
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://172.16.0.1/image.png")));
    }

    @Test
    public void blocksLoopback() {
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://127.0.0.1/image.png")));
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://[::1]/image.png")));
    }

    @Test
    public void blocksLinkLocalAndMetadata() {
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://169.254.1.1/image.png")));
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://169.254.169.254/")));
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://[fe80::1]/image.png")));
    }

    @Test
    public void rejectsMalformedHost() {
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://\u0000/")));
    }

    @Test
    public void ipv6MappedIpv4Blocked() {
        // IPv4-mapped IPv6 for 127.0.0.1 ::ffff:127.0.0.1
        assertThrows(ImageProxyException.class, () -> validator.validate(URI.create("http://[::ffff:127.0.0.1]/img")));
    }
}
