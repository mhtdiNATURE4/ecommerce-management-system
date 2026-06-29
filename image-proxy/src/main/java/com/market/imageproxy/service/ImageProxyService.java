package com.market.imageproxy.service;

import com.market.imageproxy.util.ImageProxyException;
import com.market.imageproxy.util.LimitedInputStream;
import com.market.imageproxy.util.UrlSafetyValidator;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

@Service
public class ImageProxyService {

    private final UrlSafetyValidator urlSafetyValidator;

    @Value("${image.proxy.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${image.proxy.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Value("${image.proxy.max-bytes:2000000}")
    private int maxBytes;

    @Value("${image.proxy.max-width:800}")
    private int maxWidth;

    @Value("${image.proxy.max-height:800}")
    private int maxHeight;

    public ImageProxyService(UrlSafetyValidator urlSafetyValidator) {
        this.urlSafetyValidator = urlSafetyValidator;
    }

    public record ImageResult(byte[] bytes, String contentType) {}

    public ImageResult fetchAndResize(String urlString, Integer width, Integer height, String format) throws ImageProxyException {
        if (urlString == null || urlString.isBlank()) {
            throw new ImageProxyException(HttpStatus.BAD_REQUEST, "missing_url");
        }

        int targetW = (width == null) ? 48 : Math.min(width, maxWidth);
        int targetH = (height == null) ? 48 : Math.min(height, maxHeight);

        try {
            URI uri = URI.create(urlString);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new ImageProxyException(HttpStatus.BAD_REQUEST, "invalid_scheme");
            }

            // Validate host/IP before connecting
            urlSafetyValidator.validate(uri);

            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty(HttpHeaders.USER_AGENT, "image-proxy/1.0");
            conn.setRequestProperty("Accept", "image/*,*/*;q=0.1");

            int status = conn.getResponseCode();
            // handle redirects manually and validate target IP
            int redirectCount = 0;
            while (status >= 300 && status < 400 && redirectCount < 3) {
                String loc = conn.getHeaderField(HttpHeaders.LOCATION);
                if (loc == null) break;
                URI next = uri.resolve(loc);
                urlSafetyValidator.validate(next);
                url = next.toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(connectTimeoutMs);
                conn.setReadTimeout(readTimeoutMs);
                conn.setRequestProperty(HttpHeaders.USER_AGENT, "image-proxy/1.0");
                conn.setRequestProperty("Accept", "image/*,*/*;q=0.1");
                status = conn.getResponseCode();
                redirectCount++;
            }

            if (status >= 400) {
                throw new ImageProxyException(HttpStatus.BAD_GATEWAY, "upstream_error");
            }

            String contentType = conn.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new ImageProxyException(HttpStatus.UNPROCESSABLE_ENTITY, "unsupported_mime");
            }

            try (InputStream in = new LimitedInputStream(conn.getInputStream(), maxBytes)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // perform resize using Thumbnailator
                Thumbnails.of(in)
                        .size(targetW, targetH)
                        .outputFormat((format == null || format.isBlank()) ? contentType.split("/",2)[1] : format)
                        .toOutputStream(baos);

                byte[] out = baos.toByteArray();
                String outContentType = conn.getContentType();
                return new ImageResult(out, outContentType != null ? outContentType : "image/jpeg");
            }

        } catch (ImageProxyException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ImageProxyException(HttpStatus.BAD_REQUEST, "invalid_url");
        } catch (java.io.IOException e) {
            // map LimitedInputStream overflow to 413 Payload Too Large
            if (e.getMessage() != null && e.getMessage().contains("max_bytes_exceeded")) {
                throw new ImageProxyException(HttpStatus.PAYLOAD_TOO_LARGE, "max_bytes_exceeded");
            }
            throw new ImageProxyException(HttpStatus.BAD_GATEWAY, "upstream_io_error");
        } catch (Exception e) {
            throw new ImageProxyException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error");
        }
    }
}
