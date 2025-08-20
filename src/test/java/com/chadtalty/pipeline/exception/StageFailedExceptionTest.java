package com.chadtalty.pipeline.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StageFailedExceptionTest {
    @Test
    void messageOnly() {
        var ex = new StageFailedException("bad");
        assertEquals("bad", ex.getMessage());
    }

    @Test
    void withCause() {
        var cause = new IllegalStateException("root");
        var ex = new StageFailedException("bad", cause);
        assertSame(cause, ex.getCause());
    }
}
