package com.market.imageproxy.util;

import org.springframework.http.HttpStatus;

public class ImageProxyException extends Exception {
    private final HttpStatus status;

    public ImageProxyException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
