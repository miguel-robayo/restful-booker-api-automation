package com.credibanco.qa.restfulbooker.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Line-buffered stream that masks secrets before writing each line to the target (FW-01). Rest Assured's conditional
 * log goes through it because it prints request bodies (e.g. the {@code /auth} password) and cannot blacklist them.
 *
 * <p>The buffer is per thread, so lines of parallel scenarios are never mixed or split.
 */
public final class MaskingOutputStream extends OutputStream {

    private static final int NEW_LINE = '\n';

    private final OutputStream target;
    private final ThreadLocal<ByteArrayOutputStream> line = ThreadLocal.withInitial(ByteArrayOutputStream::new);

    public MaskingOutputStream(OutputStream target) {
        this.target = target;
    }

    @Override
    public void write(int b) throws IOException {
        ByteArrayOutputStream buffer = line.get();
        buffer.write(b);
        if (b == NEW_LINE) {
            emit(buffer);
        }
    }

    @Override
    public void flush() throws IOException {
        ByteArrayOutputStream buffer = line.get();
        if (buffer.size() > 0) {
            emit(buffer);
        }
        synchronized (target) {
            target.flush();
        }
    }

    private void emit(ByteArrayOutputStream buffer) throws IOException {
        String masked = SensitiveDataMasker.maskLine(buffer.toString(StandardCharsets.UTF_8));
        buffer.reset();
        synchronized (target) {
            target.write(masked.getBytes(StandardCharsets.UTF_8));
        }
    }
}
