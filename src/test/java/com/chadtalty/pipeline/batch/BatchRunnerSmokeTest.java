package com.chadtalty.pipeline.batch;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import com.chadtalty.pipeline.runner.PipelineDefinitionSource;
import com.chadtalty.pipeline.runner.PipelineRunner;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BatchRunnerSmokeTest {

    private static PipelineDefinitionSource defs(StageStep<StageContext> step) {
        var holder = new PipelineDefinitionSource.StageHolder("s", 1, step, (c) -> true);
        return (name) -> List.of(holder);
    }

    @Test
    void seed_putsParamsAndRunsOnce() {
        StageStep<StageContext> step = ctx -> {
            assertEquals("https://x", ctx.get("sourceUrl", String.class));
            assertEquals("foo", ctx.get("token", String.class));
            return StageResult.success();
        };
        var runner = new PipelineRunner(defs(step), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);
        new BatchRunner(runner).runSeed("p", new StageContext(), Map.of("sourceUrl", "https://x", "token", "foo"));
    }

    @Test
    void runBatch_twoItems_runsTwice() {
        AtomicInteger count = new AtomicInteger();
        StageStep<StageContext> step = ctx -> {
            count.incrementAndGet();
            return StageResult.success();
        };

        var runner = new PipelineRunner(defs(step), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);
        var items = Stream.of(BatchItem.of("A", "param", "va"), BatchItem.of("B", "param", "vb"));

        new BatchRunner(runner).runBatch("p", StageContext::new, items, BatchOptions.defaults());
        assertEquals(2, count.get());
    }

    @Test
    void runBatch_stopPolicy_bubblesFirstFailure() {
        StageStep<StageContext> fail = ctx -> {
            throw new RuntimeException("boom");
        };
        var runner = new PipelineRunner(defs(fail), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);

        var items = Stream.of(BatchItem.of("A"), BatchItem.of("B"));
        var opts = new BatchOptions(2, BatchOptions.FailurePolicy.STOP, java.time.Duration.ofMinutes(1));

        assertThrows(
                RuntimeException.class, () -> new BatchRunner(runner).runBatch("p", StageContext::new, items, opts));
    }

    @Test
    void runBatch_continuePolicy_completesAll() {
        AtomicInteger attempts = new AtomicInteger();
        StageStep<StageContext> sometimes = ctx -> {
            if (attempts.getAndIncrement() == 0) throw new RuntimeException("first fails");
            return StageResult.success();
        };

        var runner = new PipelineRunner(defs(sometimes), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);
        var items = Stream.of(BatchItem.of("A"), BatchItem.of("B"));
        var opts = new BatchOptions(2, BatchOptions.FailurePolicy.CONTINUE, java.time.Duration.ofMinutes(1));

        assertDoesNotThrow(() -> new BatchRunner(runner).runBatch("p", StageContext::new, items, opts));
        assertEquals(2, attempts.get()); // both items attempted
    }
}
