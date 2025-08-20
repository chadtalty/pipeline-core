package com.chadtalty.pipeline.api;

/**
 * Result of a stage execution.
 * <p>
 * A result consists of a {@link StageStatus} and an optional human-readable message for logs/metrics.
 * Helper factories are provided for common cases.
 *
 * @param status  control directive for the runner
 * @param message optional detail for operators (may be {@code null})
 * @since 1.0
 */
public record StageResult(StageStatus status, String message) {

    /**
     * Convenience for {@code CONTINUE} with no message.
     */
    public static StageResult success() {
        return new StageResult(StageStatus.CONTINUE, null);
    }

    /**
     * Convenience for {@code SKIP} with a reason.
     *
     * @param msg reason the stage was skipped
     */
    public static StageResult skip(String msg) {
        return new StageResult(StageStatus.SKIP, msg);
    }

    /**
     * Convenience for {@code RETRY} with a reason (used in logs/metrics).
     *
     * @param msg reason to retry
     */
    public static StageResult retry(String msg) {
        return new StageResult(StageStatus.RETRY, msg);
    }

    /**
     * Convenience for {@code STOP} with a reason.
     *
     * @param msg reason to stop the pipeline
     */
    public static StageResult stop(String msg) {
        return new StageResult(StageStatus.STOP, msg);
    }
}
