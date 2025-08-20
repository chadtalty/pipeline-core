package com.chadtalty.pipeline.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import com.chadtalty.pipeline.runner.PipelineDefinitionSource;
import com.chadtalty.pipeline.runner.PipelineRunner;
import com.chadtalty.pipeline.runner.StageInvocation;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class StageInterceptorErrorPathTest {

    private static PipelineDefinitionSource defs(StageStep<StageContext> step) {
        var h = new PipelineDefinitionSource.StageHolder("s", 1, step, c -> true);
        return n -> List.of(h);
    }

    @Test
    void onErrorInvoked_whenStageThrows_andAfterNotCalled() {
        AtomicBoolean before = new AtomicBoolean(false);
        AtomicBoolean after = new AtomicBoolean(false);
        AtomicBoolean onErr = new AtomicBoolean(false);

        StageInterceptor spy = new StageInterceptor() {
            @Override
            public java.util.Optional<StageResult> before(StageInvocation inv, StageContext ctx) {
                before.set(true);
                return java.util.Optional.empty();
            }

            @Override
            public void after(StageInvocation inv, StageContext ctx, StageResult r, long d) {
                after.set(true);
            }

            @Override
            public void onError(StageInvocation inv, StageContext ctx, Exception ex, long d) {
                onErr.set(true);
            }
        };

        StageStep<StageContext> failing = ctx -> {
            throw new RuntimeException("boom");
        };
        var runner = new PipelineRunner(defs(failing), List.of(spy), 0, 0, PipelineRunner.FailurePolicy.CONTINUE);

        assertDoesNotThrow(() -> runner.run("p", new StageContext()));
        assertTrue(before.get(), "before should run");
        assertTrue(onErr.get(), "onError should run");
        // after() is not called on exceptions (runner calls onError instead)
        assertFalse(after.get(), "after should not run on exception");
    }
}
