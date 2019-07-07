package de.lhug.webflowtester.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.definition.registry.NoSuchFlowDefinitionException;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.test.MockRequestControlContext;

import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.stub.StubFlow;
import lombok.Value;

public class XMLMockFlowBuilderTest {

    private XMLMockFlowConfiguration configuration;
    private FlowTestContext context;

    private XMLMockFlowBuilder sut;

    @Before
    public void setUp() {
        configuration = new XMLMockFlowConfiguration("/simpleFlows/standaloneFlow.xml");
        context = new FlowTestContext(new SomeBean("I am groot"));
        sut = new XMLMockFlowBuilder(configuration);
    }

    @Value
    private static class SomeBean {
        private final String message;
    }

    @Test
    public void shouldBuildFlowFromConfiguration() throws Exception {
        Flow result = sut.buildFlow();

        assertThat(result.getId(), is("standaloneFlow"));
        assertThat(result.getStateIds(), is(arrayContaining("start", "step", "bye")));
    }

    @Test
    public void shouldBuildFlowFromConfigurationAndContext() throws Exception {
        configuration = new XMLMockFlowConfiguration("/simpleFlows/flowWithDependentBeans.xml");
        sut = new XMLMockFlowBuilder(configuration);

        Flow result = sut.withContext(context).buildFlow();

        assertThat(result.getId(), is("flowWithDependentBeans"));
        assertThat(result.getApplicationContext().containsBean("someBean"), is(true));
    }

    @Test
    public void shouldOnlyBuildFlowOnce() throws Exception {
        assertThat(sut.buildFlow(),
                sameInstance(sut.buildFlow()));
    }

    @Test
    public void shouldBuildFlowWithParent() throws Exception {
        configuration = new XMLMockFlowConfiguration("/inheritanceFlows/childFlow.xml");
        configuration.addParentFlow("/inheritanceFlows/parentFlow.xml");
        sut = new XMLMockFlowBuilder(configuration);

        Flow result = sut.buildFlow();

        assertThat(result.getId(), is("childFlow"));
        assertThat(result.getStateIds(), is(arrayContainingInAnyOrder("child-entry", "end", "motherKnowsBest")));
    }

    @Test(expected = NoSuchFlowDefinitionException.class)
    public void shouldBeUnableToEnterSubflowStateWhenNoSubflowIsRegistered() throws Exception {
        configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
        sut = new XMLMockFlowBuilder(configuration);

        Flow result = sut.buildFlow();

        result.getStateInstance("step").enter(new MockRequestControlContext(result));
    }

    @Test
    public void shouldRegisterStubFlowsInBuiltContext() throws Exception {
        configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
        context.addSubFlow(new StubFlow("subFlow", "end"));
        sut = new XMLMockFlowBuilder(configuration);
        sut.withContext(context);

        Flow result = sut.buildFlow();

        result.getStateInstance("step").enter(new MockRequestControlContext(result));
    }

    @Test
    public void shouldRegisterPassedMessages() throws Exception {
        configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
        context.getMessages(Locale.getDefault()).addMessage("key", "value");
        sut = new XMLMockFlowBuilder(configuration);
        sut.withContext(context);
        Flow flow = sut.buildFlow();

        String result = flow.getApplicationContext().getMessage("key", null, Locale.getDefault());

        assertThat(result, is("value"));
    }

    @Test(expected = NoSuchMessageException.class)
    public void shouldNotAddMessagesWhenContextIsNotSet() throws Exception {
        configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
        context.getMessages(Locale.getDefault()).addMessage("key", "value");
        sut = new XMLMockFlowBuilder(configuration);
        Flow flow = sut.buildFlow();

        flow.getApplicationContext().getMessage("key", null, Locale.getDefault());
    }
}
