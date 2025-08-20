package com.chadtalty.pipeline.base;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.api.StageResult;
import com.chadtalty.pipeline.api.StageStatus;
import com.chadtalty.pipeline.api.StageStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for pipeline stages that provides consistent debug logging and timing.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Logs a <em>START</em> line with pipeline, stage id, attempt, and a small sample of context keys
 *       (excluding reserved keys beginning with {@code "__"}).</li>
 *   <li>Calls {@link #doExecute(StageContext)} to perform the stage's business logic.</li>
 *   <li>Logs an <em>END</em> line with the resulting {@link StageStatus} and elapsed time in ms.</li>
 *   <li>Propagates any thrown exception (the runner will apply retry/failure policy).</li>
 * </ul>
 * <p>
 * Reserved context keys used by the runner and this base:
 * <ul>
 *   <li>{@code __pipeline} – logical pipeline name</li>
 *   <li>{@code __stageId}  – stage identifier</li>
 *   <li>{@code __attempt}  – 1-based attempt counter for this stage</li>
 * </ul>
 *
 * @param <C> concrete {@link StageContext} type used by this stage
 *
 */
public abstract class AbstractStage<C extends StageContext> implements StageStep<C> {

    private static final Logger log = LoggerFactory.getLogger(AbstractStage.class);

    /**
     * Template method that wraps {@link #doExecute(StageContext)} with debug logging,
     * duration measurement, and consistent error handling.
     * <p>
     * Implementations should override {@link #doExecute(StageContext)} only.
     *
     * @param ctx mutable context shared across stages
     * @return the {@link StageResult}; {@code null} is treated as {@link StageResult#success()}
     * @throws Exception any exception thrown by {@link #doExecute(StageContext)} is propagated
     */
    @Override
    public final StageResult execute(C ctx) throws Exception {
        // Read runner-provided metadata from reserved keys.
        String pipeline = String.valueOf(ctx.data().getOrDefault("__pipeline", "(unknown)"));
        String stageId = String.valueOf(ctx.data().getOrDefault("__stageId", "(unknown)"));
        int attempt = (int) ctx.data().getOrDefault("__attempt", 1);

        long start = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug(
                    "START pipeline={} stage={} attempt={} keys={}",
                    pipeline,
                    stageId,
                    attempt,
                    // Show up to 8 user keys to keep logs compact; hide reserved keys.
                    ctx.data().keySet().stream()
                            .filter(k -> !k.startsWith("__"))
                            .limit(8)
                            .toList());
        }

        StageResult result = StageResult.success();
        try {
            result = doExecute(ctx);
            return (result == null) ? StageResult.success() : result;
        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "ERROR pipeline={} stage={} attempt={} err={}", pipeline, stageId, attempt, ex.toString(), ex);
            }
            throw ex; // let the runner decide on retries/failure policy
        } finally {
            long durMs = (System.nanoTime() - start) / 1_000_000;
            if (log.isDebugEnabled()) {
                log.debug(
                        "END   pipeline={} stage={} status={} durationMs={}",
                        pipeline,
                        stageId,
                        (result == null ? StageStatus.CONTINUE : result.status()),
                        durMs);
            }
        }
    }

    /**
     * Implement the stage's business logic here.
     * <p>
     * Guidelines:
     * <ul>
     *   <li>Return {@link StageResult#success()} (or {@code null}) to advance to the next stage.</li>
     *   <li>Return {@link StageResult#retry(String)} to signal a retry (runner respects backoff/limits).</li>
     *   <li>Return {@link StageResult#skip(String)} to skip remaining work in this stage.</li>
     *   <li>Return {@link StageResult#stop(String)} to stop the pipeline immediately.</li>
     *   <li>Throw an exception for hard failures; the runner applies the configured failure policy.</li>
     * </ul>
     *
     * @param ctx mutable context for reading inputs and writing outputs
     * @return a {@link StageResult}; returning {@code null} is treated as success
     * @throws Exception to indicate failure (I/O, validation, etc.)
     */
    protected abstract StageResult doExecute(C ctx) throws Exception;
}
