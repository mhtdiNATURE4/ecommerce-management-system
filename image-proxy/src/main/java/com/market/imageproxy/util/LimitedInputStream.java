package com.market.imageproxy.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {

    private final long maxBytes;
    private long read = 0;

    public LimitedInputStream(InputStream in, long maxBytes) {
        super(in);
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        if (read >= maxBytes) throw new IOException("max_bytes_exceeded");
        int b = super.read();
        if (b != -1) read++;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        long allowed = Math.min(len, maxBytes - read);
        if (allowed <= 0) throw new IOException("max_bytes_exceeded");
        int r = super.read(b, off, (int) allowed);
        if (r > 0) read += r;
        return r;
    }
}
