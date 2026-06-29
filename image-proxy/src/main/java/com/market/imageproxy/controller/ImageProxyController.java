package com.market.imageproxy.controller;

import com.market.imageproxy.service.ImageProxyService;
import com.market.imageproxy.util.ImageProxyException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/v1")
public class ImageProxyController {

    private final ImageProxyService service;

    public ImageProxyController(ImageProxyService service) {
        this.service = service;
    }

    public record ThumbnailRequest(
            String url,
            Integer width,
            Integer height,
            String format,
            Long timestamp
    ) {}

    @PostMapping("/thumbnail")
    public ResponseEntity<?> thumbnail(@Valid @RequestBody ThumbnailRequest req) {
        try {
            var result = service.fetchAndResize(req.url(), req.width(), req.height(), req.format());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(result.contentType()));
            headers.setCacheControl("max-age=3600, public");
            return new ResponseEntity<>(result.bytes(), headers, HttpStatus.OK);
        } catch (ImageProxyException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "upstream_error"));
        }
    }
}
