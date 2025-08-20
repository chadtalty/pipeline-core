package com.chadtalty.pipeline.batch;

import java.util.Map;

/**
 * Immutable descriptor for a single batch input.
 * <p>
 * A batch run fans out the pipeline over a stream of {@code BatchItem}s. The {@code key} should be a
 * stable identifier (e.g., URL, file name, id) that can be used for idempotency and logging, while
 * {@code params} are copied into the {@code StageContext} before the first stage executes.
 *
 * @param key    stable identifier for this item (avoid high-cardinality values in metrics)
 * @param params per-item parameters to seed into the {@code StageContext}
 */
public record BatchItem(String key, Map<String, Object> params) {

    /**
     * Create an item with only a key and no additional parameters.
     *
     * @param key stable identifier for the item
     * @return a {@code BatchItem}
     */
    public static BatchItem of(String key) {
        return new BatchItem(key, Map.of());
    }

    /**
     * Create an item with a single parameter.
     *
     * @param key   stable identifier for the item
     * @param param parameter name to add to context
     * @param value parameter value
     * @return a {@code BatchItem}
     */
    public static BatchItem of(String key, String param, Object value) {
        return new BatchItem(key, Map.of(param, value));
    }
}
