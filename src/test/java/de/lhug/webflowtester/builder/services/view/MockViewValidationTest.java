package de.lhug.webflowtester.builder.services.view;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.lhug.webflowtester.builder.MessageContainer.Messages;
import de.lhug.webflowtester.builder.XMLMockFlowBuilder;
import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;
import de.lhug.webflowtester.executor.MockFlowTester;
import de.lhug.webflowtester.helper.BeanModelValidator;

public class MockViewValidationTest {

    private MockFlowTester tester;

    @Before
    public void setUp() {
        tester = MockFlowTester
                .from(new XMLMockFlowBuilder(new XMLMockFlowConfiguration("/eventFlows/validationFlow.xml"))
                        .withContext(flowContext()));
    }

    private FlowTestContext flowContext() {
        FlowTestContext ctx = new FlowTestContext(new BeanModelValidator());
        Messages msgs = ctx.getMessages(Locale.getDefault());
        msgs.addMessage("amount.tooLow", "too low");
        msgs.addMessage("amount.tooHigh", "too high");
        return ctx;
    }

    @Test
    public void shouldNotValidateIfValidationIsDisabled() throws Exception {
        tester.startFlow();
        tester.setEventId("doNotValidate");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("amount", "-1");

        tester.resumeFlow(parameters);
        Object result = tester.getOutputAttributes().get("beanModel");

        assertThat(result, hasProperty("amount", is(-1)));
    }

    @Test
    public void shouldValidateIfValidationIsEnabled() throws Exception {
        tester.startFlow();
        tester.setEventId("doValidate");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("amount", "-1");

        tester.resumeFlow(parameters);

        assertThat(tester.getAllMessages(), contains(hasProperty("text", is("too low"))));
    }

    @Test
    public void shouldValidateIfValidationIsNotSet() throws Exception {
        tester.startFlow();
        tester.setEventId("decideOnValidation");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("amount", "101");

        tester.resumeFlow(parameters);

        assertThat(tester.getAllMessages(), contains(hasProperty("text", is("too high"))));
    }
}
