package de.lhug.webflow.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.Conventions;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.FlowAssembler;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.model.FlowModelFlowBuilder;
import org.springframework.webflow.engine.model.builder.DefaultFlowModelHolder;
import org.springframework.webflow.engine.model.builder.FlowModelBuilder;
import org.springframework.webflow.engine.model.builder.xml.XmlFlowModelBuilder;
import org.springframework.webflow.engine.model.registry.FlowModelHolder;
import org.springframework.webflow.engine.model.registry.FlowModelRegistry;
import org.springframework.webflow.engine.model.registry.FlowModelRegistryImpl;
import org.springframework.webflow.test.MockFlowBuilderContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlowDefinitionBuilder {

	private final FlowDefinitionResource flowDefinitionResource;
	private final FlowModelRegistry flowModelRegistry = new FlowModelRegistryImpl();

	private final List<FlowDefinitionResource> dependentFlowModels = new ArrayList<>();
	private final Map<String, Object> dependentBeans = new HashMap<>();

	List<FlowDefinitionResource> getDependentFlowModels() {
		return Collections.unmodifiableList(dependentFlowModels);
	}

	Map<String, Object> getDependentBeans() {
		return Collections.unmodifiableMap(dependentBeans);
	}

	public FlowDefinition build() {
		return buildFlow();
	}

	private Flow buildFlow() {
		MockFlowBuilderContext flowBuilderContext = new MockFlowBuilderContext(flowDefinitionResource.getId(),
				flowDefinitionResource.getAttributes());
		configureFlowBuilderContext(flowBuilderContext);
		FlowBuilder builder = createFlowBuilder(flowDefinitionResource);
		FlowAssembler assembler = new FlowAssembler(builder, flowBuilderContext);
		return assembler.assembleFlow();
	}

	private void configureFlowBuilderContext(MockFlowBuilderContext context) {
		dependentBeans.entrySet().forEach(entry -> context.registerBean(entry.getKey(), entry.getValue()));
	}

	private FlowBuilder createFlowBuilder(FlowDefinitionResource resource) {
		registerDependentFlowModels();
		FlowModelBuilder modelBuilder = new XmlFlowModelBuilder(resource.getPath(), flowModelRegistry);
		FlowModelHolder modelHolder = new DefaultFlowModelHolder(modelBuilder);
		flowModelRegistry.registerFlowModel(resource.getId(), modelHolder);
		return new FlowModelFlowBuilder(modelHolder);
	}

	private void registerDependentFlowModels() {
		dependentFlowModels.forEach(this::registerFlowModel);
	}

	private void registerFlowModel(FlowDefinitionResource resource) {
		FlowModelBuilder modelBuilder = new XmlFlowModelBuilder(resource.getPath(), flowModelRegistry);
		flowModelRegistry.registerFlowModel(resource.getId(), new DefaultFlowModelHolder(modelBuilder));
	}

	public FlowDefinitionBuilder addParent(FlowDefinitionResource parent) {
		dependentFlowModels.add(parent);
		return this;
	}

	public FlowDefinitionBuilder addBean(String beanName, Object bean) {
		dependentBeans.put(beanName, bean);
		return this;
	}

	public FlowDefinitionBuilder addBean(Object bean) {
		String name = Conventions.getVariableName(bean);
		return addBean(name, bean);
	}
}
