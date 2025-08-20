package com.chadtalty.pipeline.runner;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineRunnerBranchesTest {

    private static PipelineDefinitionSource defs(List<PipelineDefinitionSource.StageHolder> hs) {
        return n -> hs;
    }

    @Test
    void stopResultHaltsPipelineWithoutThrow() {
        StageStep<StageContext> s1 = ctx -> StageResult.stop("done");
        StageStep<StageContext> s2 = ctx -> {
            fail("should not run");
            return StageResult.success();
        };

        var h1 = new PipelineDefinitionSource.StageHolder("s1", 1, s1, c -> true);
        var h2 = new PipelineDefinitionSource.StageHolder("s2", 2, s2, c -> true);

        var runner = new PipelineRunner(defs(List.of(h1, h2)), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);
        assertDoesNotThrow(() -> runner.run("p", new StageContext()));
    }

    @Test
    void conditionFalse_skipsStage() {
        StageStep<StageContext> s1 = ctx -> {
            fail("should be skipped");
            return StageResult.success();
        };
        var h1 = new PipelineDefinitionSource.StageHolder("s1", 1, s1, c -> false);

        var runner = new PipelineRunner(defs(List.of(h1)), List.of(), 0, 0, PipelineRunner.FailurePolicy.STOP);
        runner.run("p", new StageContext()); // no exception, nothing executed
    }
}
