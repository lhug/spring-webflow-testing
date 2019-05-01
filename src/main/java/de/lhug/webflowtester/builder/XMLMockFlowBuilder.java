package de.lhug.webflowtester.builder;

import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.model.FlowModelFlowBuilder;
import org.springframework.webflow.engine.model.builder.DefaultFlowModelHolder;
import org.springframework.webflow.engine.model.builder.FlowModelBuilder;
import org.springframework.webflow.engine.model.builder.xml.XmlFlowModelBuilder;
import org.springframework.webflow.engine.model.registry.FlowModelHolder;
import org.springframework.webflow.engine.model.registry.FlowModelRegistry;
import org.springframework.webflow.engine.model.registry.FlowModelRegistryImpl;

import de.lhug.webflowtester.builder.configuration.XMLMockFlowConfiguration;

/**
 * Builds Flow Objects from XML Resources
 * 
 * @see ExternalizedMockFlowBuilder
 *
 */
public class XMLMockFlowBuilder extends ExternalizedMockFlowBuilder {

    private final FlowModelRegistry flowModelRegistry;

    /**
     * Constructs a Builder-Instance from a given
     * {@link XMLMockFlowConfiguration}
     * 
     * More formally, the passed configuration object is registered in the
     * builder and accessed during the construction process.
     * 
     * @param configuration
     *            the {@link XMLMockFlowConfiguration} containing links to all
     *            necessary Resources to build the
     *            {@link org.springframework.webflow.engine.Flow}
     */
    public XMLMockFlowBuilder(XMLMockFlowConfiguration configuration) {
        super(configuration);
        flowModelRegistry = new FlowModelRegistryImpl();
    }

    /**
     * Creates the {@link FlowBuilder} which is used to construct the
     * {@link org.springframework.webflow.engine.model.FlowModel}.
     * 
     * The process follows three steps:
     * <ol>
     * <li>build and register the parent flow models as defined in the
     * {@link XMLMockFlowConfiguration}</li>
     * <li>build the {@link FlowModelHolder} for the main flow resource</li>
     * <li>create the {@link FlowBuilder} with the given
     * {@code FlowModelHolder}</li>
     * </ol>
     * 
     * @see ExternalizedMockFlowBuilder#buildFlow() buildFlow
     * @see ExternalizedMockFlowBuilder#registerBeans(MockFlowBuilderContext)
     *      registerBeans
     */
    @Override
    protected FlowBuilder createFlowBuilder() {
        registerDependentFlows();
        FlowDefinitionResource resource = getConfiguration().getResource();
        FlowModelHolder modelHolder = createFlowModelHolder(resource);
        register(resource.getId(), modelHolder);
        return new FlowModelFlowBuilder(modelHolder);
    }

    private void registerDependentFlows() {
        ((XMLMockFlowConfiguration) getConfiguration()).getFlowResources().forEach(this::registerFlowResource);
    }

    private void registerFlowResource(FlowDefinitionResource resource) {
        FlowModelHolder modelHolder = createFlowModelHolder(resource);
        register(resource.getId(), modelHolder);
    }

    private FlowModelHolder createFlowModelHolder(FlowDefinitionResource resource) {
        FlowModelBuilder flowModelBuilder = new XmlFlowModelBuilder(resource.getPath(), flowModelRegistry);
        return new DefaultFlowModelHolder(flowModelBuilder);
    }

    private void register(String id, FlowModelHolder modelHolder) {
        flowModelRegistry.registerFlowModel(id, modelHolder);
    }
}
