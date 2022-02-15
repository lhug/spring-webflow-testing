package de.lhug.webflowtester.executor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.binding.message.Message;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.engine.impl.FlowExecutionImplFactory;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.View;
import org.springframework.webflow.execution.factory.StaticFlowExecutionListenerLoader;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockParameterMap;

import de.lhug.webflowtester.builder.MockFlowBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tester class for simplified WebFlow Testing
 * <p>
 * This class can be used for easy testing of Spring WebFlows. It exposes
 * necessary control mechanisms as well as a suite of convenience-methods for
 * common assertions. The passed {@link MockFlowBuilder} supplies the
 * {@link Flow} to be tested, which in turn will be built once and then kept,
 * meaning that this class does <b>not</b> support Flows stored in
 * {@link org.springframework.webflow.definition.registry.FlowDefinitionHolder}s.
 * A typical test case, given a simple flow like so
 *
 * <pre>
 * &lt;view-state id=&quot;start&quot;&gt;
 *   &lt;transition on=&quot;next&quot; to=&quot;end&quot; /&gt;
 * &lt;/view-state&gt;
 * &lt;end-state id=&quot;end&quot; /&gt;
 * </pre>
 * <p>
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
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MockFlowTester {

	/**
	 * Builds an instance of this using a {@link MockFlowBuilder}.
	 * <p>
	 * The {@link MockFlowBuilder#buildFlow()} method is called before the instance
	 * is constructed, meaning the passed builder can be replaced with a lambda
	 * directly supplying a fully constructed {@link Flow} instance.
	 *
	 * @param builder an implementation of {@link MockFlowBuilder} to supply the
	 *                {@link Flow} instance
	 * @return an initialized MockFlowTester
	 * @throws NullPointerException when the builder is null
	 */
	public static MockFlowTester from(MockFlowBuilder builder) {
		return new MockFlowTester(builder.buildFlow());
	}

	private final FlowExecutionImplFactory executionFactory = new FlowExecutionImplFactory();
	private final Flow testFlow;
	private final MessageContextStoringFlowExecutionListener listener = new MessageContextStoringFlowExecutionListener();

	private FlowExecutionImpl execution;
	private MockExternalContext context;
	private String eventId;
	private Object request;

	/**
	 * Returns the current Flow Execution.
	 * <p>
	 * This does not do any state-checks but merely returns the currently used
	 * {@link FlowExecution} instance.
	 *
	 * @return the current {@link FlowExecution}, or <code>null</code> if no
	 * execution is present
	 */
	public FlowExecution getCurrentFlowExecution() {
		return execution;
	}

	/**
	 * Starts the flow.
	 * <p>
	 * Discards the previous {@link FlowExecution} if present, and all other passed
	 * information, and creates a fresh instance. This does not generate any
	 * warnings.
	 * <p>
	 * Methods on requiring an active session can be called after this.
	 *
	 * @see #getScope()
	 * @see #getCurrentStateId()
	 * @see #resumeFlow()
	 */
	public void startFlow() {
		startFlow(Collections.emptyMap());
	}

	/**
	 * Starts the flow with the given input arguments.
	 * <p>
	 * Discards the previous {@link FlowExecution} if present, and all other passed
	 * information, and creates a fresh instance, directly passing the input
	 * arguments. This does not generate any warnings.
	 * <p>
	 * Methods on requiring an active session can be called after this.
	 *
	 * @param inputArguments a {@link Map} containing the Attributes to be passed to
	 *                       the {@link Flow}, not <code>null</code>
	 * @see #getScope()
	 * @see #getCurrentStateId()
	 * @see #resumeFlow()
	 */
	public <V> void startFlow(Map<String, V> inputArguments) {
		initFlowExecution();
		newContext();
		eventId = null;
		execution.start(new LocalAttributeMap<>(inputArguments), context);
	}

	private void newContext() {
		context = new MockExternalContext();
		if (request != null) {
			context.setNativeRequest(request);
		}
	}

	private void initFlowExecution() {
		registerFlowExecutionListener();
		execution = (FlowExecutionImpl) executionFactory.createFlowExecution(testFlow);
	}

	private void registerFlowExecutionListener() {
		executionFactory
				.setExecutionListenerLoader(new StaticFlowExecutionListenerLoader(listener));
	}

	/**
	 * Starts the flow execution at the given state id
	 * <p>
	 * This creates a new {@link FlowExecution}, discarding any previous Executions,
	 * and sets the current state to the passed {@code stateId}. The resulting state
	 * is as if the flow had just entered the given state, meaning that the declared
	 * {@code &lt;on-entry&gt;} directives are considered to be finished. To make this
	 * obvious: this does <b>not</b> actually call the entry-actions. This is used
	 * to avoid having to run through the entire flow and test states in isolation.
	 * After this has been called, the current flow is active.
	 *
	 * @param stateId the state to enter
	 */
	public void startFlowAt(String stateId) {
		initFlowExecution();
		execution.setCurrentState(stateId);
	}

	/**
	 * Continues the active flow execution.
	 * <p>
	 * Before this is called, an {@link #setEventId(String) event} must be set to
	 * allow continuation of the flow. The event is then used to determine the next
	 * transition of the flow. Every call of this method is being treated as a new
	 * Request, meaning that any previously given Request parameters are discarded.
	 *
	 * @throws IllegalStateException when no event id is set or no flow execution is
	 *                               available
	 * @see FlowExecution#resume(ExternalContext)
	 * @see #setEventId(String)
	 */
	public void resumeFlow() {
		resumeFlow(Collections.emptyMap());
	}

	/**
	 * Continues the active flow execution with the given request parameters.
	 * <p>
	 * Before this is called, an {@link #setEventId(String) event} must be set to
	 * allow continuation of the flow. The event is then used to determine the next
	 * transition of the flow.
	 * <p>
	 * Every call of this method is being treated as a new Request, meaning that any
	 * previously given Request parameters are discarded.
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
	 * @param inputArguments a {@link Map} containing the current RequestParameters
	 * @throws IllegalStateException when no event id is set or no flow execution is
	 *                               available
	 * @see FlowExecution#resume(ExternalContext)
	 * @see #setEventId(String)
	 */
	public void resumeFlow(Map<? extends String, ?> inputArguments) {
		assertActiveExecution();
		Assert.state(eventId != null, "An event ID must be set to resume the flow");
		newContext();
		setRequestParameters(inputArguments);
		context.setEventId(eventId);
		execution.resume(context);
	}

	private void setRequestParameters(Map<? extends String, ?> inputArguments) {
		MockParameterMap parameterMap = new MockParameterMap();
		inputArguments.forEach((key, value) -> {
			if (value instanceof String[]) {
				parameterMap.put(key, (String[]) value);
			} else if (value instanceof MultipartFile) {
				parameterMap.put(key, (MultipartFile) value);
			} else {
				parameterMap.put(key, Objects.toString(value));
			}
		});
		context.setRequestParameterMap(parameterMap);
	}

	/**
	 * Sets the event to be called on the next resume operation
	 *
	 * @param eventId the next event id
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
	 * @throws IllegalStateException if no active {@link FlowExecution} is present
	 * @deprecated junit dependency will be removed. prefer {@link #executionHasEnded()}
	 */
	@Deprecated(since = "1.4", forRemoval = true)
	public void assertFlowExecutionEnded() {
		assertTrue(executionHasEnded());
	}

	/**
	 * Returns if the current execution has ended, and the flow is stopped.
	 *
	 * @return true if the flow ended, false if the flow is active
	 * @throws IllegalStateException if no active {@link FlowExecution} is present
	 */
	public boolean executionHasEnded() {
		assertActiveExecution();

		return execution.hasEnded();
	}

	private void assertActiveExecution() {
		Assert.state(execution != null, "Flow must be started before assertions can be made.");
	}

	/**
	 * Asserts that the flow returned with the given outcome, meaning the given
	 * endState Id.
	 * <p>
	 * Will raise an {@link AssertionError} if the outcome id was not as expected.
	 *
	 * @param expectedOutcome the expected flow execution outcome
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has not ended
	 * @deprecated junit dependency will be removed. prefer {@link #getFlowOutcome()}
	 */
	@Deprecated(since = "1.4", forRemoval = true)
	public void assertFlowOutcomeIs(String expectedOutcome) {
		assertEquals(expectedOutcome, getFlowOutcome());
	}

	/**
	 * Returns the flow outcome, meaning the given end state id.
	 *
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has not ended
	 */
	public String getFlowOutcome() {
		assertActiveExecution();
		assertOutcomeAccessible();

		return execution.getOutcome().getId();
	}

	private void assertOutcomeAccessible() {
		Assert.state(execution.hasEnded(), "Flow Execution must have ended to assert the outcome");
	}

	/**
	 * Returns any given output attributes
	 *
	 * @return an {@link AttributeMap} containing all output attributes of the
	 * current flow.
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has not ended
	 */
	public AttributeMap<Object> getOutputAttributes() {
		assertActiveExecution();
		assertOutcomeAccessible();

		return execution.getOutcome().getOutput();
	}

	/**
	 * Asserts that an external redirect to the passed url was rendered.
	 * <p>
	 * Raises an {@link AssertionError} if the actual url differs from the passed
	 * url.
	 *
	 * @param url The URL that should have been redirected to
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has not ended
	 * @deprecated junit dependency will be removed. prefer {@link #getExternalRedirectUrl()}
	 */
	@Deprecated(since = "1.4", forRemoval = true)
	public void assertExternalRedirectTo(String url) {
		assertEquals(url, getExternalRedirectUrl());
	}

	/**
	 * Returns the rendered external redirect url.
	 *
	 * @return String the external redirect url, or {@code null} if no redirect was rendered
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has not ended
	 */
	public String getExternalRedirectUrl() {
		assertActiveExecution();
		assertOutcomeAccessible();

		return context.getExternalRedirectUrl();
	}

	/**
	 * Asserts the current state id.
	 * <p>
	 * Raises an {@link AssertionError} if the current state id differs from the
	 * passed stateId.
	 *
	 * @param stateId the expected id of the current state
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has ended
	 * @deprecated junit dependency will be removed. prefer {@link #getCurrentStateId()}
	 */
	@Deprecated(since = "1.4", forRemoval = true)
	public void assertCurrentStateIs(String stateId) {
		assertEquals(stateId, getCurrentStateId());
	}

	/**
	 * Returns the current state id
	 *
	 * @return String the id of the current state
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has ended
	 */
	public String getCurrentStateId() {
		assertActiveExecution();
		assertActiveSessionAccessible();

		return execution.getActiveSession().getState().getId();
	}

	private void assertActiveSessionAccessible() {
		Assert.state(execution.isActive(), "Flow Execution must be active to assert current events");
	}

	/**
	 * Returns the Flow Scope Attributes of the current flow.
	 *
	 * @return the {@link AttributeMap} containing the current Flow Scope
	 * @throws IllegalStateException if no {@link FlowExecution} is present or the
	 *                               current flow execution has ended
	 */
	public AttributeMap<Object> getScope() {
		assertActiveExecution();
		assertActiveSessionAccessible();

		return execution.getActiveSession().getScope();
	}

	/**
	 * Returns the last used {@link MockExternalContext}
	 * <p>
	 * As every call of {@link #startFlow()} and {@link #resumeFlow()} creates a new
	 * instance of {@link MockExternalContext}, this only returns the last used
	 * context, which can then be asserted as desired.
	 *
	 * @return the last used {@link MockExternalContext}, or <code>null</code> if no
	 * request has been sent
	 */
	public MockExternalContext getLastRequestContext() {
		return context;
	}

	public Set<Message> getAllMessages() {
		assertActiveExecution();
		return listener.messages;
	}

	private static class MessageContextStoringFlowExecutionListener implements FlowExecutionListener {
		public Set<Message> messages = new HashSet<>();

		@Override
		public void viewRendering(RequestContext context, View view, StateDefinition viewState) {
			Message[] allMessages = context.getMessageContext().getAllMessages();
			messages = new HashSet<>(Arrays.asList(allMessages));
		}
	}

	/**
	 * Sets the request object to hold during flow execution
	 * <p>
	 * If this is not set, the {@link ExternalContext} used during execution
	 * will only contain an empty Object.
	 * Setting the request object here ensures that every {@code ExternalContext}
	 * holds this request object.
	 * <p>
	 * Passing null to this method unsets the held object and the {@code ExternalContext}
	 * reverts to using an empty Object.
	 *
	 * @param request the request object to hold during flow execution
	 */
	public void setRequest(Object request) {
		this.request = request;
	}
}
