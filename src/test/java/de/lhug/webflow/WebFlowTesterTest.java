package de.lhug.webflow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.test.MockExternalContext;

public class WebFlowTesterTest {

	private WebFlowTester sut;

	private MockExternalContext externalContext;

	@Before
	public void setUp() {
		URL flowXml = getClass().getResource("/exampleFlow.xml");
		sut = new WebFlowTester(flowXml);
		externalContext = new MockExternalContext();
	}

	@Test
	public void shouldStartFlow() throws Exception {
		sut.startFlow(externalContext);
	}

	@Test
	public void shouldReturnNullBeforeFlowHasStarted() throws Exception {
		FlowExecution result = sut.getFlowExecution();

		assertThat(result, is(nullValue()));
	}

	@Test
	public void shouldReturnFlowExecutionAfterStartingWebFlow() throws Exception {
		sut.startFlow(externalContext);

		FlowExecution result = sut.getFlowExecution();

		assertThat(result, is(not(nullValue())));
		assertThat(result.getDefinition().getId(), is("exampleFlow"));
	}

	@Test
	public void shouldSetInitialFlowState() throws Exception {
		sut.setCurrentState("step");

		assertThat(sut.getFlowExecution().getActiveSession().getState().getId(), is("step"));
	}

	@Test
	public void shouldExecuteTransitionAndStopAtViewState() throws Exception {
		sut.setCurrentState("step");
		externalContext.setEventId("back");

		sut.resumeFlow(externalContext);

		assertThat(sut.getFlowExecution().getActiveSession().getState().getId(), is("start"));
	}
}
