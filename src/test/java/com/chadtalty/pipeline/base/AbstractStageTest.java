package com.chadtalty.pipeline.base;

import static org.junit.jupiter.api.Assertions.*;

import com.chadtalty.pipeline.api.*;
import org.junit.jupiter.api.Test;

class AbstractStageTest {

    /** Simple concrete stage for testing. */
    static class OkStage extends AbstractStage<StageContext> {
        @Override
        protected StageResult doExecute(StageContext ctx) {
            // return null to ensure wrapper maps it to CONTINUE
            ctx.put("touched", true);
            return null;
        }
    }

    static class FailingStage extends AbstractStage<StageContext> {
        @Override
        protected StageResult doExecute(StageContext ctx) {
            throw new RuntimeException("boom");
        }
    }

    @Test
    void execute_wrapsDoExecute_andTreatsNullAsSuccess() throws Exception {
        var ctx = new StageContext();
        ctx.put("__pipeline", "p");
        ctx.put("__stageId", "ok");
        ctx.put("__attempt", 1);

        var stage = new OkStage();
        StageResult r = stage.execute(ctx);

        assertEquals(StageStatus.CONTINUE, r.status());
        assertEquals(Boolean.TRUE, ctx.get("touched", Boolean.class));
    }

    @Test
    void execute_propagatesExceptions_andStillLogsEnd() {
        var ctx = new StageContext();
        ctx.put("__pipeline", "p");
        ctx.put("__stageId", "fail");
        ctx.put("__attempt", 1);

        var stage = new FailingStage();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> stage.execute(ctx));
        assertEquals("boom", ex.getMessage());
    }
}
