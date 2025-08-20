package com.chadtalty.pipeline.interceptors;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.api.StageResult;
import com.chadtalty.pipeline.runner.StageInvocation;
import com.chadtalty.pipeline.util.Ordered;
import java.util.Optional;

/**
 * Cross-cutting hooks around each stage execution.
 * <p>
 * Interceptors are invoked in ascending {@link #getOrder()} for {@link #before(StageInvocation, StageContext)},
 * and in <em>reverse</em> order for {@link #after(StageInvocation, StageContext, StageResult, long)}
 * and {@link #onError(StageInvocation, StageContext, Exception, long)}.
 * <p>
 * <strong>Duration semantics:</strong> The {@code durationNanos} provided to {@code after/onError} measures the
 * wall-clock time from immediately before the stage (or a short-circuit) begins until it completes or throws.
 * It therefore <em>includes</em> time spent in {@code before(...)}.
 * <p>
 * <strong>Short-circuiting:</strong> Returning a non-empty {@link StageResult} from {@link #before(...)} will skip
 * the actual stage execution and use that result instead (e.g., to {@code SKIP} or {@code RETRY} a stage).
 *
 * @implNote Keep interceptor work lightweight and non-blocking. Heavy work will inflate stage duration.
 */
public interface StageInterceptor extends Ordered {

    /**
     * Called before a stage attempt executes. May return a {@link StageResult} to short-circuit execution.
     *
     * @param inv descriptor of the stage attempt (pipeline, stage id, order, attempt)
     * @param ctx mutable context shared across stages
     * @return {@code Optional.of(result)} to short-circuit the stage, or {@code Optional.empty()} to proceed
     */
    default Optional<StageResult> before(StageInvocation inv, StageContext ctx) {
        return Optional.empty();
    }

    /**
     * Called after a stage attempt completes normally (including short-circuits).
     *
     * @param inv           descriptor of the stage attempt
     * @param ctx           context at completion
     * @param result        the stage result (may be {@code null} which is treated as {@code CONTINUE})
     * @param durationNanos duration in nanoseconds measured around the attempt
     */
    default void after(StageInvocation inv, StageContext ctx, StageResult result, long durationNanos) {}

    /**
     * Called when a stage attempt throws an exception (after retries may still occur).
     *
     * @param inv           descriptor of the stage attempt
     * @param ctx           context at time of error
     * @param ex            the thrown exception
     * @param durationNanos duration in nanoseconds measured around the attempt
     */
    default void onError(StageInvocation inv, StageContext ctx, Exception ex, long durationNanos) {}

    /**
     * Defines ordering among interceptors. Lower values run earlier in {@code before(...)} and later in
     * {@code after(...)} / {@code onError(...)}.
     *
     * @return the order (default 0)
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
