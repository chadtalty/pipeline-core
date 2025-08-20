package com.chadtalty.pipeline.runner;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.api.StageResult;
import com.chadtalty.pipeline.api.StageStatus;
import com.chadtalty.pipeline.api.StageStep;
import com.chadtalty.pipeline.interceptors.StageInterceptor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default pipeline runner that executes ordered stages from a {@link PipelineDefinitionSource}.
 * <p>
 * Features:
 * <ul>
 *   <li>Per-stage retries with backoff.</li>
 *   <li>Interceptors around stage attempts (before/after/error).</li>
 *   <li>Run-level listeners (start/complete) for chaining and reporting.</li>
 *   <li>Control statuses (CONTINUE/SKIP/RETRY/STOP) via {@link StageResult}.</li>
 *   <li>On failure policy: {@link FailurePolicy#STOP} rethrows; {@link FailurePolicy#CONTINUE} advances to next stage.</li>
 * </ul>
 */
public class PipelineRunner {
    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final PipelineDefinitionSource definitions;
    private final List<StageInterceptor> interceptors;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;
    private final FailurePolicy onFailureDefault;

    /** Default action after retries are exhausted. */
    public enum FailurePolicy {
        STOP,
        CONTINUE
    }

    /**
     * @param definitions      source of ordered stages for a pipeline
     * @param interceptors     stage interceptors (executed in order; after/onError in reverse)
     * @param retryMaxAttempts number of additional attempts after the first try
     * @param retryBackoffMs   delay between attempts in milliseconds
     * @param onFailureDefault default policy when a stage fails after retries
     */
    public PipelineRunner(
            PipelineDefinitionSource definitions,
            List<StageInterceptor> interceptors,
            int retryMaxAttempts,
            long retryBackoffMs,
            FailurePolicy onFailureDefault) {
        this.definitions = definitions;
        this.interceptors = new ArrayList<>(interceptors == null ? List.of() : interceptors);
        this.interceptors.sort(Comparator.comparingInt(StageInterceptor::getOrder));
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.onFailureDefault = onFailureDefault == null ? FailurePolicy.STOP : onFailureDefault;
    }

    /**
     * Execute the named pipeline with the supplied context.
     *
     * @param pipeline pipeline name (must resolve to one or more stages)
     * @param ctx      mutable context shared across stages
     * @param <C>      context type
     * @throws RuntimeException when a stage fails and policy is {@link FailurePolicy#STOP}
     */
    public <C extends StageContext> void run(String pipeline, C ctx) {
        Objects.requireNonNull(pipeline, "pipeline must not be null");
        Objects.requireNonNull(ctx, "ctx must not be null");

        String runId = UUID.randomUUID().toString();
        Instant started = Instant.now();
        PipelineOutcome outcome = PipelineOutcome.SUCCESS;

        try {
            listeners().forEach(listener -> listener.onStart(pipeline, runId, ctx));

            var stages = definitions.stagesFor(pipeline);
            if (stages == null || stages.isEmpty()) {
                log.warn("Pipeline {} has no stages", pipeline);
                return;
            }

            for (var holder : stages) {
                // reserved metadata for base/logging/metrics
                ctx.put("__pipeline", pipeline);
                ctx.put("__stageId", holder.id());

                // pre-execution condition check
                if (!holder.condition().test(ctx)) {
                    if (log.isDebugEnabled()) log.debug("Skipping stage {} due to condition=false", holder.id());
                    continue;
                }

                int attempt = 0;
                while (true) {
                    attempt++;
                    ctx.put("__attempt", attempt);
                    StageInvocation inv = new StageInvocation(pipeline, holder.id(), holder.order(), attempt);
                    long startNs = System.nanoTime();

                    // BEFORE (interceptors may short-circuit with a StageResult)
                    StageResult shortCircuit = null;
                    for (StageInterceptor it : interceptors) {
                        var maybe = it.before(inv, ctx);
                        if (maybe.isPresent()) {
                            shortCircuit = maybe.get();
                            break;
                        }
                    }

                    try {
                        @SuppressWarnings("unchecked")
                        StageResult result =
                                (shortCircuit != null) ? shortCircuit : ((StageStep<C>) holder.step()).execute(ctx);

                        // AFTER (reverse order)
                        long dur = System.nanoTime() - startNs;
                        for (int i = interceptors.size() - 1; i >= 0; i--) {
                            interceptors.get(i).after(inv, ctx, result, dur);
                        }

                        // Handle control flow
                        var status =
                                (result == null || result.status() == null) ? StageStatus.CONTINUE : result.status();
                        if (status == StageStatus.CONTINUE) break;
                        if (status == StageStatus.SKIP) break;
                        if (status == StageStatus.STOP) return;
                        if (status == StageStatus.RETRY) {
                            if (attempt <= retryMaxAttempts) {
                                sleep(retryBackoffMs);
                                continue;
                            }
                            throw new RuntimeException("Retries exceeded for " + holder.id());
                        }
                        break; // unknown -> continue
                    } catch (Exception ex) {
                        long dur = System.nanoTime() - startNs;
                        for (int i = interceptors.size() - 1; i >= 0; i--) {
                            interceptors.get(i).onError(inv, ctx, ex, dur);
                        }

                        // If there are retries left, try again
                        if (attempt <= retryMaxAttempts) {
                            sleep(retryBackoffMs);
                            continue;
                        }

                        // Policy after retries exhausted
                        if (onFailureDefault == FailurePolicy.STOP) {
                            outcome = PipelineOutcome.FAILURE;
                            if (ex instanceof RuntimeException re) throw re;
                            throw new RuntimeException(ex);
                        } else {
                            // CONTINUE: move on to next stage
                            break;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            outcome = PipelineOutcome.FAILURE;
            throw e;
        } finally {
            var report = new RunReport(pipeline, runId, outcome, started, Instant.now(), ctx);
            listeners().forEach(l -> l.onComplete(report));
        }
    }

    // --- Run listeners ---

    /** Attached run-level listeners (e.g., for chaining/alerts). */
    private final List<PipelineRunListener> runListeners = new ArrayList<>();

    /** Add a run listener (ignored if null). */
    public void addRunListener(PipelineRunListener l) {
        if (l != null) runListeners.add(l);
    }

    private List<PipelineRunListener> listeners() {
        return runListeners;
    }

    // --- Utils ---

    /** Sleep helper that preserves interrupt semantics. */
    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
