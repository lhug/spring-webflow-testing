package de.lhug.webflowtester.stub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionOutcome;

class StubFlowTest {

	private StubFlow sut;

	@BeforeEach
	void setUp() {
		sut = new StubFlow("flowId", "endStateId");
	}

	@Test
	void shouldThrowExceptionWhenFlowIdIsNull() {

		assertThatThrownBy(() -> new StubFlow(null, "state"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowExceptionWhenEndStateIdIsNull() {
		assertThatThrownBy(() -> new StubFlow("state", null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldCreateStubFlowWithFlowIdAndEndStateId() {
		StubFlow result = new StubFlow("flowId", "endStateId");

		assertThat(result.getFlowDefinitionId()).isEqualTo("flowId");
		assertThat(result.getEndStateId()).isEqualTo("endStateId");
	}

	@Test
	void shouldReturnExecutableFlow() {
		FlowDefinition result = sut.getFlowDefinition();

		runAndAssertEnd(result, null);
	}

	private <V> FlowExecutionOutcome runAndAssertEnd(
			FlowDefinition definition,
			MutableAttributeMap<V> input) {
		FlowExecution exec = new FlowExecutionImpl((Flow) definition);
		exec.start(input, null);

		assertThat(exec.hasEnded()).isTrue();
		return exec.getOutcome();
	}

	@Test
	void flowShouldEmitEndStateId() {
		FlowDefinition result = sut.getFlowDefinition();

		FlowExecutionOutcome outcome = runAndAssertEnd(result, null);

		assertThat(outcome.getId()).isEqualTo("endStateId");
	}

	@Test
	void shouldCacheFlowDefinition() {
		FlowDefinition result = sut.getFlowDefinition();

		assertThat(result).isSameAs(sut.getFlowDefinition());
	}

	@Test
	void shouldThrowExceptionWhenTryingToSetNullAsEndStateId() {
		assertThatThrownBy(() -> sut.setEndStateId(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldEmitUpdatedFlowDefinitionWhenEndStateIdChanges() {
		FlowDefinition before = sut.getFlowDefinition();

		sut.setEndStateId("otherEndState");
		FlowDefinition after = sut.getFlowDefinition();

		assertThat(before).isNotSameAs(after);
		assertThat(before.getId()).isEqualTo(after.getId());

		assertThat(before.getPossibleOutcomes()).containsExactly("endStateId");
		assertThat(after.getPossibleOutcomes()).containsExactly("otherEndState");
	}

	@Test
	void shouldReturnEmptyMapBeforeFlowWasExecuted() {
		AttributeMap<Object> result = sut.getInputAttributes();

		assertThat(result.asMap()).isEmpty();
	}

	@Test
	void shouldReturnPassedInputAttributesWhenFlowWasExecutedWithInputAttributes() {
		LocalAttributeMap<String> input = new LocalAttributeMap<>("key", "input");

		runAndAssertEnd(sut.getFlowDefinition(), input);

		AttributeMap<Object> result = sut.getInputAttributes();
		assertThat(result).isEqualTo(input);
	}

	@Test
	void shouldResetInputAttributesAfterFetching() {
		LocalAttributeMap<String> input = new LocalAttributeMap<>("key", "input");
		runAndAssertEnd(sut.getFlowDefinition(), input);
		sut.getInputAttributes();

		AttributeMap<Object> result = sut.getInputAttributes();

		assertThat(result.asMap()).isEmpty();
	}

	@Test
	void shouldEmitOutputAttributes() {
		sut.addOutputAttribute("out", "put");

		FlowExecutionOutcome result = runAndAssertEnd(sut.getFlowDefinition(), null);

		assertThat(result.getOutput().get("out")).isEqualTo("put");
	}

	@Test
	void shouldEmitMultipleOutputAttributes() {
		sut.addOutputAttribute("key", "value");
		byte[] other = new byte[]{1, 1, 2, 3, 5, 8};
		sut.addOutputAttribute("other", other);

		AttributeMap<Object> result = runAndAssertEnd(sut.getFlowDefinition(), null).getOutput();

		assertThat(result.get("key")).isEqualTo("value");
		assertThat(result.get("other")).isEqualTo(other);
	}

	@Test
	void shouldEmitPassedOutputMap() {
		sut.setOutputAttributes(Collections.singletonMap("left", "right"));

		AttributeMap<Object> result = runAndAssertEnd(sut.getFlowDefinition(), null).getOutput();

		assertThat(result.get("left")).isEqualTo("right");
	}

	@Test
	void shouldUpdateOutputAttributesAfterAssembly() {
		FlowDefinition definition = sut.getFlowDefinition();

		sut.addOutputAttribute("some", "value");

		FlowExecutionOutcome result = runAndAssertEnd(definition, null);
		assertThat(result.getOutput().get("some")).isEqualTo("value");
	}
}
