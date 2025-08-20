package com.chadtalty.pipeline.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.api.StageResult;
import com.chadtalty.pipeline.runner.StageInvocation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StageInterceptorDefaultsTest {

    @Test
    void defaultsAreNoopAndOrderIsZero() {
        StageInterceptor i = new StageInterceptor() {};
        var inv = new StageInvocation("p", "s", 1, 1);
        var ctx = new StageContext();

        Optional<StageResult> before = i.before(inv, ctx);
        assertTrue(before.isEmpty());

        // just invoke to cover lines
        i.after(inv, ctx, StageResult.success(), 123);
        i.onError(inv, ctx, new RuntimeException("x"), 456);

        assertEquals(0, i.getOrder());
    }
}
