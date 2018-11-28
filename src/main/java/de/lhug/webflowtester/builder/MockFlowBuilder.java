package de.lhug.webflowtester.builder;

import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.FlowExecution;

/**
 * Base interface for all MockFlowBuilder implementations.
 * 
 * Implementation classes must let this return a valid and executable
 * {@link Flow} object which can be started in a {@link FlowExecution}.
 *
 */
@FunctionalInterface
public interface MockFlowBuilder {

    /**
     * Builds a ready-to-use {@link Flow} instance.
     * 
     * It is explicitly not guaranteed that successive calls succeed or return
     * distinct results, as implementations may decide to cache the builds or
     * disallow multiple calls via {@link RuntimeException}.
     * 
     * @return a usable {@link Flow}
     */
    Flow buildFlow();

}