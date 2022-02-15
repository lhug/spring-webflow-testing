package de.lhug.webflowtester.executor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import de.lhug.webflowtester.builder.MockFlowBuilder;
import de.lhug.webflowtester.builder.XMLMockFlowBuilder;
import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.binding.message.Message;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.test.MockExternalContext;

public class MockFlowTesterTest {

	private XMLMockFlowConfiguration configuration;
	private FlowTestContext context;

	private MockFlowTester sut;

	@Test
	public void shouldBuildMockFlowExecutorFromConfiguration() {
		XMLMockFlowConfiguration simpleFlow = new XMLMockFlowConfiguration(
				"/simpleFlows/standaloneFlow.xml");
		MockFlowBuilder simpleFlowBuilder = new XMLMockFlowBuilder(simpleFlow);

		MockFlowTester tester = MockFlowTester.from(simpleFlowBuilder);

		assertThat(tester).isNotNull();
	}

	@Test
	public void shouldReturnNullAsCurrentFlowExecutionIfFlowIsNotStarted() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		FlowExecution result = sut.getCurrentFlowExecution();

		assertThat(result).isNull();
	}

	private void initConfigFrom(String flowPath) {
		configuration = new XMLMockFlowConfiguration(flowPath);
	}

	private void initSut() {
		MockFlowBuilder builder = new XMLMockFlowBuilder(configuration).withContext(context);
		sut = MockFlowTester.from(builder);
	}

	@Test
	public void shouldReturnCurrentFlowExecutionWhenFlowIsStarted() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		FlowExecution result = sut.getCurrentFlowExecution();

		assertThat(result.hasStarted()).isTrue();
		assertThat(result.getDefinition().getId()).isEqualTo("standaloneFlow");
	}

	@Test
	public void shouldStartFlowExecution() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		sut.startFlow();

		FlowExecution execution = sut.getCurrentFlowExecution();
		assertThat(execution.hasStarted()).isTrue();
		assertThat(execution.getDefinition().getId()).isEqualTo("standaloneFlow");
		assertThat(execution.getActiveSession().getState().getId()).isEqualTo("start");
	}

	@Test
	public void shouldSetCurrentStateWithoutEnteringOtherStates() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		sut.startFlowAt("step");

		FlowExecution execution = sut.getCurrentFlowExecution();
		assertThat(execution.hasStarted()).isTrue();
		assertThat(execution.getDefinition().getId()).isEqualTo("standaloneFlow");
		assertThat(execution.getActiveSession().getState().getId()).isEqualTo("step");
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowIllegalStateExceptionWhenNoEventIsSetWhenResumingFlow() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		sut.resumeFlow();
	}

	@Test
	public void shouldResumeFlowWithGivenEventId() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		sut.setEventId("page");

		sut.resumeFlow();
		FlowExecution execution = sut.getCurrentFlowExecution();
		assertThat(execution.hasStarted()).isTrue();
		assertThat(execution.getDefinition().getId()).isEqualTo("standaloneFlow");
		assertThat(execution.getActiveSession().getState().getId()).isEqualTo("step");
	}

	@Test
	public void shouldPassInputAttributes() {
		initConfigFrom("/simpleFlows/flowWithInput.xml");
		initSut();
		Map<String, String> arguments = Collections.singletonMap("inputArgument", "this is a String");

		sut.startFlow(arguments);

		FlowExecution execution = sut.getCurrentFlowExecution();
		Object argument = execution.getActiveSession().getScope().get("inputArgument");
		assertThat(argument).isEqualTo("this is a String");
	}

	@Test
	public void shouldResumeFlowWithInputArguments() {
		initConfigFrom("/simpleFlows/flowWithInput.xml");
		initSut();
		Map<String, String> arguments = Collections.singletonMap("inputParameter", "well, why not");
		sut.startFlowAt("start");
		sut.setEventId("page");

		sut.resumeFlow(arguments);

		FlowExecution execution = sut.getCurrentFlowExecution();
		Object argument = execution.getActiveSession().getScope().get("passed");
		assertThat(argument).isEqualTo("well, why not");
	}

	@Test
	public void shouldThrowIllegalStateExceptionWhenAssertingEndedFlowExecutionBeforeFlowWasStarted() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		assertThatThrownBy(() -> sut.executionHasEnded())
				.isInstanceOf((IllegalStateException.class))
				.hasMessage("Flow must be started before assertions can be made.");
	}

	@Test
	public void shouldReturnFalseWhenFlowIsActive() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		assertThat(sut.executionHasEnded()).isFalse();
	}

	@Test
	public void shouldReturnTrueWhenFlowExecutionHasEnded() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();
		sut.setEventId("close");
		sut.resumeFlow();

		assertThat(sut.executionHasEnded()).isTrue();
	}

	@Test
	public void shouldThrowIllegalStateExceptionWhenAssertingOutcomeBeforeFlowWasStarted() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		assertThatThrownBy(() -> sut.getFlowOutcome())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Flow must be started before assertions can be made.");
	}

	@Test
	public void shouldThrowExceptionWhenAssertionOutcomeBeforeFlowHasEnded() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		assertThatThrownBy(() -> sut.getFlowOutcome())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Flow Execution must have ended to assert the outcome");
	}

	@Test
	public void shouldReturnFlowOutcome() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();
		sut.setEventId("close");
		sut.resumeFlow();

		assertThat(sut.getFlowOutcome()).isEqualTo("bye");
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenGettingOutputArgumentsBeforeFlowWasStarted() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();

		sut.getOutputAttributes();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenGettingOutputArgumentsBeforeFlowHasEnded() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();
		sut.startFlow(Collections.singletonMap("to", "bananas"));

		sut.getOutputAttributes();
	}

	@Test
	public void shouldReturnOutputAttributesWhenFlowHasEnded() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();
		sut.startFlow(Collections.singletonMap("to", "output"));

		AttributeMap<Object> result = sut.getOutputAttributes();

		assertThat(result.get("out")).isEqualTo("hooray");
	}

	@Test
	public void shouldThrowExceptionWhenAssertingExternalRedirectBeforeFlowWasStarted() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();

		assertThatThrownBy(() -> sut.getExternalRedirectUrl())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Flow must be started before assertions can be made.");
	}

	@Test
	public void shouldThrowExceptionWhenAssertingExternalRedirectBeforeFlowHasEnded() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();
		sut.startFlow(Collections.singletonMap("to", "bananas"));

		assertThatThrownBy(() -> sut.getExternalRedirectUrl())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Flow Execution must have ended to assert the outcome");
	}

	@Test
	public void shouldReturnNullWhenFlowDidNotRenderRedirectUrl() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();
		sut.startFlow(Map.of("to", "output"));

		assertThat(sut.getExternalRedirectUrl()).isNull();
	}

	@Test
	public void shouldReturnExternalRedirectUrl() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();
		sut.startFlow(Map.of("to", "redirect"));

		assertThat(sut.getExternalRedirectUrl()).isEqualTo("http://www.google.de");
	}

	@Test
	public void shouldThrowExceptionWhenTryingToAccessCurrentStateBeforeFlowWasStarted() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		assertThatThrownBy(() -> sut.getCurrentStateId())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Flow must be started before assertions can be made.");
	}

	@Test
	public void shouldThrowExceptionWhenTryingToAccessCurrentStateAfterFlowHasEnded() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();
		sut.setEventId("close");
		sut.resumeFlow();

		assertThatThrownBy(() -> sut.getCurrentStateId())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Flow Execution must be active to assert current events");
	}

	@Test
	public void shouldReturnCurrentStateId() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		assertThat(sut.getCurrentStateId()).isEqualTo("start");
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenAccessingScopeBeforeFlowWasStarted() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		sut.getScope();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenAccessingScopeAfterFlowEnded() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();
		sut.setEventId("close");
		sut.resumeFlow();

		sut.getScope();
	}

	@Test
	public void shouldReturnAttributeMapWhenAccessingScopeDuringFlowExecution() {
		initConfigFrom("/simpleFlows/flowWithInput.xml");
		initSut();
		Map<String, String> arguments = Collections.singletonMap("inputArgument", "this is a String");
		sut.startFlow(arguments);

		AttributeMap<Object> result = sut.getScope();

		assertThat(result.get("inputArgument")).isEqualTo("this is a String");
	}

	@Test
	public void shouldReturnNullIfNoRequestHasBeenSent() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();

		MockExternalContext result = sut.getLastRequestContext();

		assertThat(result, is(nullValue()));
	}

	@Test
	public void shouldReturnLastRequestContextAfterRequestHasBeenSent() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		MockExternalContext result = sut.getLastRequestContext();

		assertThat(result).isNotNull();
		assertThat(result.isResponseComplete()).isTrue();
	}

	@Test
	public void shouldReturnAllMessages() {
		initConfigFrom("/simpleFlows/messageAddingFlow.xml");
		context = new FlowTestContext();
		context.addBean("service", new SomeService());
		initSut();
		sut.startFlowAt("start");
		sut.setEventId("message");
		sut.resumeFlow();

		Set<Message> result = sut.getAllMessages();

		assertThat(result)
				.extracting(Message::getText)
				.containsExactly("This is a message");
	}

	public static class SomeService {
		@SuppressWarnings("unused") // used in flow
		public void addMessage(MessageContext messageContext) {
			messageContext.addMessage(new MessageBuilder()
					.source("service")
					.info().defaultText("This is a message").build());
		}
	}

	@Test
	public void shouldStartFlowWithPassedRequestObject() {
		initConfigFrom("/simpleFlows/flowWithOutput.xml");
		initSut();
		MockHttpServletRequest request = MockMvcRequestBuilders.get("/some/path").buildRequest(new MockServletContext());
		sut.setRequest(request);
		sut.startFlow(Collections.singletonMap("to", "emitRequest"));

		assertThat(sut.getOutputAttributes().get("request"))
				.isSameAs(request);
	}

	@Test
	public void shouldResumeFlowWithPassedRequestObject() {
		initConfigFrom("/simpleFlows/standaloneFlow.xml");
		initSut();
		sut.startFlow();

		Map<String, String> request = new HashMap<>();
		sut.setRequest(request);

		sut.setEventId("page");
		sut.resumeFlow();

		assertThat(sut.getLastRequestContext().getNativeRequest()).isSameAs(request);
	}
}
