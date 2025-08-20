package com.chadtalty.pipeline.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a pipeline stage and associates it with one or more pipelines.
 * <p>
 * The registry discovers {@code @Stage}-annotated beans, groups them by pipeline name,
 * and orders them by {@link #order()} (or by YAML {@code stages:[]} if explicitly declared).
 * <p>
 * <b>Condition:</b> The {@link #condition()} string (e.g., SpEL in the Spring starter)
 * is evaluated against the current {@code StageContext}. If it evaluates to a falsy value,
 * the stage is skipped.
 *
 * <pre>{@code
 * @Component("bulkFetch")
 * @Stage(pipeline = {"bulk-data-ingest"}, order = 1, condition = "#root.get('enabled', T(Boolean))")
 * public class BulkFetchStage implements StageStep<StageContext> {
 *   public StageResult execute(StageContext ctx) { ... }
 * }
 * }</pre>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Stage {

    /**
     * Logical pipeline names this stage participates in.
     * Multiple pipelines are supported.
     */
    String[] pipeline();

    /**
     * Relative position within a pipeline. Lower values run earlier.
     * May be overridden by explicit ordering in configuration.
     */
    int order();

    /**
     * Optional stable id for this stage. If empty, the registry may use the bean name.
     */
    String id() default "";

    /**
     * Optional expression (e.g., SpEL in the Spring starter) evaluated against the {@code StageContext}.
     * If the expression is falsy, the stage is skipped.
     */
    String condition() default "";
}
