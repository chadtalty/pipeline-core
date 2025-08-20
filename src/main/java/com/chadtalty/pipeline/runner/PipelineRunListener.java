package com.chadtalty.pipeline.runner;

import com.chadtalty.pipeline.api.StageContext;

/**
 * Listener for run-level lifecycle events.
 * <p>
 * Use this to integrate chaining, notifications, or run-scoped tracing/metrics.
 */
public interface PipelineRunListener {

    /**
     * Called right before the first stage is evaluated.
     *
     * @param pipeline pipeline name
     * @param runId    unique identifier for this run
     * @param ctx      initial context (mutable)
     */
    default void onStart(String pipeline, String runId, StageContext ctx) {}

    /**
     * Called after the run completes (success or failure).
     *
     * @param report immutable snapshot of the run
     */
    default void onComplete(RunReport report) {}
}
