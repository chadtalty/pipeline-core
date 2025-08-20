package com.chadtalty.pipeline.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.api.StageResult;
import com.chadtalty.pipeline.api.StageStep;
import com.chadtalty.pipeline.runner.PipelineDefinitionSource;
import com.chadtalty.pipeline.runner.PipelineRunner;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BatchRunnerTest {

    @Test
    void runsItemsInBatch_twice() {
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
    void stopPolicy_bubblesUpOnFirstFailure() {
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
    void seedRun_putsParamsInContext() {
        StageStep<StageContext> step = ctx -> {
            assertEquals("https://x", ctx.get("sourceUrl", String.class));
            assertEquals("foo", ctx.get("token", String.class));
            return StageResult.success();
        };
        var runner = new PipelineRunner(defs(step), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);

        new BatchRunner(runner).runSeed("p", new StageContext(), Map.of("sourceUrl", "https://x", "token", "foo"));
    }

    private static PipelineDefinitionSource defs(StageStep<StageContext> step) {
        var holder = new PipelineDefinitionSource.StageHolder("s", 1, step, (c) -> true);
        return (name) -> List.of(holder);
    }
}
