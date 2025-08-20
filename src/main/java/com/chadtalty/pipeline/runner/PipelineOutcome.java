package com.chadtalty.pipeline.runner;

/**
 * Final outcome of a pipeline run.
 *
 * <ul>
 *   <li>{@link #SUCCESS} – all stages completed (skips allowed).</li>
 *   <li>{@link #FAILURE} – at least one stage failed and the policy resulted in a failed run.</li>
 *   <li>{@link #PARTIAL} – reserved for advanced scenarios (e.g., batch runs with mixed results).</li>
 * </ul>
 */
public enum PipelineOutcome {
    SUCCESS,
    FAILURE,
    PARTIAL
}
