package com.chadtalty.pipeline.runner;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import com.chadtalty.pipeline.interceptors.StageInterceptor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class PipelineRunnerTest {

    private static PipelineDefinitionSource defs(Map<String, List<PipelineDefinitionSource.StageHolder>> map) {
        return map::get;
    }

    private static PipelineDefinitionSource.StageHolder holder(String id, int order, StageStep<StageContext> step) {
        Predicate<StageContext> cond = (c) -> true;
        return new PipelineDefinitionSource.StageHolder(id, order, step, cond);
    }

    @Test
    void runsStagesInOrder_andContinues() {
        List<String> order = new ArrayList<>();
        StageStep<StageContext> s1 = ctx -> {
            order.add("s1");
            return StageResult.success();
        };
        StageStep<StageContext> s2 = ctx -> {
            order.add("s2");
            return StageResult.success();
        };

        var map = Map.<String, List<PipelineDefinitionSource.StageHolder>>of(
                "p", List.of(holder("s1", 1, s1), holder("s2", 2, s2)));
        var runner = new PipelineRunner(defs(map), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);
        runner.run("p", new StageContext());

        assertEquals(List.of("s1", "s2"), order);
    }

    @Test
    void retryThenSuccess_onException() {
        AtomicInteger calls = new AtomicInteger();
        StageStep<StageContext> flaky = ctx -> {
            int n = calls.incrementAndGet();
            if (n <= 2) throw new RuntimeException("boom " + n);
            return StageResult.success();
        };
        var map = Map.of("p", List.of(holder("flaky", 1, flaky)));
        var runner = new PipelineRunner(
                defs(map), List.of(), /*maxAttempts*/ 2, /*backoff*/ 1, PipelineRunner.FailurePolicy.STOP);

        runner.run("p", new StageContext());
        assertEquals(3, calls.get(), "should attempt 3 times (2 retries then success)");
    }

    @Test
    void stopOnFailure_haltsPipeline() {
        AtomicInteger next = new AtomicInteger();
        StageStep<StageContext> fail = ctx -> {
            throw new RuntimeException("nope");
        };
        StageStep<StageContext> after = ctx -> {
            next.incrementAndGet();
            return StageResult.success();
        };
        var map = Map.of("p", List.of(holder("fail", 1, fail), holder("after", 2, after)));
        var runner = new PipelineRunner(defs(map), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);

        assertThrows(RuntimeException.class, () -> runner.run("p", new StageContext()));
        assertEquals(0, next.get(), "STOP policy should prevent later stages");
    }

    @Test
    void continueOnFailure_keepsGoing() {
        AtomicInteger next = new AtomicInteger();
        StageStep<StageContext> fail = ctx -> {
            throw new RuntimeException("nope");
        };
        StageStep<StageContext> after = ctx -> {
            next.incrementAndGet();
            return StageResult.success();
        };
        var map = Map.of("p", List.of(holder("fail", 1, fail), holder("after", 2, after)));
        var runner = new PipelineRunner(defs(map), List.of(), 0, 0, PipelineRunner.FailurePolicy.CONTINUE);

        // runner swallows after-policy exceptions and continues
        assertDoesNotThrow(() -> runner.run("p", new StageContext()));
        assertEquals(1, next.get(), "CONTINUE policy should run later stages");
    }

    @Test
    void interceptorShortCircuit_skipsStageExecution() {
        AtomicInteger executed = new AtomicInteger();
        StageStep<StageContext> step = ctx -> {
            executed.incrementAndGet();
            return StageResult.success();
        };

        StageInterceptor shortCircuit = new StageInterceptor() {
            @Override
            public java.util.Optional<StageResult> before(StageInvocation inv, StageContext ctx) {
                return Optional.of(StageResult.skip("skip by interceptor"));
            }
        };

        var map = Map.of("p", List.of(holder("s", 1, step)));
        var runner = new PipelineRunner(defs(map), List.of(shortCircuit), 0, 0, PipelineRunner.FailurePolicy.STOP);

        runner.run("p", new StageContext());
        assertEquals(0, executed.get(), "stage should not execute when short-circuited");
    }

    @Test
    void attemptCounter_isVisibleAndIncrementsOnRetry() {
        List<Integer> attemptsSeen = new ArrayList<>();
        StageStep<StageContext> flaky = ctx -> {
            attemptsSeen.add((Integer) ctx.get("__attempt", Integer.class));
            if (attemptsSeen.size() < 3) throw new RuntimeException("try again");
            return StageResult.success();
        };
        var map = Map.of("p", List.of(holder("flaky", 1, flaky)));
        var runner = new PipelineRunner(defs(map), List.of(), 2, 0, PipelineRunner.FailurePolicy.STOP);

        runner.run("p", new StageContext());
        assertEquals(List.of(1, 2, 3), attemptsSeen);
    }
}
