package com.chadtalty.pipeline.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StageResultFactoriesTest {

    @Test
    void factoriesProduceExpectedStatuses() {
        assertEquals(StageStatus.CONTINUE, StageResult.success().status());
        assertEquals(StageStatus.SKIP, StageResult.skip("because").status());
        assertEquals(StageStatus.RETRY, StageResult.retry("try again").status());
        assertEquals(StageStatus.STOP, StageResult.stop("halt").status());

        assertEquals("because", StageResult.skip("because").message());
        assertNull(StageResult.success().message());
    }
}
