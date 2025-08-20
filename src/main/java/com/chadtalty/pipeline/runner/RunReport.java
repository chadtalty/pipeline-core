package com.chadtalty.pipeline.runner;

import com.chadtalty.pipeline.api.StageContext;
import java.time.Instant;

/**
 * Immutable snapshot describing the outcome of a pipeline run.
 *
 * @param pipeline     pipeline name
 * @param runId        unique id for the run
 * @param outcome      final outcome ({@link PipelineOutcome})
 * @param startedAt    run start time
 * @param finishedAt   run end time
 * @param finalContext the context as it existed at run completion
 */
public record RunReport(
        String pipeline,
        String runId,
        PipelineOutcome outcome,
        Instant startedAt,
        Instant finishedAt,
        StageContext finalContext) {}
