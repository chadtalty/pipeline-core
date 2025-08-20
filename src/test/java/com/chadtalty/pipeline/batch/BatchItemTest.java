package com.chadtalty.pipeline.batch;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BatchItemTest {
    @Test
    void ofKey_only() {
        var it = BatchItem.of("K");
        assertEquals("K", it.key());
        assertTrue(it.params().isEmpty());
    }

    @Test
    void ofKeySingleParam() {
        var it = BatchItem.of("K", "p", 42);
        assertEquals("K", it.key());
        assertEquals(1, it.params().size());
        assertEquals(42, it.params().get("p"));
    }
}
