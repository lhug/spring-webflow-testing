package de.lhug.webflowtester.builder.services;

import org.springframework.binding.convert.ConversionService;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.expression.ExpressionParser;
import org.springframework.validation.Validator;
import org.springframework.webflow.engine.builder.BinderConfiguration;
import org.springframework.webflow.engine.builder.ViewFactoryCreator;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.View;
import org.springframework.webflow.execution.ViewFactory;

import de.lhug.webflowtester.builder.services.view.MockView;
import lombok.RequiredArgsConstructor;

/**
 * A view factory creator that returns view factories that produce Mock View
 * implementations that can be used to assert that the correct view id was
 * selected as part of a flow execution test.
 * 
 */
class MockViewFactoryCreator implements ViewFactoryCreator {

    @Override
    public ViewFactory createViewFactory(Expression viewId, ExpressionParser expressionParser,
            ConversionService conversionService, BinderConfiguration binderConfiguration, Validator validator) {
        return new MockViewFactory(viewId, expressionParser);
    }

    @Override
    public String getViewIdByConvention(String viewStateId) {
        return viewStateId;
    }

    /**
     * Returns a Mock View implementation that simply holds the evaluated view
     * identifier.
     *
     */
    @RequiredArgsConstructor
    static class MockViewFactory implements ViewFactory {
        private final Expression viewIdExpression;
        private final ExpressionParser expressionParser;

        @Override
        public View getView(RequestContext context) {
            String viewId = (String) this.viewIdExpression.getValue(context);
            MockView view = new MockView(viewId, context);
            view.setExpressionParser(expressionParser);
            return view;
        }
    }
}
