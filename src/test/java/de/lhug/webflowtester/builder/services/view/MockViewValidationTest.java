package de.lhug.webflowtester.builder.services.view;

import static org.assertj.core.api.Assertions.assertThat;

import de.lhug.webflowtester.builder.XMLMockFlowBuilder;
import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.executor.MockFlowTester;
import de.lhug.webflowtester.helper.BeanModelValidator;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockViewValidationTest {

	private MockFlowTester tester;

	@BeforeEach
	void setUp() {
		tester = MockFlowTester
				.from(new XMLMockFlowBuilder(new XMLMockFlowConfiguration("/eventFlows/validationFlow.xml"))
						.withContext(flowContext()));
	}

	private FlowTestContext flowContext() {
		var ctx = new FlowTestContext(new BeanModelValidator());
		var messages = ctx.getMessages(Locale.getDefault());
		messages.addMessage("amount.tooLow", "too low");
		messages.addMessage("amount.tooHigh", "too high");
		return ctx;
	}

	@Test
	void shouldNotValidateIfValidationIsDisabled() {
		tester.startFlow();
		tester.setEventId("doNotValidate");

		var parameters = Map.of("amount", "-1");

		tester.resumeFlow(parameters);
		var result = tester.getOutputAttributes().get("beanModel");

		assertThat(result).hasFieldOrPropertyWithValue("amount", -1);
	}

	@Test
	void shouldValidateIfValidationIsEnabled() {
		tester.startFlow();
		tester.setEventId("doValidate");

		var parameters = Map.of("amount", "-1");

		tester.resumeFlow(parameters);

		assertThat(tester.getAllMessages())
				.singleElement()
				.hasFieldOrPropertyWithValue("text", "too low");
	}

	@Test
	void shouldValidateIfValidationIsNotSet() {
		tester.startFlow();
		tester.setEventId("decideOnValidation");

		var parameters = Map.of("amount", "101");

		tester.resumeFlow(parameters);

		assertThat(tester.getAllMessages())
				.singleElement()
				.hasFieldOrPropertyWithValue("text", "too high");
	}
}
