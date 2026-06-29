package com.market.imageproxy.controller;

import com.market.imageproxy.service.ImageProxyService;
import com.market.imageproxy.util.ImageProxyException;
import com.market.imageproxy.util.UrlSafetyValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public class ImageProxyControllerTest {

    private static HttpServer server;
    private static int port;

    @BeforeAll
    public static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    public void validPublicImageReturns200() throws Exception {
        // serve a tiny PNG
        server.createContext("/img.png", exchange -> {
            byte[] img = new byte[]{(byte)137,80,78,71};
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, img.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(img); }
        });

        UrlSafetyValidator validator = new UrlSafetyValidator() {
            @Override public void validate(java.net.URI uri) throws ImageProxyException { /* allow */ }
        };

        ImageProxyService svc = new ImageProxyService(validator);
        var controller = new ImageProxyController(svc);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        String url = "http://localhost:" + port + "/img.png";
        String body = String.format("{\"url\":\"%s\",\"width\":48,\"height\":48}", url);

        mvc.perform(post("/v1/thumbnail").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    public void privateIpReturns403() throws Exception {
        UrlSafetyValidator validator = new UrlSafetyValidator() {
            @Override public void validate(java.net.URI uri) throws ImageProxyException { throw new ImageProxyException(org.springframework.http.HttpStatus.FORBIDDEN, "blocked_ip"); }
        };

        ImageProxyService svc = new ImageProxyService(validator);
        var controller = new ImageProxyController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        String body = "{\"url\":\"http://10.0.0.1/image.png\"}";

        mvc.perform(post("/v1/thumbnail").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    public void redirectToPrivateIpBlocked() throws Exception {
        // context /r redirects to http://10.0.0.1/target
        server.createContext("/r", exchange -> {
            exchange.getResponseHeaders().set("Location", "http://10.0.0.1/target");
            exchange.sendResponseHeaders(302, -1);
        });

        UrlSafetyValidator validator = new UrlSafetyValidator() {
            @Override public void validate(java.net.URI uri) throws ImageProxyException {
                if (uri.getHost().equals("10.0.0.1")) throw new ImageProxyException(org.springframework.http.HttpStatus.FORBIDDEN, "blocked_ip");
            }
        };

        ImageProxyService svc = new ImageProxyService(validator);
        var controller = new ImageProxyController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        String url = "http://localhost:" + port + "/r";
        String body = String.format("{\"url\":\"%s\"}", url);

        mvc.perform(post("/v1/thumbnail").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    public void nonImageContentReturns422() throws Exception {
        server.createContext("/text", exchange -> {
            String txt = "hello";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, txt.length());
            try (OutputStream os = exchange.getResponseBody()) { os.write(txt.getBytes()); }
        });

        UrlSafetyValidator validator = new UrlSafetyValidator() {
            @Override public void validate(java.net.URI uri) throws ImageProxyException { /* allow */ }
        };

        ImageProxyService svc = new ImageProxyService(validator);
        var controller = new ImageProxyController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        String url = "http://localhost:" + port + "/text";
        String body = String.format("{\"url\":\"%s\"}", url);

        mvc.perform(post("/v1/thumbnail").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void oversizedResponseTriggers413() throws Exception {
        server.createContext("/big", exchange -> {
            byte[] big = new byte[3000];
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, big.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(big); }
        });

        UrlSafetyValidator validator = new UrlSafetyValidator() {
            @Override public void validate(java.net.URI uri) throws ImageProxyException { /* allow */ }
        };

        ImageProxyService svc = new ImageProxyService(validator);
        // lower maxBytes for test
        var svcField = ImageProxyService.class.getDeclaredField("maxBytes");
        svcField.setAccessible(true);
        svcField.setInt(svc, 1024);

        var controller = new ImageProxyController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        String url = "http://localhost:" + port + "/big";
        String body = String.format("{\"url\":\"%s\"}", url);

        mvc.perform(post("/v1/thumbnail").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPayloadTooLarge());
    }
}
