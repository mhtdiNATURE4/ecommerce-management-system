package com.market.imageproxy.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class LimitedInputStreamTest {

    @Test
    public void enforcesMaxBytes() throws IOException {
        byte[] data = new byte[1024];
        var in = new LimitedInputStream(new ByteArrayInputStream(data), 100);
        byte[] buf = new byte[50];
        int r1 = in.read(buf);
        assertEquals(50, r1);
        int r2 = in.read(buf);
        assertEquals(50, r2);
        // next read should throw
        assertThrows(IOException.class, () -> in.read());
    }
}
