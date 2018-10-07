package de.lhug.webflow.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.test.MockExternalContext;

import de.lhug.webflow.util.TestFlowDefinitionFactory;

public class MockFlowExecutorTest {

	private FlowDefinition flowDefinition;

	private MutableAttributeMap input = new LocalAttributeMap();
	private MockExternalContext context = new MockExternalContext();
	private MockFlowExecutor sut;

	@Before
	public void setUp() {
		flowDefinition = TestFlowDefinitionFactory.simpleFlow();
		sut = new MockFlowExecutor(flowDefinition);
	}

	@Test
	public void shouldStartFlowAtBeginning() {
		sut.startFlow(context);

		FlowExecution result = sut.getFlowExecution();
		assertThat(result.hasStarted(), is(true));
		assertThat(result.isActive(), is(true));
		assertThat(result.hasEnded(), is(false));
		assertThat(result.getActiveSession().getState().getId(), is("start"));
		assertThat(result.getActiveSession().getScope().get("someAttribute"), is(nullValue()));
	}

	@Test
	public void shouldPassInputAttributesToFlow() throws Exception {
		input.put("someAttribute", "someValue");

		sut.startFlow(input, context);

		FlowExecution result = sut.getFlowExecution();
		assertThat(result.hasStarted(), is(true));
		assertThat(result.isActive(), is(true));
		assertThat(result.hasEnded(), is(false));
		assertThat(result.getActiveSession().getState().getId(), is("start"));
		assertThat(result.getActiveSession().getScope().get("someAttribute"), is("someValue"));
	}

	@Test
	public void shouldResumeFlow() throws Exception {
		sut.startFlow(context);
		context.setEventId("page");

		sut.resumeFlow(context);

		FlowExecution result = sut.getFlowExecution();
		assertThat(result.hasEnded(), is(false));
		assertThat(result.getActiveSession().getState().getId(), is("step"));
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenResumingInactiveFlow() throws Exception {
		sut.resumeFlow(context);
	}

	@Test
	public void shouldStartFlowAtArbitraryState() throws Exception {
		sut.setCurrentState("step");

		FlowExecution result = sut.getFlowExecution();
		assertThat(result.hasStarted(), is(true));
		assertThat(result.isActive(), is(true));
		assertThat(result.hasEnded(), is(false));
		assertThat(result.getActiveSession().getState().getId(), is("step"));
	}

	@Test
	public void shouldSetFlowToDesiredStateWhenFlowIsStarted() throws Exception {
		sut.startFlow(context);

		sut.setCurrentState("bye");

		// Flow is set to end-state, but flow is paused
		FlowExecution result = sut.getFlowExecution();
		assertThat(result.getActiveSession().getState().getId(), is("bye"));
	}

	@Test
	public void shouldEndFlowWhenEnteringEndState() throws Exception {
		sut.startFlow(context);
		context.setEventId("close");

		sut.resumeFlow(context);

		FlowExecution result = sut.getFlowExecution();
		assertThat(result.isActive(), is(false));
		assertThat(result.hasEnded(), is(true));
	}
}
