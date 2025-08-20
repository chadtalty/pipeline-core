package com.chadtalty.pipeline.batch;

import com.chadtalty.pipeline.api.StageContext;
import com.chadtalty.pipeline.runner.PipelineRunner;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Helper that executes a pipeline for one or many inputs (batch).
 * <p>
 * Each batch item runs with a <em>fresh</em> {@link StageContext}. The item's {@code params} are copied
 * into the context before the first stage executes. A synthetic {@code "__batch"} flag and {@code "seed"}
 * (the item's key) are also added for observability.
 *
 * <h4>Failure semantics</h4>
 * - If {@link BatchOptions.FailurePolicy#STOP}, the first failing item will propagate its exception and
 *   cancel remaining futures (via executor shutdown).<br>
 * - If {@link BatchOptions.FailurePolicy#CONTINUE}, failures are recorded and the batch proceeds.
 *
 */
public final class BatchRunner {
    private final PipelineRunner runner;

    /**
     * @param runner pipeline runner used to execute each item
     */
    public BatchRunner(PipelineRunner runner) {
        this.runner = runner;
    }

    /**
     * Execute a single seeded run (non-batch). {@code seedParams} are copied into {@code baseCtx}.
     *
     * @param pipeline  pipeline name
     * @param baseCtx   context to seed and pass to the run
     * @param seedParams key/value pairs to put into the context before running
     * @param <C>       context type
     * @throws RuntimeException if the underlying run fails and the runner policy is STOP
     */
    public <C extends StageContext> void runSeed(String pipeline, C baseCtx, java.util.Map<String, Object> seedParams) {
        seedParams.forEach(baseCtx::put);
        runner.run(pipeline, baseCtx);
    }

    /**
     * Execute a batch run over a stream of items.
     *
     * @param pipeline   pipeline name
     * @param ctxFactory factory providing a <em>fresh</em> context per item
     * @param items      a finite {@link Stream} of {@link BatchItem}; do not reuse a consumed stream
     * @param options    batch execution options (parallelism, failure policy, timeouts)
     * @param <C>        context type
     * @throws RuntimeException if an item fails and {@code onItemFailure == STOP}
     */
    public <C extends StageContext> void runBatch(
            String pipeline, Supplier<C> ctxFactory, Stream<BatchItem> items, BatchOptions options) {

        ExecutorService exec = Executors.newFixedThreadPool(Math.max(1, options.maxParallel()));
        try {
            // Submit each item as a task; each gets a brand-new context.
            List<Future<Boolean>> futures = items.map(item -> exec.submit(() -> {
                        C ctx = ctxFactory.get();
                        ctx.put("__batch", true); // marker for metrics/logging
                        ctx.put("seed", item.key()); // stable key for idempotency/diagnostics
                        item.params().forEach(ctx::put);

                        try {
                            runner.run(pipeline, ctx);
                            return true;
                        } catch (Exception ex) {
                            if (options.onItemFailure() == BatchOptions.FailurePolicy.STOP) throw ex;
                            return false; // CONTINUE policy: swallow and mark failed
                        }
                    }))
                    .toList();

            // Wait for completion (propagates the first thrown exception if STOP)
            for (Future<Boolean> f : futures) {
                f.get();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        } catch (ExecutionException ee) {
            // Unwrap the item failure to present the original cause.
            throw new RuntimeException(ee.getCause());
        } finally {
            exec.shutdownNow();
        }
    }
}
