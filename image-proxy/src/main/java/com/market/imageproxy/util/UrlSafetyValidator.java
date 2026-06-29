package com.market.imageproxy.util;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

import java.net.InetAddress;
import java.net.URI;

@Component
public class UrlSafetyValidator {

    public void validate(URI uri) throws ImageProxyException {
        try {
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new ImageProxyException(HttpStatus.BAD_REQUEST, "invalid_host");
            }

            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress a : addrs) {
                if (isBlocked(a)) {
                    throw new ImageProxyException(HttpStatus.FORBIDDEN, "blocked_ip");
                }
            }
        } catch (ImageProxyException e) {
            throw e;
        } catch (Exception e) {
            throw new ImageProxyException(HttpStatus.BAD_REQUEST, "dns_error");
        }
    }

    private boolean isBlocked(InetAddress a) {
        if (a.isAnyLocalAddress()) return true;
        if (a.isLoopbackAddress()) return true;
        if (a.isLinkLocalAddress()) return true;
        if (a.isSiteLocalAddress()) return true; // RFC1918 and site-local

        // block EC2 metadata address and similar
        String host = a.getHostAddress();
        if (host != null && host.equals("169.254.169.254")) return true;

        // IPv6 equivalents: block unspecified and loopback
        if (host != null && (host.equals("::1") || host.equals("::"))) return true;

        return false;
    }
}
