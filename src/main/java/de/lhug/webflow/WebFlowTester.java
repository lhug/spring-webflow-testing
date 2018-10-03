package de.lhug.webflow;

import java.net.URL;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.FlowAssembler;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.model.FlowModelFlowBuilder;
import org.springframework.webflow.engine.impl.FlowExecutionImpl;
import org.springframework.webflow.engine.impl.FlowExecutionImplFactory;
import org.springframework.webflow.engine.model.builder.DefaultFlowModelHolder;
import org.springframework.webflow.engine.model.builder.FlowModelBuilder;
import org.springframework.webflow.engine.model.builder.xml.XmlFlowModelBuilder;
import org.springframework.webflow.engine.model.registry.FlowModelHolder;
import org.springframework.webflow.engine.model.registry.FlowModelRegistry;
import org.springframework.webflow.engine.model.registry.FlowModelRegistryImpl;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.FlowExecutionFactory;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.FlowExecutionOutcome;
import org.springframework.webflow.execution.factory.StaticFlowExecutionListenerLoader;
import org.springframework.webflow.test.MockFlowBuilderContext;

import lombok.Getter;

@Getter
public class WebFlowTester {

	private URL mainFlowResource;

	// required fields
	private Flow cachedFlowDefinition;
	private boolean cacheFlowDefinition;

	private FlowExecutionFactory flowExecutionFactory;
	private FlowExecution flowExecution;
	private FlowExecutionOutcome flowExecutionOutcome;

	private FlowDefinitionResourceFactory resourceFactory;
	private MockFlowBuilderContext flowBuilderContext;

	private FlowModelRegistry flowModelRegistry = new FlowModelRegistryImpl();

	public WebFlowTester(URL mainFlowResource) {
		init();
		this.mainFlowResource = mainFlowResource;
	}

	// existing methods

	public void startFlow(MutableAttributeMap input, ExternalContext context) {
		flowExecution = getFlowExecutionFactory().createFlowExecution(getFlowDefinition());
		flowExecution.start(input, context);
		if (flowExecution.hasEnded()) {
			flowExecutionOutcome = flowExecution.getOutcome();
		}
	}

	public void startFlow(ExternalContext context) {
		startFlow(null, context);
	}

	public FlowExecutionFactory getFlowExecutionFactory() {
		if (flowExecutionFactory == null) {
			flowExecutionFactory = createFlowExecutionFactory();
		}
		return flowExecutionFactory;
	}

	public void resumeFlow(ExternalContext context) {
		Assert.state(flowExecution != null, "The flow execution to test is [null]; "
				+ "you must start the flow execution before you can resume it!");
		flowExecution.resume(context);
		if (flowExecution.hasEnded()) {
			flowExecutionOutcome = flowExecution.getOutcome();
		}
	}

	public void setCurrentState(String stateId) {
		if (flowExecution == null) {
			flowExecution = getFlowExecutionFactory().createFlowExecution(getFlowDefinition());
		}
		((FlowExecutionImpl) flowExecution).setCurrentState(stateId);
	}

	// lazy on getFlowExecutionFactory
	public FlowExecutionFactory createFlowExecutionFactory() {
		return new FlowExecutionImplFactory();
	}

	public ResourceLoader createResourceLoader() {
		return new DefaultResourceLoader();
	}

	public final FlowDefinition getFlowDefinition() {
		if (isCacheFlowDefinition() && cachedFlowDefinition != null) {
			return cachedFlowDefinition;
		}
		Flow flow = buildFlow();
		if (isCacheFlowDefinition()) {
			cachedFlowDefinition = flow;
		}
		return flow;
	}

	protected final Flow buildFlow() {
		FlowDefinitionResource resource = getResource(getResourceFactory());
		flowBuilderContext = new MockFlowBuilderContext(resource.getId(), resource.getAttributes());
		configureFlowBuilderContext(flowBuilderContext);
		FlowBuilder builder = createFlowBuilder(resource);
		FlowAssembler assembler = new FlowAssembler(builder, flowBuilderContext);
		return assembler.assembleFlow();
	}

	private void configureFlowBuilderContext(MockFlowBuilderContext builderContext) {
		// add beans, subflows, etc
	}

	private FlowDefinitionResource getResource(FlowDefinitionResourceFactory resourceFactory) {

		return resourceFactory.createResource(mainFlowResource.toExternalForm());
	}

	protected FlowDefinitionResource[] getModelResources(FlowDefinitionResourceFactory resourceFactory) {
		// parents and such
		return new FlowDefinitionResource[0];
	}

	protected final FlowBuilder createFlowBuilder(FlowDefinitionResource resource) {
		registerDependentFlowModels();
		FlowModelBuilder modelBuilder = new XmlFlowModelBuilder(resource.getPath(), flowModelRegistry);
		FlowModelHolder modelHolder = new DefaultFlowModelHolder(modelBuilder);
		flowModelRegistry.registerFlowModel(resource.getId(), modelHolder);
		return new FlowModelFlowBuilder(modelHolder) {
			@Override
			protected void registerFlowBeans(ConfigurableBeanFactory flowBeanFactory) {
				registerMockFlowBeans(flowBeanFactory);
			}
		};
	}

