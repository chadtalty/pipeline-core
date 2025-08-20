package com.chadtalty.pipeline.api;

/**
 * Control flow directive returned by a stage.
 * <ul>
 *   <li>{@link #CONTINUE} – stage completed; proceed to the next stage.</li>
 *   <li>{@link #SKIP} – stage was intentionally skipped (condition, cache hit, feature flag).</li>
 *   <li>{@link #RETRY} – stage asked the runner to retry (respecting backoff/limits).</li>
 *   <li>{@link #STOP} – stop the pipeline immediately (treated as success/failure based on context).</li>
 * </ul>
 *
 * @since 1.0
 */
public enum StageStatus {
    /** Stage completed successfully; advance to the next stage. */
    CONTINUE,

    /** Stage was skipped by design; advance to the next stage. */
    SKIP,

    /** Ask the runner to retry this stage (up to configured attempt limits). */
    RETRY,

    /** Stop the pipeline run now (no further stages will execute). */
    STOP
}
