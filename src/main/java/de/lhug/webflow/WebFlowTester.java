package de.lhug.webflow;

import java.net.URL;

import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.test.execution.AbstractXmlFlowExecutionTests;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WebFlowTester extends AbstractXmlFlowExecutionTests {

	private final URL flowUrl;

	@Override
	protected FlowDefinitionResource getResource(FlowDefinitionResourceFactory resourceFactory) {
		return resourceFactory.createResource(flowUrl.toExternalForm());
	}

	@Override
	public void startFlow(ExternalContext context) {
		super.startFlow(context);
	}

	@Override
	public FlowExecution getFlowExecution() {
		return super.getFlowExecution();
	}

	@Override
	public void setCurrentState(String stateId) {
		super.setCurrentState(stateId);
	}

	@Override
	public void resumeFlow(ExternalContext context) {
		super.resumeFlow(context);
	}
}
