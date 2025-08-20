package com.chadtalty.pipeline.interceptors;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import com.chadtalty.pipeline.runner.PipelineDefinitionSource;
import com.chadtalty.pipeline.runner.PipelineRunner;
import com.chadtalty.pipeline.runner.StageInvocation;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StageInterceptorOrderTest {

    private static PipelineDefinitionSource defs(StageStep<StageContext> step) {
        var holder = new PipelineDefinitionSource.StageHolder("s", 1, step, (c) -> true);
        return (name) -> List.of(holder);
    }

    @Test
    void beforeRunsInOrder_afterRunsInReverse_andCanShortCircuit() {
        List<String> calls = new ArrayList<>();

        StageInterceptor a = new StageInterceptor() {
            @Override
            public int getOrder() {
                return 1;
            }

            @Override
            public java.util.Optional<StageResult> before(StageInvocation inv, StageContext ctx) {
                calls.add("a.before");
                return Optional.empty();
            }

            @Override
            public void after(StageInvocation inv, StageContext ctx, StageResult r, long d) {
                calls.add("a.after");
            }
        };

        StageInterceptor b = new StageInterceptor() {
            @Override
            public int getOrder() {
                return 2;
            }

            @Override
            public java.util.Optional<StageResult> before(StageInvocation inv, StageContext ctx) {
                calls.add("b.before");
                // short-circuit: skip real stage execution
                return Optional.of(StageResult.skip("skip"));
            }

            @Override
            public void after(StageInvocation inv, StageContext ctx, StageResult r, long d) {
                calls.add("b.after");
            }
        };

        AtomicInteger executed = new AtomicInteger();
        StageStep<StageContext> step = ctx -> {
            executed.incrementAndGet();
            return StageResult.success();
        };

        var runner = new PipelineRunner(defs(step), List.of(a, b), 0, 0, PipelineRunner.FailurePolicy.STOP);
        runner.run("p", new StageContext());

        assertEquals(0, executed.get(), "short-circuit should avoid stage execution");
        assertEquals(List.of("a.before", "b.before", "b.after", "a.after"), calls);
    }
}
