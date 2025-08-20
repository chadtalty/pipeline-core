package com.chadtalty.pipeline.runner;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FailurePolicyRunnerTest {

    private static PipelineDefinitionSource defs(List<PipelineDefinitionSource.StageHolder> hs) {
        return n -> hs;
    }

    @Test
    void stopPolicy_returnsWithoutThrow_andDoesNotRunNextStage() {
        StageStep<StageContext> fail = ctx -> {
            throw new RuntimeException("boom");
        };
        AtomicInteger next = new AtomicInteger();
        StageStep<StageContext> after = ctx -> {
            next.incrementAndGet();
            return StageResult.success();
        };

        var h1 = new PipelineDefinitionSource.StageHolder("fail", 1, fail, c -> true);
        var h2 = new PipelineDefinitionSource.StageHolder("after", 2, after, c -> true);

        var runner = new FailurePolicy(defs(List.of(h1, h2)), List.of(), 0, 0, FailurePolicy.FailurePolicyType.STOP);

        assertDoesNotThrow(() -> runner.run("p", new StageContext()));
        assertEquals(0, next.get(), "STOP should not run later stages");
    }

    @Test
    void continuePolicy_runsNextStage() {
        StageStep<StageContext> fail = ctx -> {
            throw new RuntimeException("boom");
        };
        AtomicInteger next = new AtomicInteger();
        StageStep<StageContext> after = ctx -> {
            next.incrementAndGet();
            return StageResult.success();
        };

        var h1 = new PipelineDefinitionSource.StageHolder("fail", 1, fail, c -> true);
        var h2 = new PipelineDefinitionSource.StageHolder("after", 2, after, c -> true);

        var runner =
                new FailurePolicy(defs(List.of(h1, h2)), List.of(), 0, 0, FailurePolicy.FailurePolicyType.CONTINUE);

        assertDoesNotThrow(() -> runner.run("p", new StageContext()));
        assertEquals(1, next.get(), "CONTINUE should run later stages");
    }
}
