package de.lhug.webflowtester.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.engine.impl.FlowExecutionImplFactory;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionFactory;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockParameterMap;

import de.lhug.webflowtester.builder.MockFlowBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Tester class for simplified WebFlow Testing
 * 
 * This class can be used for easy testing of Spring WebFlows. It exposes
 * necessary control mechanisms as well as a suite of convenience-methods for
 * common assertions. The passed {@link MockFlowBuilder} supplies the
 * {@link Flow} to be tested, which in turn will be built once and then kept,
 * meaning that this class does <b>not</b> support Flows stored in
 * {@link FlowDefinitionHolder}s. A typical test case, given a simple flow like
 * so
 * <p>
 * 
 * <pre>
 * &lt;view-state id=&quot;start&quot;&gt;
 *   &lt;transition on=&quot;next&quot; to=&quot;end&quot; /&gt;
 * &lt;/view-state&gt;
 * &lt;end-state id=&quot;end&quot; /&gt;
 * </pre>
 * 
 * would be tested like so:
 * 
 * <pre>
 * MockFlowTester tester = MockFlowTester.from(builder);
 * tester.startFlow();
 * tester.assertCurrentStateIs("start");
 * 
 * tester.setEventId("next");
 * tester.resumeFlow();
 * 
 * tester.assertFlowOutcomeIs("end");
 * </pre>
 * <p>
 * All assertions will throw {@link IllegalStateException} when invoked at the
 * wrong time, e.G. an outcome-assertion before the flow was started.
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MockFlowTester {

    /**
     * Builds an instance of this using a {@link MockFlowBuilder}.
     * 
     * The {@link MockFlowBuilder#buildFlow()} method is called before the
     * instance is constructed, meaning the passed builder can be replaced with
     * a lambda directly supplying a preconstructed {@link Flow} instance.
     * 
     * @param builder
     *            an implementation of {@link MockFlowBuilder} to supply the
     *            {@link Flow} instance
     * @return an initialized MockFlowTester
     * @throws NullPointerException
     *             when the builder is null
     */
    public static MockFlowTester from(MockFlowBuilder builder) {
        return new MockFlowTester(builder.buildFlow());
    }

    private final FlowExecutionFactory executionFactory = new FlowExecutionImplFactory();
    private final Flow testFlow;

    private FlowExecutionImpl execution;
    private MockExternalContext context;
    private String eventId;

    /**
     * Returns the current Flow Execution.
     * 
     * This does not do any state-checks but merely returns the currently used
     * {@link FlowExecution} instance.
     * 
     * @return the current {@link FlowExecution}, or <code>null</code> if no
     *         execution is present
     */
    public FlowExecution getCurrentFlowExecution() {
        return execution;
    }

    /**
     * Starts the flow.
     * 
     * Discards the previous {@link FlowExecution} if present, and all other
     * passed information, and creates a fresh instance. This does not generate
     * any warnings.
     * 
     * Methods on requiring an active session can be called after this.
     * 
     * @see #getScope()
     * @see #assertCurrentStateIs(String)
     * @see #resumeFlow()
     */
    public void startFlow() {
        startFlow(Collections.emptyMap());
    }

    /**
     * Starts the flow with the given input arguments.
     * 
     * Discards the previous {@link FlowExecution} if present, and all other
     * passed information, and creates a fresh instance, directly passing the
     * input arguments. This does not generate any warnings.
     * 
     * Methods on requiring an active session can be called after this.
     * 
     * @param inputArguments
     *            a {@link Map} containing the Attributes to be passed to the
     *            {@link Flow}, not <code>null</code>
     * @see #getScope()
     * @see #assertCurrentStateIs(String)
     * @see #resumeFlow()
     */
    public void startFlow(Map<? extends String, ? extends Object> inputArguments) {
        initFlowExecution();
        newContext();
        eventId = null;
        execution.start(new LocalAttributeMap(inputArguments), context);
    }

    private void newContext() {
        context = new MockExternalContext();
    }

    private void initFlowExecution() {
        execution = (FlowExecutionImpl) executionFactory.createFlowExecution(testFlow);
    }

    /**
     * Starts the flow execution at the given state id
     * 
     * This creates a new {@link FlowExecution}, discarding any previous
     * Executions, and sets the current state to the passed {@code stateId}. The
     * resulting state is as if the flow had just entered the given state,
     * meaning that the declared <code><on-entry></code> directives are
     * considered to be finished.
     * <p>
     * To make this obvious: this does <b>not</b> actually call the
     * entry-actions.
     * <p>
     * This is used to avoid having to run through the entire flow and test
     * states in isolation. After this has been called, the current flow is
     * active.
     * 
     * @param stateId
     *            the state to enter
     */
    public void startFlowAt(String stateId) {
        initFlowExecution();
        execution.setCurrentState(stateId);
    }

    /**
     * Continues the active flow execution.
     * 
     * Before this is called, an {@link #setEventId(String) event} must be set
     * to allow continuation of the flow. The event is then used to determine
     * the next transition of the flow.
     * <p>
     * Every call of this method is being treated as a new Request, meaning that
     * any previously given Request parameters are discarded.
     * 
     * @throws IllegalStateException
     *             when no event id is set or no flow execution is available
     * 
     * @see FlowExecution#resume(ExternalContext)
     * @see #setEventId(String)
     */
    public void resumeFlow() {
        resumeFlow(Collections.emptyMap());
    }

    /**
     * Continues the active flow execution with the given request parameters.
     * 
     * Before this is called, an {@link #setEventId(String) event} must be set
     * to allow continuation of the flow. The event is then used to determine
     * the next transition of the flow.
     * <p>
     * Every call of this method is being treated as a new Request, meaning that
     * any previously given Request parameters are discarded.
     * <p>
     * The given RequestParameters support three Object types:
     * <ul>
     * <li>{@link MultipartFile}</li>
     * <li>{@link String}[]</li>
     * <li>{@link String}</li>
     * </ul>
     * More formally, everything that is neither a {@link MultipartFile} or a
     * {@link String}[] will be converted to String using
     * {@link Objects#toString(Object)}
     * 
     * @param inputArguments
     *            a {@link Map} containing the current RequestParameters
     * 
     * @throws IllegalStateException
     *             when no event id is set or no flow execution is available
     * 
     * @see FlowExecution#resume(ExternalContext)
     * @see #setEventId(String)
     */
    public void resumeFlow(Map<? extends String, ? extends Object> inputArguments) {
        assertActiveExecution();
        Assert.state(eventId != null, "An event ID must be set to resume the flow");
        newContext();
        setRequestParameters(inputArguments);
        context.setEventId(eventId);
        execution.resume(context);
    }

    private void setRequestParameters(Map<? extends String, ? extends Object> inputArguments) {
        MockParameterMap parameterMap = new MockParameterMap();
        inputArguments.entrySet().forEach(entry -> {
            Object value = entry.getValue();
            if (value instanceof String[]) {
                parameterMap.put(entry.getKey(), (String[]) value);
            } else if (value instanceof MultipartFile) {
                parameterMap.put(entry.getKey(), (MultipartFile) value);
            } else {
                parameterMap.put(entry.getKey(), Objects.toString(value));
            }
        });
        context.setRequestParameterMap(parameterMap);
    }

    /**
     * Sets the event to be called on the next resume operation
     * 
     * @param eventId
     *            the next event id
     * @see #resumeFlow()
     * @see #resumeFlow(Map)
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Asserts that the current flow execution has ended, meaning an
     * {@link AssertionError} is raised if the flow has not ended
     * 
     * @throws IllegalStateException
     *             if no active {@link FlowExecution} is present
     */
    public void assertFlowExecutionEnded() {
        assertActiveExecution();

        assertThat(execution.hasEnded(), is(true));
    }

    private void assertActiveExecution() {
        Assert.state(execution != null, "Flow must be started before assertions can be made.");
    }

    /**
     * Asserts that the flow returned with the given outcome, meaning the given
     * endState Id.
     * 
     * Will raise an {@link AssertionError} if the outcome id was not as
     * expected.
     * 
     * @param expectedOutcome
     *            the expected flow execution outcome
     * @throws IllegalStateException
     *             if no {@link FlowExecution} is present or the current flow
     *             execution has not ended
     */
    public void assertFlowOutcomeIs(String expectedOutcome) {
        assertActiveExecution();
        assertOutcomeAccessible();

        assertThat(execution.getOutcome().getId(), is(expectedOutcome));
    }

    private void assertOutcomeAccessible() {
        Assert.state(execution.hasEnded(), "Flow Execution must have ended to assert the outcome");
    }

    /**
     * Returns any given output attributes
     * 
     * @return an {@link AttributeMap} containing all output attributes of the
     *         current flow.
     * @throws IllegalStateException
     *             if no {@link FlowExecution} is present or the current flow
     *             execution has not ended
     */
    public AttributeMap getOutputAttributes() {
        assertActiveExecution();
        assertOutcomeAccessible();

        return execution.getOutcome().getOutput();
    }

    /**
     * Asserts that an external redirect to the passed url was rendered.
     * 
     * Raises an {@link AssertionError} if the actual url differs from the
     * passed url.
     * 
     * @param url
     *            The URL that should have been redirected to
     * @throws IllegalStateException
     *             if no {@link FlowExecution} is present or the current flow
     *             execution has not ended
     */
    public void assertExternalRedirectTo(String url) {
        assertActiveExecution();
        assertOutcomeAccessible();

        assertThat(context.getExternalRedirectUrl(), is(url));
    }

    /**
     * Asserts the current state id.
     * 
     * Raises an {@link AssertionError} if the current state id differs from the
     * passed stateId.
     * 
     * @param stateId
     *            the expected id of the current state
     * @throws IllegalStateException
     *             if no {@link FlowExecution} is present or the current flow
     *             execution has ended
     */
    public void assertCurrentStateIs(String stateId) {
        assertActiveExecution();
        assertActiveSessionAccessible();

        assertThat(execution.getActiveSession().getState().getId(), is(stateId));
    }

    private void assertActiveSessionAccessible() {
        Assert.state(execution.isActive(), "Flow Execution must be active to assert current events");
    }

    /**
     * Returns the Flow Scope Attributes of the current flow.
     * 
     * @return the {@link AttributeMap} containing the current Flow Scope
     * @throws IllegalStateException
     *             if no {@link FlowExecution} is present or the current flow
     *             execution has ended
     */
    public AttributeMap getScope() {
        assertActiveExecution();
        assertActiveSessionAccessible();

        return execution.getActiveSession().getScope();
    }

    /**
     * Returns the last used {@link MockExternalContext}
     * 
     * As every call of {@link #startFlow()} and {@link #resumeFlow()} creates a
     * new instance of {@link MockExternalContext}, this only returns the last
     * used context, which can then be asserted as desired.
     * 
     * @return the last used {@link MockExternalContext}, or <code>null</code>
     *         if no request has been sent
     */
    public MockExternalContext getLastRequestContext() {
        return context;
    }
}
