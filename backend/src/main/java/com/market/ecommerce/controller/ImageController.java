package com.market.ecommerce.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @GetMapping(value = "/placeholder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> placeholder(@RequestParam String url) {
        try {
            URI uri = URI.create(url);
            try (InputStream in = uri.toURL().openStream()) {
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "invalid image"));
            }

            int targetSize = 48; // small thumbnail
            int ow = original.getWidth();
            int oh = original.getHeight();
            int nw = targetSize;
            int nh = Math.max(1, (int) ((double) targetSize * oh / ow));
            if (oh > ow) {
                nh = targetSize;
                nw = Math.max(1, (int) ((double) targetSize * ow / oh));
            }

            BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, nw, nh, null);
            g.dispose();

            // encode to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            String dataUrl = "data:image/jpeg;base64," + base64;

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(6)))
                    .body(Map.of("data", dataUrl));

            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "failed to generate placeholder"));
        }
    }
}
