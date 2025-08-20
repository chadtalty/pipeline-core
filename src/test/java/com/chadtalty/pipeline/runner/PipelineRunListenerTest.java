package com.chadtalty.pipeline.runner;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PipelineRunListenerTest {

    private static PipelineDefinitionSource defs(StageStep<StageContext> step) {
        var h = new PipelineDefinitionSource.StageHolder("s", 1, step, c -> true);
        return n -> List.of(h);
    }

    @Test
    void startAndCompleteAreFired() {
        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);

        var runner = new PipelineRunner(
                defs(ctx -> StageResult.success()), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);

        runner.addRunListener(new PipelineRunListener() {
            @Override
            public void onStart(String p, String runId, StageContext ctx) {
                started.set(true);
            }

            @Override
            public void onComplete(RunReport report) {
                completed.set(true);
            }
        });

        runner.run("p", new StageContext());
        assertTrue(started.get());
        assertTrue(completed.get());
    }
}
