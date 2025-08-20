package com.chadtalty.pipeline.runner;

/**
 * Immutable descriptor passed to interceptors for a stage attempt.
 *
 * @param pipeline pipeline name
 * @param stageId  stage identifier
 * @param order    declared order inside the pipeline
 * @param attempt  1-based attempt number for this stage
 */
public record StageInvocation(String pipeline, String stageId, int order, int attempt) {}
