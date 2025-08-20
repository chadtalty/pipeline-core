package com.chadtalty.pipeline.runner;

import com.chadtalty.pipeline.api.StageContext;
import java.util.List;
import java.util.function.Predicate;

/**
 * Source of pipeline stage definitions.
 * <p>
 * Implementations return the <b>ordered</b> list of stage holders for a given pipeline name.
 * This allows different registries (e.g., Spring bean scanning + YAML) to provide the stage list.
 */
public interface PipelineDefinitionSource {

    /**
     * @param pipelineName logical pipeline identifier
     * @return ordered list of stage holders for the pipeline (may be empty, never null)
     */
    List<StageHolder> stagesFor(String pipelineName);

    /**
     * Immutable holder describing a single stage inside a pipeline.
     * Encapsulates a stage id, order, the stage implementation object, and a pre-execution condition.
     */
    final class StageHolder {
        private final String id;
        private final int order;
        private final Object step; // typically a StageStep<?>
        private final Predicate<StageContext> condition;

        /**
         * @param id        stable stage identifier (used in logs/metrics)
         * @param order     execution order (lower first)
         * @param step      stage implementation (usually a StageStep)
         * @param condition predicate that determines if the stage should run given the current context
         */
        public StageHolder(String id, int order, Object step, Predicate<StageContext> condition) {
            this.id = id;
            this.order = order;
            this.step = step;
            this.condition = condition;
        }

        /** @return stage identifier */
        public String id() {
            return id;
        }
        /** @return stage order (lower first) */
        public int order() {
            return order;
        }
        /** @return stage implementation instance */
        public Object step() {
            return step;
        }
        /** @return condition predicate evaluated before execution */
        public Predicate<StageContext> condition() {
            return condition;
        }
    }
}
