package de.lhug.webflowtester.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.test.MockExternalContext;

import de.lhug.webflowtester.builder.MockFlowBuilder;
import de.lhug.webflowtester.builder.XMLMockFlowBuilder;
import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;

public class MockFlowTesterTest {

    private XMLMockFlowConfiguration configuration;
    private FlowTestContext context;

    private MockFlowTester sut;

    @Test
    public void shouldBuildMockFlowExecutorFromConfiguration() throws Exception {
        XMLMockFlowConfiguration simpleFlow = new XMLMockFlowConfiguration("/simpleFlows/standaloneFlow.xml");
        MockFlowBuilder simpleFlowBuilder = new XMLMockFlowBuilder(simpleFlow);

        MockFlowTester tester = MockFlowTester.from(simpleFlowBuilder);

        assertThat(tester, is(not(nullValue())));
    }

    @Test
    public void shouldReturnNullAsCurrentFlowExecutionIfFlowIsNotStarted() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        FlowExecution result = sut.getCurrentFlowExecution();

        assertThat(result, is(nullValue()));
    }

    private void initConfigFrom(String flowPath) {
        configuration = new XMLMockFlowConfiguration(flowPath);
    }

    private void initSut() {
        MockFlowBuilder builder = new XMLMockFlowBuilder(configuration).withContext(context);
        sut = MockFlowTester.from(builder);
    }

    @Test
    public void shouldReturnCurrentFlowExecutionWhenFlowIsStarted() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        FlowExecution result = sut.getCurrentFlowExecution();

        assertThat(result.hasStarted(), is(true));
        assertThat(result.getDefinition().getId(), is("standaloneFlow"));
    }

    @Test
    public void shouldStartFlowExecution() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        sut.startFlow();

        FlowExecution execution = sut.getCurrentFlowExecution();
        assertThat(execution.hasStarted(), is(true));
        assertThat(execution.getDefinition().getId(), is("standaloneFlow"));
        assertThat(execution.getActiveSession().getState().getId(), is("start"));
    }

    @Test
    public void shouldSetCurrentStateWithoutEnteringOtherStates() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        sut.startFlowAt("step");

        FlowExecution execution = sut.getCurrentFlowExecution();
        assertThat(execution.hasStarted(), is(true));
        assertThat(execution.getDefinition().getId(), is("standaloneFlow"));
        assertThat(execution.getActiveSession().getState().getId(), is("step"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenNoEventIsSetWhenResumingFlow() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        sut.resumeFlow();
    }

    @Test
    public void shouldResumeFlowWithGivenEventId() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        sut.setEventId("page");

        sut.resumeFlow();
        FlowExecution execution = sut.getCurrentFlowExecution();
        assertThat(execution.hasStarted(), is(true));
        assertThat(execution.getDefinition().getId(), is("standaloneFlow"));
        assertThat(execution.getActiveSession().getState().getId(), is("step"));
    }

    @Test
    public void shouldPassInputAttributes() throws Exception {
        initConfigFrom("/simpleFlows/flowWithInput.xml");
        initSut();
        Map<String, String> arguments = Collections.singletonMap("inputArgument", "this is a String");

        sut.startFlow(arguments);

        FlowExecution execution = sut.getCurrentFlowExecution();
        Object argument = execution.getActiveSession().getScope().get("inputArgument");
        assertThat(argument, is("this is a String"));
    }

    @Test
    public void shouldResumeFlowWithInputArguments() throws Exception {
        initConfigFrom("/simpleFlows/flowWithInput.xml");
        initSut();
        Map<String, String> arguments = Collections.singletonMap("inputParameter", "well, why not");
        sut.startFlowAt("start");
        sut.setEventId("page");

        sut.resumeFlow(arguments);

        FlowExecution execution = sut.getCurrentFlowExecution();
        Object argument = execution.getActiveSession().getScope().get("passed");
        assertThat(argument, is("well, why not"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenAssertingEndedFlowExecutionBeforeFlowWasStarted() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        sut.assertFlowExecutionEnded();
    }

    @Test(expected = AssertionError.class)
    public void shouldThrowExceptionWhenAssertingEndedFlowExecutionOnActiveFlow() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        sut.assertFlowExecutionEnded();
    }

    @Test
    public void shouldAssertFlowExecutionHasEnded() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();
        sut.setEventId("close");
        sut.resumeFlow();

        sut.assertFlowExecutionEnded();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenAssertingOutcomeBeforeFlowWasStarted() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        sut.assertFlowOutcomeIs("bye");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAssertionOutcomeBeforeFlowHasended() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        sut.assertFlowOutcomeIs("bye");
    }

    @Test(expected = AssertionError.class)
    public void shouldThrowExceptionWhenOutcomeIsNotAsExpected() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();
        sut.setEventId("close");
        sut.resumeFlow();

        sut.assertFlowOutcomeIs("jupiter");
    }

    @Test
    public void shoultAssertCorrectFlowExecutionOutcome() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();
        sut.setEventId("close");
        sut.resumeFlow();

        sut.assertFlowOutcomeIs("bye");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenGettingOutputArgumentsBeforeFlowWasStarted() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();

        sut.getOutputAttributes();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenGettingOutputArgumentsBeforeFlowHasEnded() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();
        sut.startFlow(Collections.singletonMap("to", "bananas"));

        sut.getOutputAttributes();
    }

    @Test
    public void shouldReturnOutputAttributesWhenFlowHasEnded() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();
        sut.startFlow(Collections.singletonMap("to", "output"));

        AttributeMap result = sut.getOutputAttributes();

        assertThat(result.get("out"), is("hooray"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAssertingExternalRedirectBeforeFlowWasStarted() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();

        sut.assertExternalRedirectTo("http://www.google.de");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAssertingExternalRedirectBeforeFlowHasEnded() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();
        sut.startFlow(Collections.singletonMap("to", "bananas"));

        sut.assertExternalRedirectTo("http://www.google.de");
    }

    @Test(expected = AssertionError.class)
    public void shouldThrowExceptionWhenAssertingExternalRedirectToWrongUrl() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();
        sut.startFlow(Collections.singletonMap("to", "redirect"));

        sut.assertExternalRedirectTo("http://www.bing.com");
    }

    @Test
    public void shouldAssertExternalRedirect() throws Exception {
        initConfigFrom("/simpleFlows/flowWithOutput.xml");
        initSut();
        sut.startFlow(Collections.singletonMap("to", "redirect"));

        sut.assertExternalRedirectTo("http://www.google.de");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToAccessCurrentStateBeforeFlowWasStarted() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        sut.assertCurrentStateIs("start");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToAccessCurrentStateAfterFlowHasEnded() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();
        sut.setEventId("close");
        sut.resumeFlow();

        sut.assertCurrentStateIs("start");
    }

    @Test(expected = AssertionError.class)
    public void shouldThrowExceptionWhenAssertingWrongStateId() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        sut.assertCurrentStateIs("step");
    }

    @Test
    public void shouldAssertCurrentStateId() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        sut.assertCurrentStateIs("start");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAccessingScopeBeforeFlowWasStarted() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        sut.getScope();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAccessingScopeAfterFlowEnded() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();
        sut.setEventId("close");
        sut.resumeFlow();

        sut.getScope();
    }

    @Test
    public void shouldReturnAttributeMapWhenAccessingScopeDuringFlowExecution() throws Exception {
        initConfigFrom("/simpleFlows/flowWithInput.xml");
        initSut();
        Map<String, String> arguments = Collections.singletonMap("inputArgument", "this is a String");
        sut.startFlow(arguments);

        AttributeMap result = sut.getScope();

        assertThat(result.get("inputArgument"), is("this is a String"));
    }

    @Test
    public void shouldReturnNullIfNoRequestHasBeenSent() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();

        MockExternalContext result = sut.getLastRequestContext();

        assertThat(result, is(nullValue()));
    }

    @Test
    public void shouldReturnLastRequestContextAfterRequestHasBeenSent() throws Exception {
        initConfigFrom("/simpleFlows/standaloneFlow.xml");
        initSut();
        sut.startFlow();

        MockExternalContext result = sut.getLastRequestContext();

        assertThat(result, is(not(nullValue())));
        assertThat(result.isResponseComplete(), is(true));
    }

}
