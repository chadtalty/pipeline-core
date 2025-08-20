package com.chadtalty.pipeline.runner;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.api.StageResult;
import com.chadtalty.pipeline.api.StageStatus;
import com.chadtalty.pipeline.api.StageStep;
import com.chadtalty.pipeline.base.AbstractStage;
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
 * A simple pipeline runner variant that applies a default {@link FailurePolicyType} when a stage fails.
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>Discovers ordered stages via {@link PipelineDefinitionSource}.</li>
 *   <li>Executes each stage with retry/backoff.</li>
 *   <li>Notifies run-level listeners on start/complete.</li>
 *   <li>Invokes {@link StageInterceptor} hooks around each stage attempt.</li>
 * </ul>
 * <p>
 * <b>Important:</b> In this class, when {@link FailurePolicyType#STOP} is active and retries are exhausted,
 * the runner <em>returns</em> (does not throw).
 *
 * @implNote Name is slightly misleading; this is effectively a runner with an embedded default failure policy.
 */
public class FailurePolicy {

    private static final Logger log = LoggerFactory.getLogger(AbstractStage.class);

    private final PipelineDefinitionSource definitions;
    private final List<StageInterceptor> interceptors;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;
    private final FailurePolicyType onFailureDefault;

    /** Default action after a stage fails and retries are exhausted. */
    public enum FailurePolicyType {
        STOP,
        CONTINUE
    }

    /**
     * @param definitions       source of ordered stages per pipeline
     * @param interceptors      stage interceptors (before/after/error)
     * @param retryMaxAttempts  max extra attempts after the first try (e.g., 2 means up to 3 total tries)
     * @param retryBackoffMs    sleep between attempts
     * @param onFailureDefault  what to do when a stage fails after retries
     */
    public FailurePolicy(
            PipelineDefinitionSource definitions,
            List<StageInterceptor> interceptors,
            int retryMaxAttempts,
            long retryBackoffMs,
            FailurePolicyType onFailureDefault) {
        this.definitions = definitions;
        this.interceptors = new ArrayList<>(interceptors == null ? List.of() : interceptors);
        this.interceptors.sort(Comparator.comparingInt(StageInterceptor::getOrder));
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.onFailureDefault = onFailureDefault == null ? FailurePolicyType.STOP : onFailureDefault;
    }

    /**
     * Run a pipeline with the provided context.
     *
     * @param pipeline logical pipeline name
     * @param ctx      mutable context passed to stages
     * @param <C>      context type
     */
    public <C extends StageContext> void run(String pipeline, C ctx) {
        Objects.requireNonNull(pipeline, "pipeline");
        Objects.requireNonNull(ctx, "ctx");

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

                    // BEFORE (interceptors may short-circuit)
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

                        // AFTER
                        long dur = System.nanoTime() - startNs;
                        for (int i = interceptors.size() - 1; i >= 0; i--) {
                            interceptors.get(i).after(inv, ctx, result, dur);
                        }

                        // Handle control statuses
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
                        // Retry if allowed
                        if (attempt <= retryMaxAttempts) {
                            sleep(retryBackoffMs);
                            continue;
                        }

                        // In this runner, STOP means: mark failure and return (no throw)
                        if (onFailureDefault == FailurePolicyType.STOP) {
                            outcome = PipelineOutcome.FAILURE;
                            return;
                        } else {
                            // CONTINUE to next stage
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

    /** Run-level listeners (start/complete). */
    private final List<PipelineRunListener> runListeners = new ArrayList<>();

    /** Register a run listener (no-op if null). */
    public void addRunListener(PipelineRunListener l) {
        if (l != null) runListeners.add(l);
    }

    private List<PipelineRunListener> listeners() {
        return runListeners;
    }

    /** Sleep helper that preserves interrupt semantics. */
    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
