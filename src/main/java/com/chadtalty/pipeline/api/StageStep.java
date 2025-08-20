package com.chadtalty.pipeline.api;

/**
 * Functional contract for a single pipeline stage.
 * <p>
 * The runner constructs (or discovers) an ordered list of stages for a pipeline
 * and invokes {@link #execute(StageContext)} for each, passing a mutable {@link StageContext}.
 * <ul>
 *   <li>Return {@link StageResult#success()} (or {@code null}) to continue to the next stage.</li>
 *   <li>Return {@link StageResult#skip(String)} to mark the stage as skipped and continue.</li>
 *   <li>Return {@link StageResult#retry(String)} to request a retry (runner applies backoff/limits).</li>
 *   <li>Return {@link StageResult#stop(String)} to stop the entire pipeline immediately.</li>
 *   <li>Throw an exception to signal a failure; the runner’s failure policy (STOP/CONTINUE) applies.</li>
 * </ul>
 *
 * @param <C> concrete {@link StageContext} type used by the stage
 */
@FunctionalInterface
public interface StageStep<C extends StageContext> {

    /**
     * Execute the stage’s business logic.
     *
     * @param ctx mutable context that persists across stages in the same run
     * @return a {@link StageResult}; returning {@code null} is treated as {@link StageResult#success()}
     * @throws Exception to indicate failure (I/O, validation, remote errors, etc.)
     */
    StageResult execute(C ctx) throws Exception;
}
