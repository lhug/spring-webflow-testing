package de.lhug.webflowtester.stub;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

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
    public void shouldThrowExceptionWhenFlowIdIsNull() throws Exception {
        new StubFlow(null, "state");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenEndStateIdIsNull() throws Exception {
        new StubFlow("state", null);
    }

    @Test
    public void shouldCreateStubFlowWithFlowIdAndEndStateId() throws Exception {
        StubFlow result = new StubFlow("flowId", "endStateId");

        assertThat(result.getFlowDefinitionId(), is("flowId"));
        assertThat(result.getEndStateId(), is("endStateId"));
    }

    @Test
    public void shouldReturnExecutableFlow() throws Exception {
        FlowDefinition result = sut.getFlowDefinition();

        runAndAssertEnd(result, null);
    }

    private FlowExecutionOutcome runAndAssertEnd(FlowDefinition definition, MutableAttributeMap input) {
        FlowExecution exec = new FlowExecutionImpl((Flow) definition);
        exec.start(input, null);

        assertThat(exec.hasEnded(), is(true));
        return exec.getOutcome();
    }

    @Test
    public void flowShouldEmitEndStateId() throws Exception {
        FlowDefinition result = sut.getFlowDefinition();

        FlowExecutionOutcome outcome = runAndAssertEnd(result, null);

        assertThat(outcome.getId(), is("endStateId"));
    }

    @Test
    public void shouldCacheFlowDefinition() throws Exception {
        assertThat(sut.getFlowDefinition(),
                is(sameInstance(sut.getFlowDefinition())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenTryingToSetNullAsEndStateId() throws Exception {
        sut.setEndStateId(null);
    }

    @Test
    public void shouldEmitUpdatedFlowDefinitionWhenEndStateIdChanges() throws Exception {
        FlowDefinition before = sut.getFlowDefinition();

        sut.setEndStateId("otherEndState");
        FlowDefinition after = sut.getFlowDefinition();

        assertThat(before, is(not(sameInstance(after))));
        assertThat(before.getId(), is(after.getId()));

        assertThat(before.getPossibleOutcomes(), is(arrayContaining("endStateId")));
        assertThat(after.getPossibleOutcomes(), is(arrayContaining("otherEndState")));
    }

    @Test
    public void shouldReturnEmptyMapBeforeFlowWasExecuted() throws Exception {
        AttributeMap result = sut.getInputAttributes();

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnPassedInputAttributesWhenFlowWasExecutedWithInputAttributes() throws Exception {
        LocalAttributeMap input = new LocalAttributeMap("key", "input");

        runAndAssertEnd(sut.getFlowDefinition(), input);

        AttributeMap result = sut.getInputAttributes();
        assertThat(result, equalTo(input));
    }

    @Test
    public void shouldResetInputAttributesAfterFetching() throws Exception {
        LocalAttributeMap input = new LocalAttributeMap("key", "input");
        runAndAssertEnd(sut.getFlowDefinition(), input);
        sut.getInputAttributes();

        AttributeMap result = sut.getInputAttributes();

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void shouldEmitOutputAttributes() throws Exception {
        sut.addOutputAttribute("out", "put");

        FlowExecutionOutcome result = runAndAssertEnd(sut.getFlowDefinition(), null);

        assertThat(result.getOutput().get("out"), is("put"));
    }

    @Test
    public void shouldEmitMultipleOutputAttributes() throws Exception {
        sut.addOutputAttribute("key", "value");
        byte[] other = new byte[] { 1, 1, 2, 3, 5, 8 };
        sut.addOutputAttribute("other", other);

        AttributeMap result = runAndAssertEnd(sut.getFlowDefinition(), null).getOutput();

        assertThat(result.get("key"), is("value"));
        assertThat(result.get("other"), is(other));
    }

    @Test
    public void shouldEmitPassedOutputMap() throws Exception {
        sut.setOutputAttributes(Collections.singletonMap("left", "right"));

        AttributeMap result = runAndAssertEnd(sut.getFlowDefinition(), null).getOutput();

        assertThat(result.get("left"), is("right"));
    }

    @Test
    public void shouldUpdateOutputAttributesAfterAssembly() throws Exception {
        FlowDefinition definition = sut.getFlowDefinition();

        sut.addOutputAttribute("some", "value");

        FlowExecutionOutcome result = runAndAssertEnd(definition, null);
        assertThat(result.getOutput().get("some"), is("value"));
    }
}
