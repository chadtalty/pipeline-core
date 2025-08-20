package com.chadtalty.pipeline.batch;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Supplies a finite stream of {@link BatchItem}s at runtime.
 * <p>
 * Implementations may pull from databases, list files, or page remote APIs.
 * The returned stream should be consumed once and then closed by the caller.
 *
 * @implNote Keep per-call resource usage modest; avoid loading all items in memory if a lazy stream is possible.
 */
public interface BatchSupplier {

    /**
     * Produce a stream of batch items for a run.
     *
     * @param runParams parameters provided by the caller (e.g., date ranges, filters)
     * @return a finite, one-shot {@link Stream} of items
     * @throws Exception to indicate supplier failures (I/O, auth, etc.)
     */
    Stream<BatchItem> stream(Map<String, Object> runParams) throws Exception;
}
