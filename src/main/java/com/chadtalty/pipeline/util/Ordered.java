package com.chadtalty.pipeline.util;

/**
 * Minimal ordering contract to avoid a Spring dependency in pipeline-core.
 * Lower values have higher precedence (run earlier).
 */
public interface Ordered {
    default int getOrder() {
        return 0;
    }
}
