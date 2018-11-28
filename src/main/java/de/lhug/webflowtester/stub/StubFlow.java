package de.lhug.webflowtester.stub;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;
import org.springframework.webflow.engine.EndState;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.FlowExecutionOutcome;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Implementation of {@link FlowDefinitionHolder} to allow easy testing of
 * Sub-Flow results
 */
@EqualsAndHashCode(of = { "flowDefinitionId", "endStateId" })
public class StubFlow implements FlowDefinitionHolder {

    @Getter
    private final String flowDefinitionId;

    @Getter
    private String endStateId;
    private MutableAttributeMap inputAttributes = new LocalAttributeMap();
    @Setter
    private Map<String, Object> outputAttributes = new HashMap<>();

    private Flow cachedFlow;

    /**
     * Creates an initial instance of this {@link FlowDefinitionHolder}.
     * 
     * As the Flow is required to end immediately, an endStateId is required to
     * ensure this happens. The endStateId can be changed dynamically using
     * {@link #setEndStateId}.
     * 
     * @param flowId
     *            the String identifying the flow, not {@code null}
     * @param endStateId
     *            the String to be emitted as {@link FlowExecutionOutcome} id,
     *            not {@code null}
     */
    public StubFlow(String flowId, String endStateId) {
        Assert.notNull(flowId, "Flow Id may not be null");
        Assert.notNull(endStateId, "EndState Id may not be null");
        this.flowDefinitionId = flowId;
        this.endStateId = endStateId;
    }

    /**
     * Updates the initially supplied endStateId
     * 
     * After this is called, a subsequent call to {@link #getFlowDefinition()}
     * will trigger a Flow Rebuild
     * 
     * @param endStateId
     *            the new String to be assigned as {@link FlowExecutionOutcome},
     *            not {@code null}
     */
    public void setEndStateId(String endStateId) {
        Assert.notNull(endStateId, "EndState Id may not be null");
        this.endStateId = endStateId;
    }

    @Override
    public FlowDefinition getFlowDefinition() {
        if (cachedFlow == null || !cachedFlow.getPossibleOutcomes()[0].equals(endStateId)) {
            cachedFlow = new Flow(flowDefinitionId);
            new EndState(cachedFlow, endStateId);
            cachedFlow.setInputMapper((inputMap, requestControlContext) -> {
                inputAttributes.putAll((AttributeMap) inputMap);
                return null;
            });
            cachedFlow.setOutputMapper((requestControlContext, outputMap) -> {
                ((MutableAttributeMap) outputMap).putAll(new LocalAttributeMap(outputAttributes));
                return null;
            });
        }
        return cachedFlow;
    }

    public AttributeMap getInputAttributes() {
        MutableAttributeMap result = new LocalAttributeMap();
        result.putAll(inputAttributes);
        inputAttributes.clear();
        return result;
    }

    public void addOutputAttribute(String key, Object value) {
        outputAttributes.put(key, value);
    }

    /**
     * As stub flow does not mirror a resource, this will always return
     * {@code null}
     */
    @Override
    public String getFlowDefinitionResourceString() {
        return null;
    }

    /**
     * does nothing; rebuilding will be triggered conditionally on
     * {@link #getFlowDefinition()}
     */
    @Override
    public void refresh() {
        // Intentionally left empty
    }

    /**
     * does nothing as no resources are bound
     */
    @Override
    public void destroy() {
        // Intentionally left empty
    }
}
