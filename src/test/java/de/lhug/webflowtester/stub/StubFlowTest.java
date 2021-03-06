package de.lhug.webflowtester.stub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionOutcome;

public class StubFlowTest {

	private StubFlow sut;

	@Before
	public void setUp() {
		sut = new StubFlow("flowId", "endStateId");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenFlowIdIsNull() {
		new StubFlow(null, "state");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenEndStateIdIsNull() {
		new StubFlow("state", null);
	}

	@Test
	public void shouldCreateStubFlowWithFlowIdAndEndStateId() {
		StubFlow result = new StubFlow("flowId", "endStateId");

		assertThat(result.getFlowDefinitionId()).isEqualTo("flowId");
		assertThat(result.getEndStateId()).isEqualTo("endStateId");
	}

	@Test
	public void shouldReturnExecutableFlow() {
		FlowDefinition result = sut.getFlowDefinition();

		runAndAssertEnd(result, null);
	}

	private <V> FlowExecutionOutcome runAndAssertEnd(FlowDefinition definition,
			MutableAttributeMap<V> input) {
		FlowExecution exec = new FlowExecutionImpl((Flow) definition);
		exec.start(input, null);

		assertThat(exec.hasEnded()).isTrue();
		return exec.getOutcome();
	}

	@Test
	public void flowShouldEmitEndStateId() {
		FlowDefinition result = sut.getFlowDefinition();

		FlowExecutionOutcome outcome = runAndAssertEnd(result, null);

		assertThat(outcome.getId()).isEqualTo("endStateId");
	}

	@Test
	public void shouldCacheFlowDefinition() {
		FlowDefinition result = sut.getFlowDefinition();

		assertThat(result).isSameAs(sut.getFlowDefinition());
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenTryingToSetNullAsEndStateId() {
		sut.setEndStateId(null);
	}

	@Test
	public void shouldEmitUpdatedFlowDefinitionWhenEndStateIdChanges() {
		FlowDefinition before = sut.getFlowDefinition();

		sut.setEndStateId("otherEndState");
		FlowDefinition after = sut.getFlowDefinition();

		assertThat(before).isNotSameAs(after);
		assertThat(before.getId()).isEqualTo(after.getId());

		assertThat(before.getPossibleOutcomes()).containsExactly("endStateId");
		assertThat(after.getPossibleOutcomes()).containsExactly("otherEndState");
	}

	@Test
	public void shouldReturnEmptyMapBeforeFlowWasExecuted() {
		AttributeMap<Object> result = sut.getInputAttributes();

		assertThat(result.asMap()).isEmpty();
	}

	@Test
	public void shouldReturnPassedInputAttributesWhenFlowWasExecutedWithInputAttributes() {
		LocalAttributeMap<String> input = new LocalAttributeMap<>("key", "input");

		runAndAssertEnd(sut.getFlowDefinition(), input);

		AttributeMap<Object> result = sut.getInputAttributes();
		assertThat(result).isEqualTo(input);
	}

	@Test
	public void shouldResetInputAttributesAfterFetching() {
		LocalAttributeMap<String> input = new LocalAttributeMap<>("key", "input");
		runAndAssertEnd(sut.getFlowDefinition(), input);
		sut.getInputAttributes();

		AttributeMap<Object> result = sut.getInputAttributes();

		assertThat(result.asMap()).isEmpty();
	}

	@Test
	public void shouldEmitOutputAttributes() {
		sut.addOutputAttribute("out", "put");

		FlowExecutionOutcome result = runAndAssertEnd(sut.getFlowDefinition(), null);

		assertThat(result.getOutput().get("out")).isEqualTo("put");
	}

	@Test
	public void shouldEmitMultipleOutputAttributes() {
		sut.addOutputAttribute("key", "value");
		byte[] other = new byte[] { 1, 1, 2, 3, 5, 8 };
		sut.addOutputAttribute("other", other);

		AttributeMap<Object> result = runAndAssertEnd(sut.getFlowDefinition(), null).getOutput();

		assertThat(result.get("key")).isEqualTo("value");
		assertThat(result.get("other")).isEqualTo(other);
	}

	@Test
	public void shouldEmitPassedOutputMap() {
		sut.setOutputAttributes(Collections.singletonMap("left", "right"));

		AttributeMap<Object> result = runAndAssertEnd(sut.getFlowDefinition(), null).getOutput();

		assertThat(result.get("left")).isEqualTo("right");
	}

	@Test
	public void shouldUpdateOutputAttributesAfterAssembly() {
		FlowDefinition definition = sut.getFlowDefinition();

		sut.addOutputAttribute("some", "value");

		FlowExecutionOutcome result = runAndAssertEnd(definition, null);
		assertThat(result.getOutput().get("some")).isEqualTo("value");
	}
}
