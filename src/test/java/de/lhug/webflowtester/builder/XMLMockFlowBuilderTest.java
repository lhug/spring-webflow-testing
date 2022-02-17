package de.lhug.webflowtester.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.stub.StubFlow;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.definition.registry.NoSuchFlowDefinitionException;
import org.springframework.webflow.test.MockRequestControlContext;

class XMLMockFlowBuilderTest {

	private XMLMockFlowConfiguration configuration;
	private FlowTestContext context;

	private XMLMockFlowBuilder sut;

	@BeforeEach
	void setUp() {
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
	void shouldBuildFlowFromConfiguration() {
		var result = sut.buildFlow();

		assertThat(result.getId()).isEqualTo("standaloneFlow");
		assertThat(result.getStateIds()).containsExactly("start", "step", "bye");
	}

	@Test
	void shouldBuildFlowFromConfigurationAndContext() {
		configuration = new XMLMockFlowConfiguration("/simpleFlows/flowWithDependentBeans.xml");
		sut = new XMLMockFlowBuilder(configuration);

		var result = sut.withContext(context).buildFlow();

		assertThat(result.getId()).isEqualTo("flowWithDependentBeans");
		assertThat(result.getApplicationContext().containsBean("someBean")).isTrue();
	}

	@Test
	void shouldOnlyBuildFlowOnce() {
		assertThat(sut.buildFlow()).isSameAs(sut.buildFlow());
	}

	@Test
	void shouldBuildFlowWithParent() {
		configuration = new XMLMockFlowConfiguration("/inheritanceFlows/childFlow.xml");
		configuration.addParentFlow("/inheritanceFlows/parentFlow.xml");
		sut = new XMLMockFlowBuilder(configuration);

		var result = sut.buildFlow();

		assertThat(result.getId()).isEqualTo("childFlow");
		assertThat(result.getStateIds()).containsExactly("child-entry", "end", "motherKnowsBest");
	}

	@Test
	void shouldBeUnableToEnterSubflowStateWhenNoSubflowIsRegistered() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		sut = new XMLMockFlowBuilder(configuration);

		var flow = sut.buildFlow();

		var result = flow.getStateInstance("step");
		var context = new MockRequestControlContext(flow);

		assertThatThrownBy(() -> result.enter(context))
				.isInstanceOf(NoSuchFlowDefinitionException.class);
	}

	@Test
	void shouldRegisterStubFlowsInBuiltContext() {
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
	void shouldRegisterPassedMessages() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		context.getMessages(Locale.getDefault()).addMessage("key", "value");
		sut = new XMLMockFlowBuilder(configuration);
		sut.withContext(context);
		var flow = sut.buildFlow();

		var result = flow.getApplicationContext().getMessage("key", null, Locale.getDefault());

		assertThat(result).isEqualTo("value");
	}

	@Test
	void shouldNotAddMessagesWhenContextIsNotSet() {
		configuration = new XMLMockFlowConfiguration("/subFlows/flow.xml");
		context.getMessages(Locale.getDefault()).addMessage("key", "value");
		sut = new XMLMockFlowBuilder(configuration);
		var applicationContext = sut.buildFlow().getApplicationContext();

		var locale = Locale.getDefault();

		assertThatThrownBy(() -> applicationContext.getMessage("key", null, locale))
				.isInstanceOf(NoSuchMessageException.class);
	}
}
