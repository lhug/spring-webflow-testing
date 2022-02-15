package de.lhug.webflowtester.builder;

import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.stub.StubFlow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.definition.registry.NoSuchFlowDefinitionException;
import org.springframework.webflow.test.MockRequestControlContext;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	@AllArgsConstructor
	@Getter
	private static class SomeBean {
		private final String message;
	}

	@Test
	public void shouldBuildFlowFromConfiguration() {
		var result = sut.buildFlow();

		assertThat(result.getId()).isEqualTo("standaloneFlow");
		assertThat(result.getStateIds()).containsExactly("start", "step", "bye");
	}

	@Test
	public void shouldBuildFlowFromConfigurationAndContext() {
		configuration = new XMLMockFlowConfiguration("/simpleFlows/flowWithDependentBeans.xml");
		sut = new XMLMockFlowBuilder(configuration);

		var result = sut.withContext(context).buildFlow();

		assertThat(result.getId()).isEqualTo("flowWithDependentBeans");
		assertThat(result.getApplicationContext().containsBean("someBean")).isTrue();
	}

	@Test
	public void shouldOnlyBuildFlowOnce() {
		assertThat(sut.buildFlow()).isSameAs(sut.buildFlow());
	}

	@Test
	public void shouldBuildFlowWithParent() {
		configuration = new XMLMockFlowConfiguration("/inheritanceFlows/childFlow.xml");
		configuration.addParentFlow("/inheritanceFlows/parentFlow.xml");
		sut = new XMLMockFlowBuilder(configuration);

		var result = sut.buildFlow();

		assertThat(result.getId()).isEqualTo("childFlow");
		assertThat(result.getStateIds()).containsExactly("child-entry", "end", "motherKnowsBest");
	}

	@Test
	public void shouldBeUnableToEnterSubflowStateWhenNoSubflowIsRegistered() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		sut = new XMLMockFlowBuilder(configuration);

		var flow = sut.buildFlow();

		var result = flow.getStateInstance("step");
		var context = new MockRequestControlContext(flow);

		assertThatThrownBy(() -> result.enter(context))
				.isInstanceOf(NoSuchFlowDefinitionException.class);
	}

	@Test
	public void shouldRegisterStubFlowsInBuiltContext() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		context.addSubFlow(new StubFlow("subFlow", "end"));
		sut = new XMLMockFlowBuilder(configuration);
		sut.withContext(context);

		var flow = sut.buildFlow();

		var result = flow.getStateInstance("step");
		var context = new MockRequestControlContext(flow);

		assertThatCode(() -> result.enter(context))
				.doesNotThrowAnyException();
	}

	@Test
	public void shouldRegisterPassedMessages() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		context.getMessages(Locale.getDefault()).addMessage("key", "value");
		sut = new XMLMockFlowBuilder(configuration);
		sut.withContext(context);
		var flow = sut.buildFlow();

		var result = flow.getApplicationContext().getMessage("key", null, Locale.getDefault());

		assertThat(result).isEqualTo("value");
	}

	@Test
	public void shouldNotAddMessagesWhenContextIsNotSet() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		context.getMessages(Locale.getDefault()).addMessage("key", "value");
		sut = new XMLMockFlowBuilder(configuration);
		var applicationContext = sut.buildFlow().getApplicationContext();

		assertThatThrownBy(() -> applicationContext.getMessage("key", null, Locale.getDefault()))
				.isInstanceOf(NoSuchMessageException.class);
	}
}
