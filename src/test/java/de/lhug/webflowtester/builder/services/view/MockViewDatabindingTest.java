package de.lhug.webflowtester.builder.services.view;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.message.Message;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.StateDefinition;

import de.lhug.webflowtester.builder.XMLMockFlowBuilder;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.executor.MockFlowTester;
import de.lhug.webflowtester.helper.BeanModel;

public class MockViewDatabindingTest {

	private MockFlowTester tester;

	@Before
	public void setUp() {
		tester = MockFlowTester
				.from(new XMLMockFlowBuilder(
						new XMLMockFlowConfiguration("/eventFlows/modelExpressionFlow.xml")));
	}

	@Test
	public void shouldBindEmptyBeanModelAsModelAttributeOfView() throws Exception {
		tester.startFlow();

		tester.assertCurrentStateIs("start");
		Object currentModel = getCurrentModelObject();
		assertThat(currentModel, is(new BeanModel()));
	}

	private Object getCurrentModelObject() {
		StateDefinition currentState = tester.getCurrentFlowExecution().getActiveSession().getState();
		Expression modelExpression = (Expression) currentState.getAttributes().get("model");
		return modelExpression.getValue(tester.getScope());
	}

	@Test
	public void shouldNotBindValuesWhenNoParametersAreConfigured() throws Exception {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addToFlowScope("beanModel", model);

		tester.resumeFlow();

		assertThat(model.getAmount(), is(2));
		assertThat(model.getName(), is("nope"));
		assertThat(model.getEntries(), is(empty()));
	}

	private BeanModel createBeanModel() {
		BeanModel model = new BeanModel();
		model.setAmount(2);
		model.setName("nope");
		model.setEntries(Collections.emptyList());
		return model;
	}

	private void addToFlowScope(String key, Object value) {
		MutableAttributeMap<Object> flowScope = (MutableAttributeMap<Object>) tester.getScope();
		flowScope.put(key, value);
	}

	@Test
	public void shouldBindAttributesFromRequestParametersToModelObject() throws Exception {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addToFlowScope("beanModel", model);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", "Bean Model");
		parameters.put("amount", "99");
		parameters.put("entries", new String[] { "one", "two", "four" });

		tester.resumeFlow(parameters);

		assertThat(model.getAmount(), is(99));
		assertThat(model.getName(), is("Bean Model"));
		assertThat(model.getEntries(), contains("one", "two", "four"));
	}

	@Test
	public void shouldAddErrorMessageToMessageContextOnBindingError() throws Exception {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addToFlowScope("beanModel", model);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", "Bean Model");
		parameters.put("amount", "ninetynine");
		parameters.put("entries", new String[] { "one", "two", "four" });

		tester.resumeFlow(parameters);

		assertThat(model.getName(), is("Bean Model"));
		assertThat(model.getEntries(), contains("one", "two", "four"));
		assertThat(model.getAmount(), is(2));

		Set<Message> messages = tester.getAllMessages();
		assertThat(messages, hasSize(1));
		assertThat(messages, contains(hasProperty("text", is("typeMismatch on amount"))));
	}

	@Test
	public void shouldNotLeaveStateOnBindingErrors() throws Exception {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addToFlowScope("beanModel", model);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", "Bean Model");
		parameters.put("amount", "ninetynine");
		parameters.put("entries", new String[] { "one", "two", "four" });

		tester.resumeFlow(parameters);

		tester.assertCurrentStateIs("start");
	}
}
