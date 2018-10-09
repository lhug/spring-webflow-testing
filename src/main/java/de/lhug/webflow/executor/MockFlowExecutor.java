package de.lhug.webflow.executor;

import org.springframework.util.Assert;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.engine.impl.FlowExecutionImplFactory;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionFactory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockFlowExecutor {

	private final FlowDefinition flowDefinition;
	private final FlowExecutionFactory flowExecutionFactory = new FlowExecutionImplFactory();

	@Getter
	private FlowExecution flowExecution;

	public void startFlow(ExternalContext externalContext) {
		startFlow(null, externalContext);
	}

	public void startFlow(MutableAttributeMap parameters, ExternalContext externalContext) {
		buildFlowExecution();
		flowExecution.start(parameters, externalContext);
	}

	private void buildFlowExecution() {
		flowExecution = flowExecutionFactory.createFlowExecution(flowDefinition);
	}

	public void resumeFlow(ExternalContext context) {
		Assert.state(flowExecution != null, "Flow must be started before it can be resumed");
		flowExecution.resume(context);
	}

	public void setCurrentState(String stateId) {
		if (flowExecution == null) {
			buildFlowExecution();
		}
		setState(stateId);
	}

	private void setState(String stateId) {
		((FlowExecutionImpl) flowExecution).setCurrentState(stateId);
	}

}
