package com.chadtalty.pipeline.batch;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BatchOptionsTest {
    @Test
    void defaults_areSane() {
        var d = BatchOptions.defaults();
        assertEquals(4, d.maxParallel());
        assertEquals(BatchOptions.FailurePolicy.CONTINUE, d.onItemFailure());
        assertEquals(10, d.perItemTimeout().toMinutes());
    }
}
