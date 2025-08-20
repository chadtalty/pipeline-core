package com.chadtalty.pipeline.batch;

import java.time.Duration;

/**
 * Options controlling a batch execution.
 *
 * @param maxParallel     maximum items to process concurrently (thread pool size &gt;= 1)
 * @param onItemFailure   behavior when an item fails after stage-level retries: {@link FailurePolicy#STOP} or {@link FailurePolicy#CONTINUE}
 * @param perItemTimeout  optional timeout budget per item (not enforced by {@link BatchRunner} in this version;
 *                        include timeouts in your stages or wrap execution externally)
 */
public record BatchOptions(int maxParallel, FailurePolicy onItemFailure, Duration perItemTimeout) {

    /** Policy applied when a single item fails. */
    public enum FailurePolicy {
        STOP,
        CONTINUE
    }

    /**
     * Sensible defaults: {@code maxParallel=4}, {@code onItemFailure=CONTINUE}, {@code perItemTimeout=10m}.
     *
     * @return default options
     */
    public static BatchOptions defaults() {
        return new BatchOptions(4, FailurePolicy.CONTINUE, Duration.ofMinutes(10));
    }
}