	// convenience methods here

	protected void setFlowExecutionAttributes(AttributeMap executionAttributes) {
		getFlowExecutionImplFactory().setExecutionAttributes(executionAttributes);
	}

	protected void setFlowExecutionListener(FlowExecutionListener executionListener) {
		getFlowExecutionImplFactory().setExecutionListenerLoader(
				new StaticFlowExecutionListenerLoader(executionListener));
	}

	protected final Flow getFlow() {
		return (Flow) getFlowDefinition();
	}

	protected FlowDefinitionRegistry getFlowDefinitionRegistry() {
		return (FlowDefinitionRegistry) flowBuilderContext.getFlowDefinitionLocator();
	}

	protected MutableAttributeMap getViewScope() {
		return getFlowExecution().getActiveSession().getViewScope();
	}

	protected MutableAttributeMap getFlowScope() {
		return getFlowExecution().getActiveSession().getScope();
	}

	protected MutableAttributeMap getConversationScope() {
		return getFlowExecution().getConversationScope();
	}

	/**
	 * Returns the attribute in view scope. View-scoped attributes are local to
	 * the current view state and are cleared when the view state exits.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 */
	protected Object getViewAttribute(String attributeName) {
		return getFlowExecution().getActiveSession().getViewScope().get(attributeName);
	}

	/**
	 * Returns the required attribute in view scope; asserts the attribute is
	 * present. View-scoped attributes are local to the current view state and
	 * are cleared when the view state exits.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException
	 *             if the attribute was not present
	 */
	protected Object getRequiredViewAttribute(String attributeName) {
		return getFlowExecution().getActiveSession().getViewScope().getRequired(attributeName);
	}

	/**
	 * Returns the required attribute in view scope; asserts the attribute is
	 * present and of the correct type. View-scoped attributes are local to the
	 * current view state and are cleared when the view state exits.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException
	 *             if the attribute was not present or was of the wrong type
	 */
	protected Object getRequiredViewAttribute(String attributeName, Class<?> requiredType) {
		return getFlowExecution().getActiveSession().getViewScope().getRequired(attributeName, requiredType);
	}

	/**
	 * Returns the attribute in flow scope. Flow-scoped attributes are local to
	 * the active flow session.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 */
	protected Object getFlowAttribute(String attributeName) {
		return getFlowExecution().getActiveSession().getScope().get(attributeName);
	}

	/**
	 * Returns the required attribute in flow scope; asserts the attribute is
	 * present. Flow-scoped attributes are local to the active flow session.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException
	 *             if the attribute was not present
	 */
	protected Object getRequiredFlowAttribute(String attributeName) {
		return getFlowExecution().getActiveSession().getScope().getRequired(attributeName);
	}

	/**
	 * Returns the required attribute in flow scope; asserts the attribute is
	 * present and of the correct type. Flow-scoped attributes are local to the
	 * active flow session.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException
	 *             if the attribute was not present or was of the wrong type
	 */
	protected Object getRequiredFlowAttribute(String attributeName, Class<?> requiredType) {
		return getFlowExecution().getActiveSession().getScope().getRequired(attributeName, requiredType);
	}

	/**
	 * Returns the attribute in conversation scope. Conversation-scoped
	 * attributes are shared by all flow sessions.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 */
	protected Object getConversationAttribute(String attributeName) {
		return getFlowExecution().getConversationScope().get(attributeName);
	}

	/**
	 * Returns the required attribute in conversation scope; asserts the
	 * attribute is present. Conversation-scoped attributes are shared by all
	 * flow sessions.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException
	 *             if the attribute was not present
	 */
	protected Object getRequiredConversationAttribute(String attributeName) {
		return getFlowExecution().getConversationScope().getRequired(attributeName);
	}

	/**
	 * Returns the required attribute in conversation scope; asserts the
	 * attribute is present and of the required type. Conversation-scoped
	 * attributes are shared by all flow sessions.
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the attribute value
	 * @throws IllegalStateException
	 *             if the attribute was not present or not of the required type
	 */
	protected Object getRequiredConversationAttribute(String attributeName, Class<?> requiredType) {
		return getFlowExecution().getConversationScope().getRequired(attributeName, requiredType);
	}

	// internal helpers

	private void init() {
		resourceFactory = new FlowDefinitionResourceFactory(createResourceLoader());
	}

	private FlowExecutionImplFactory getFlowExecutionImplFactory() {
		return (FlowExecutionImplFactory) getFlowExecutionFactory();
	}

	private void registerDependentFlowModels() {
		FlowDefinitionResource[] modelResources = getModelResources(getResourceFactory());
		if (modelResources != null) {
			for (int i = 0; i < modelResources.length; i++) {
				FlowDefinitionResource modelResource = modelResources[i];
				FlowModelBuilder modelBuilder = new XmlFlowModelBuilder(modelResource.getPath(), flowModelRegistry);
				flowModelRegistry.registerFlowModel(modelResource.getId(), new DefaultFlowModelHolder(modelBuilder));
			}
		}
	}

	protected void registerMockFlowBeans(ConfigurableBeanFactory flowBeanFactory) {
	}

}
