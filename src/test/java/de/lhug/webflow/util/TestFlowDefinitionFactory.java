package de.lhug.webflow.util;

import java.net.URL;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.FlowAssembler;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.FlowBuilderContext;
import org.springframework.webflow.engine.builder.model.FlowModelFlowBuilder;
import org.springframework.webflow.engine.model.builder.DefaultFlowModelHolder;
import org.springframework.webflow.engine.model.builder.FlowModelBuilder;
import org.springframework.webflow.engine.model.builder.xml.XmlFlowModelBuilder;
import org.springframework.webflow.engine.model.registry.FlowModelHolder;
import org.springframework.webflow.engine.model.registry.FlowModelRegistry;
import org.springframework.webflow.engine.model.registry.FlowModelRegistryImpl;
import org.springframework.webflow.test.MockFlowBuilderContext;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestFlowDefinitionFactory {

	private static final FlowDefinitionResourceFactory RESOURCE_FACTORY = new FlowDefinitionResourceFactory(new DefaultResourceLoader());

	public static FlowDefinition simpleFlow() {
		return buildFlow("/exampleFlow.xml");
	}

	private static Flow buildFlow(String resourcePath) {
		FlowDefinitionResource resource = createResource(resourcePath);
		FlowBuilderContext flowBuilderContext = new MockFlowBuilderContext(resource.getId(), resource.getAttributes());
		// configureFlowBuilderContext(flowBuilderContext);
		FlowBuilder builder = createFlowBuilder(resource);
		FlowAssembler assembler = new FlowAssembler(builder, flowBuilderContext);
		return assembler.assembleFlow();
	}

	private static FlowDefinitionResource createResource(String path) {
		URL resource = TestFlowDefinitionFactory.class.getResource(path);
		return RESOURCE_FACTORY.createResource(resource.toExternalForm());
	}

	private static FlowBuilder createFlowBuilder(FlowDefinitionResource resource) {
		// registerDependentFlowModels();
		final FlowModelRegistry flowModelRegistry = new FlowModelRegistryImpl();
		FlowModelBuilder modelBuilder = new XmlFlowModelBuilder(resource.getPath(), flowModelRegistry);
		FlowModelHolder modelHolder = new DefaultFlowModelHolder(modelBuilder);
		flowModelRegistry.registerFlowModel(resource.getId(), modelHolder);
		return new FlowModelFlowBuilder(modelHolder) {
			@Override
			protected void registerFlowBeans(ConfigurableBeanFactory flowBeanFactory) {
				// registerMockFlowBeans(flowBeanFactory);
			}
		};
	}
}
