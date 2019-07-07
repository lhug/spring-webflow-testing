package de.lhug.webflowtester.builder.context;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.CollectionUtils;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.support.FlowBuilderContextImpl;

import de.lhug.webflowtester.builder.services.TestFlowBuilderServicesFactory;

/**
 * A stub flow service locator implementation suitable for a test environment.
 * <p>
 * Allows programmatic registration of subflows needed by a flow execution being
 * tested, see {@link #registerSubflow(Flow)}. Subflows registered are typically
 * stubs that verify parent flow input and output scenarios.
 * </p>
 * <p>
 * Also supports programmatic registration of additional custom services needed
 * by a flow (such as Actions) managed in a backing Spring
 * {@link ConfigurableBeanFactory}. See the
 * {@link #registerBean(String, Object)} method. Beans registered are typically
 * mocks or stubs of business services invoked by the flow.
 * </p>
 * 
 */
public class MockFlowBuilderContext extends FlowBuilderContextImpl {

    /**
     * Creates a new mock flow service locator for a {@link Flow} with the given id
     * and no attributes.
     * 
     * @param flowId the String denoting the id of the flow to build
     */
    public MockFlowBuilderContext(String flowId) {
        this(flowId, CollectionUtils.EMPTY_ATTRIBUTE_MAP);
    }

    /**
     * Creates a new mock flow service locator for a {@link Flow} with the given id
     * and attributes
     * 
     * @param flowId     the String denoting the id of the flow to build
     * @param attributes the AttributeMap containing flow attributes
     */
    public MockFlowBuilderContext(String flowId, AttributeMap attributes) {
        super(flowId, attributes, new FlowDefinitionRegistryImpl(), TestFlowBuilderServicesFactory.getServices());
    }

    /**
     * Register a subflow definition in the backing flow registry, typically to
     * support a flow execution test. For test scenarios, the subflow is often a
     * stub used to verify parent flow input and output mapping behavior.
     * 
     * @param subflow the subflow
     */
    public void registerSubflow(Flow subflow) {
        ((FlowDefinitionRegistryImpl) getFlowDefinitionLocator()).registerFlowDefinition(subflow);
    }

    /**
     * Register a bean in the backing bean factory, typically to support a flow
     * execution test. For test scenarios, if the bean is a service invoked by a
     * bean invoking action it is often a stub or dynamic mock implementation of the
     * service's business interface.
     * 
     * @param beanName the bean name
     * @param bean     the singleton instance
     */
    public void registerBean(String beanName, Object bean) {
        ((ConfigurableApplicationContext) getApplicationContext()).getBeanFactory().registerSingleton(beanName, bean);
    }

}
