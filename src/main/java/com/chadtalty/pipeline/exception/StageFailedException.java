package com.chadtalty.pipeline.exception;

/**
 * Exception that indicates a stage-level failure which should be handled by the runner's
 * failure policy (e.g., STOP vs CONTINUE).
 * <p>
 * Use this instead of a generic {@link RuntimeException} when you want to be explicit about
 * a business failure distinct from infrastructure issues.
 */
public class StageFailedException extends RuntimeException {

    /**
     * Create a stage failure with a message describing the error and (optionally) remediation hints.
     *
     * @param message human-readable description of the failure
     */
    public StageFailedException(String message) {
        super(message);
    }

    /**
     * Create a stage failure with a root cause for diagnostics.
     *
     * @param message description of the failure
     * @param cause   underlying exception
     */
    public StageFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
