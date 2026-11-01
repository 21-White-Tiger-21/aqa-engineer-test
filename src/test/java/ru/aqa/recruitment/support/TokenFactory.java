package ru.aqa.recruitment.support;

import java.util.concurrent.atomic.AtomicLong;

public final class TokenFactory {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private TokenFactory() {
    }

    public static String validToken() {
        return "A%031d".formatted(SEQUENCE.incrementAndGet());
    }
}
