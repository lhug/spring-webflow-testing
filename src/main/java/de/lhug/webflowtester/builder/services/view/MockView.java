package de.lhug.webflowtester.builder.services.view;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

import org.springframework.binding.expression.EvaluationException;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.expression.ExpressionParser;
import org.springframework.binding.expression.support.FluentParserContext;
import org.springframework.binding.mapping.MappingResult;
import org.springframework.binding.mapping.MappingResults;
import org.springframework.binding.mapping.impl.DefaultMapper;
import org.springframework.binding.mapping.impl.DefaultMapping;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.message.MessageResolver;
import org.springframework.core.style.ToStringCreator;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.webflow.definition.TransitionDefinition;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.View;
import org.springframework.webflow.validation.ValidationHelper;
import org.springframework.webflow.validation.WebFlowMessageCodesResolver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * A Mock view implementation that performs data binding and validation
 * and holds the name of the view to be rendered. Useful for asserting correct view state
 * behavior prior to rendering.
 */
@RequiredArgsConstructor
@Log
public class MockView implements View {

	@Getter
	private final String viewId;
	private final RequestContext context;
	private final MessageCodesResolver messageCodesResolver = new WebFlowMessageCodesResolver();
	@Setter
	private Validator validator;
	@Setter
	private ExpressionParser expressionParser;
	private boolean userEventProcessed = false;

	@Override
	public void render() throws IOException {
		context.getExternalContext().getResponseWriter().write(viewId);
	}

	@Override
	public boolean userEventQueued() {
		return context.getRequestParameters().contains("_eventId");
	}

	@Override
	public void processUserEvent() {
		String eventId = getEventId();
		if (eventId == null) {
			return;
		}
		Object model = getModelObject();
		if (model != null) {
			TransitionDefinition transition = context.getMatchingTransition(eventId);
			processBinding(model, transition);
		}
		userEventProcessed = true;
	}

	private void processBinding(Object model, TransitionDefinition transition) {
		if (shouldBind(transition)) {
			MappingResults results = bind(model);
			List<?> bindingErrors = extractBindingErrors(results);
			if (!bindingErrors.isEmpty()) {
				addErrorMessages(bindingErrors);
			}
			processValidation(model, transition, results);
		}
	}

	private boolean shouldBind(TransitionDefinition transition) {
		return transition == null || transition.getAttributes().getBoolean("bind", Boolean.TRUE);
	}

	private MappingResults bind(Object model) {
		ParameterMap requestParameters = context.getRequestParameters();
		DefaultMapper mapper = createDefaultMapper(model.getClass(), requestParameters);
		return mapper.map(requestParameters, model);
	}

	private List<?> extractBindingErrors(MappingResults results) {
		return results.getResults(this::isBindingError);
	}

	private boolean isBindingError(MappingResult result) {
		return result.isError() && !Objects.equals("propertyNotFound", result.getCode());
	}

	private void addErrorMessages(List<?> mappingErrors) {
		MessageContext messageContext = context.getMessageContext();
		mappingErrors.stream()
				.map(this::buildErrorMessage)
				.forEach(messageContext::addMessage);
	}

	private DefaultMapper createDefaultMapper(Class<?> modelClass, ParameterMap requestParameters) {
		Set<String> parameterNames = requestParameters.asMap().keySet();
		DefaultMapper mapper = new DefaultMapper();

		for (String parameterName : parameterNames) {
			DefaultMapping mapping = createMapping(modelClass, parameterName);
			mapper.addMapping(mapping);
		}

		return mapper;
	}

	private DefaultMapping createMapping(Class<?> modelClass, String parameterName) {
		FluentParserContext parserContext = new FluentParserContext().evaluate(modelClass);
		Expression targetExpression = expressionParser.parseExpression(parameterName, parserContext);
		return new DefaultMapping(new RequestParameterExpression(parameterName), targetExpression);
	}

	private String getEventId() {
		return context.getRequestParameters().get("_eventId");
	}

	private Object getModelObject() {
		Expression modelExpression = getModelExpression();
		if (modelExpression != null) {
			try {
				return modelExpression.getValue(context);
			} catch (EvaluationException e) {
				log.log(Level.WARNING, "Expression {} could not be evaluated. Is the requested Object accessible from the view?", modelExpression.getExpressionString());
			}
		}
		return null;
	}

	private Expression getModelExpression() {
		return (Expression) context.getCurrentState().getAttributes().get("model");
	}

	private MessageResolver buildErrorMessage(Object offer) {
		MappingResult error = (MappingResult) offer;
		String field = error.getMapping().getTargetExpression().getExpressionString();
		return new MessageBuilder().error().defaultText(error.getCode() + " on " + field).build();
	}

	private void processValidation(Object model, TransitionDefinition transition,
			MappingResults results) {
		if (shouldValidate(transition)) {
			validate(model, results);
		}
	}

	private boolean shouldValidate(TransitionDefinition transition) {
		Boolean validationAttribute = getValidationAttribute(transition);
		if (validationAttribute != null) {
			return validationAttribute;
		}
		return true;
	}

	private Boolean getValidationAttribute(TransitionDefinition transition) {
		return transition != null
				? transition.getAttributes().getBoolean("validate")
				: null;
	}

	private void validate(Object model, MappingResults mappingResults) {
		ValidationHelper helper = new ValidationHelper(model, context, getEventId(),
				getModelExpression().getExpressionString(),
				expressionParser, messageCodesResolver, mappingResults);
		helper.setValidator(validator);
		helper.validate();
	}

	@Override
	public Serializable getUserEventState() {
		return null;
	}

	@Override
	public boolean hasFlowEvent() {
		return userEventProcessed && !context.getMessageContext().hasErrorMessages();
	}

	@Override
	public Event getFlowEvent() {
		return new Event(this, getEventId());
	}

	@Override
	public void saveState() {
		// intentionally left blank
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("viewId", viewId).toString();
	}

	@RequiredArgsConstructor
	private static class RequestParameterExpression implements Expression {

		private final String parameterName;

		@Override
		public String getExpressionString() {
			return parameterName;
		}

		@Override
		public Object getValue(Object context) {
			ParameterMap parameters = (ParameterMap) context;
			return parameters.asMap().get(parameterName);
		}

		@Override
		public Class<?> getValueType(Object context) {
			return String.class;
		}

		@Override
		public void setValue(Object context, Object value) {
			throw new UnsupportedOperationException("Setting request parameters is not allowed");
		}

		@Override
		public String toString() {
			return "parameter:'" + parameterName + "'";
		}
	}

}
