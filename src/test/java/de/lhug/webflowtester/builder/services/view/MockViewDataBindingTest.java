package de.lhug.webflowtester.builder.services.view;

import static org.assertj.core.api.Assertions.assertThat;

import de.lhug.webflowtester.builder.XMLMockFlowBuilder;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.executor.MockFlowTester;
import de.lhug.webflowtester.helper.BeanModel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.message.Message;
import org.springframework.webflow.core.collection.MutableAttributeMap;

class MockViewDataBindingTest {

	private MockFlowTester tester;

	@BeforeEach
	void setUp() {
		tester = MockFlowTester
				.from(new XMLMockFlowBuilder(
						new XMLMockFlowConfiguration("/eventFlows/modelExpressionFlow.xml")));
	}

	@Test
	void shouldBindEmptyBeanModelAsModelAttributeOfView() {
		tester.startFlow();

		assertThat(tester.getCurrentStateId()).isEqualTo("start");
		var currentModel = getCurrentModelObject();
		assertThat(currentModel).isEqualTo(new BeanModel());
	}

	private Object getCurrentModelObject() {
		var currentState = tester.getCurrentFlowExecution().getActiveSession().getState();
		Expression modelExpression = (Expression) currentState.getAttributes().get("model");
		return modelExpression.getValue(tester.getScope());
	}

	@Test
	void shouldNotBindValuesWhenNoParametersAreConfigured() {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		var model = createBeanModel();
		addBeanModelToFlowScope(model);

		tester.resumeFlow();

		assertThat(model.getAmount()).isEqualTo(2);
		assertThat(model.getName()).isEqualTo("nope");
		assertThat(model.getEntries()).isEmpty();
	}

	private BeanModel createBeanModel() {
		BeanModel model = new BeanModel();
		model.setAmount(2);
		model.setName("nope");
		model.setEntries(Collections.emptyList());
		return model;
	}

	private void addBeanModelToFlowScope(BeanModel beanModel) {
		MutableAttributeMap<Object> flowScope = (MutableAttributeMap<Object>) tester.getScope();
		flowScope.put("beanModel", beanModel);
	}

	@Test
	void shouldLogErrorWhenModelExpressionCannotBeEvaluated() {
		var handler = addHandler();
		tester.startFlowAt("start");
		tester.setEventId("continue");

		tester.resumeFlow();

		assertThat(handler.logs)
				.singleElement()
				.extracting(LogRecord::getLevel, this::message)
				.containsExactly(
						Level.WARNING,
						"Expression [beanModel] could not be evaluated. Is the requested Object accessible from the view?");
	}

	private RecordingHandler addHandler() {
		var logger = Logger.getLogger(MockView.class.getName());
		var handler = new RecordingHandler(Level.WARNING);
		logger.setUseParentHandlers(false);
		logger.addHandler(handler);
		return handler;
	}

	private String message(LogRecord record) {
		return MessageFormat.format(
				record.getMessage(),
				record.getParameters()
		);
	}

	@Test
	void shouldBindAttributesFromRequestParametersToModelObject() {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addBeanModelToFlowScope(model);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", "Bean Model");
		parameters.put("amount", "99");
		parameters.put("entries", new String[]{"one", "two", "four"});

		tester.resumeFlow(parameters);

		assertThat(model.getAmount()).isEqualTo(99);
		assertThat(model.getName()).isEqualTo("Bean Model");
		assertThat(model.getEntries()).containsExactly("one", "two", "four");
	}

	@Test
	void shouldAddErrorMessageToMessageContextOnBindingError() {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addBeanModelToFlowScope(model);

		var parameters = Map.ofEntries(
				Map.entry("name", "Bean Model"),
				Map.entry("amount", "ninety-nine"),
				Map.entry("entries", new String[]{"one", "two", "four"})
		);

		tester.resumeFlow(parameters);

		assertThat(model.getName()).isEqualTo("Bean Model");
		assertThat(model.getEntries()).containsExactly("one", "two", "four");
		assertThat(model.getAmount()).isEqualTo(2);

		var messages = tester.getAllMessages();

		assertThat(messages)
				.singleElement()
				.extracting(Message::getText)
				.isEqualTo("typeMismatch on amount");
	}

	@Test
	void shouldNotLeaveStateOnBindingErrors() {
		tester.startFlowAt("start");
		tester.setEventId("continue");
		BeanModel model = createBeanModel();
		addBeanModelToFlowScope(model);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", "Bean Model");
		parameters.put("amount", "ninety-nine");
		parameters.put("entries", new String[]{"one", "two", "four"});

		tester.resumeFlow(parameters);

		assertThat(tester.getCurrentStateId()).isEqualTo("start");
	}

	static class RecordingHandler extends Handler {

		final Level level;

		RecordingHandler(Level level) {
			this.level = level;
			setLevel(level);
		}

		final List<LogRecord> logs = new ArrayList<>();
		@Override
		public void publish(LogRecord record) {
			logs.add(record);
		}

		@Override
		public void flush() {
			//intentionally left blank
		}

		@Override
		public void close() throws SecurityException {
			// intentionally left blank
		}
	}
}
