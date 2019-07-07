package de.lhug.webflowtester.builder.services;

import org.springframework.binding.convert.ConversionService;
import org.springframework.binding.convert.service.DefaultConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.expression.spel.WebFlowSpringELExpressionParser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestFlowBuilderServicesFactory {

    public static FlowBuilderServices getServices() {
        FlowBuilderServices services = new TestFlowBuilderServices();
        services.setViewFactoryCreator(new MockViewFactoryCreator());
        services.setConversionService(new DefaultConversionService());
        services.setApplicationContext(createTestApplicationContext());
        return services;
    }

    private static class TestFlowBuilderServices extends FlowBuilderServices {
        @Override
        public void setConversionService(ConversionService conversionService) {
            super.setConversionService(conversionService);
            setExpressionParser(new WebFlowSpringELExpressionParser(new SpelExpressionParser(), conversionService));
        }
    }

    private static ApplicationContext createTestApplicationContext() {
        StaticApplicationContext context = new StaticApplicationContext();
        context.refresh();
        return context;
    }
}
