package com.chadtalty.pipeline.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable, thread-safe context shared across all stages in a single pipeline run.
 * <p>
 * The context behaves like a namespaced key/value bag for inputs and intermediate outputs.
 * Runners and base classes may place <em>reserved</em> metadata keys (prefixed with {@code "__"}), e.g.:
 * <ul>
 *   <li>{@code __pipeline} – pipeline name</li>
 *   <li>{@code __stageId}  – current stage id</li>
 *   <li>{@code __attempt}  – current attempt number (1-based)</li>
 *   <li>{@code __runId}    – unique id for this pipeline run</li>
 * </ul>
 * Application code should avoid writing keys beginning with {@code "__"}.
 *
 */
public class StageContext {

    /** Backing store for context data (thread-safe for simple usage patterns). */
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    /**
     * Access the raw map (advanced scenarios only). Prefer {@link #put(String, Object)} and {@link #get(String, Class)}.
     */
    public Map<String, Object> data() {
        return data;
    }

    /**
     * Put a value into the context, replacing any existing value.
     *
     * @param key   non-null key
     * @param value value to store (may be {@code null})
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Get a value and cast it to the requested type.
     *
     * @param key  key to read
     * @param type expected class
     * @param <T>  generic type parameter
     * @return the value cast to {@code T}, or {@code null} if absent
     * @throws ClassCastException if the stored value is not an instance of {@code type}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }

    /**
     * Get a value without casting (raw {@link Object}).
     *
     * @param key key to read
     * @return the stored value or {@code null}
     */
    public Object get(String key) {
        return data.get(key);
    }
}
