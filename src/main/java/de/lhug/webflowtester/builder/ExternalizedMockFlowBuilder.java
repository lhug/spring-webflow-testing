package de.lhug.webflowtester.builder;

import java.util.Locale;
import java.util.Map.Entry;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.FlowAssembler;
import org.springframework.webflow.engine.builder.FlowBuilder;

import de.lhug.webflowtester.builder.MessageContainer.Message;
import de.lhug.webflowtester.builder.MessageContainer.Messages;
import de.lhug.webflowtester.builder.configuration.ExternalizedMockFlowConfiguration;
import de.lhug.webflowtester.builder.configuration.FlowTestContext;
import de.lhug.webflowtester.builder.context.MockFlowBuilderContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Abstract base class for externalized Flow Definitions, such as XML Flow
 * Definitions.
 *
 * This accepts a {@link ExternalizedMockFlowConfiguration} object containing
 * all necessary information to create usable {@link Flow} objects. It comes
 * with an automatic caching mechanism to prevent a single flow from being built
 * multiple times. It also contains a {@link #withContext(FlowTestContext)
 * method} to allow easy configuration of the internal
 * {@link org.springframework.context.ApplicationContext}. Should further
 * configuration be required, subclasses can override
 * {@link #registerBeans(MockFlowBuilderContext)}.
 *
 */
@RequiredArgsConstructor
public abstract class ExternalizedMockFlowBuilder implements MockFlowBuilder {

	@Getter(AccessLevel.PACKAGE)
	private final ExternalizedMockFlowConfiguration configuration;
	private FlowTestContext context;

	private Flow flow;

	/**
	 * Creates a ready-to-use {@link Flow} instance. This class caches the first
	 * built {@link Flow} to allow for safe multiple calls, however that still
	 * should be avoided as the returned Object is mutable and not a defensive copy.
	 * Any changes to the returned object <em>will</em> write through to the cached
	 * instance.
	 *
	 * Building a Flow with this follows this order:
	 * <ol>
	 * <li>Create a
	 * {@link org.springframework.webflow.engine.builder.FlowBuilderContext
	 * FlowBuilderContext} for the Flow</li>
	 * <li>Call {@link #registerBeans(MockFlowBuilderContext)}</li>
	 * <li>Call {@link #registerStubFlows(FlowDefinitionRegistry)}</li>
	 * <li>Call {@link #createFlowBuilder()}</li>
	 * <li>Assemble and return the {@link Flow}</li>
	 * </ol>
	 */
	@Override
	public Flow buildFlow() {
		if (flow == null) {
			buildInternal();
		}
		return flow;
	}

	private void buildInternal() {
		FlowDefinitionResource resource = configuration.getResource();
		MockFlowBuilderContext builderContext = new MockFlowBuilderContext(resource.getId());
		registerBeans(builderContext);
		registerStubFlows((FlowDefinitionRegistry) builderContext.getFlowDefinitionLocator());
		registerMessages(((StaticApplicationContext) builderContext.getApplicationContext()).getStaticMessageSource());
		FlowBuilder builder = createFlowBuilder();
		flow = new FlowAssembler(builder, builderContext).assembleFlow();
	}

	/**
	 * Registers beans in the internal
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * <p>
	 * The passed {@link MockFlowBuilderContext} allows for registering other
	 * objects as well. It can be used to register {@link Flow} implementations, it
	 * is suggested to use {@link #registerStubFlows(FlowDefinitionRegistry)} for
	 * this. Subclasses may override this to further enrich the context. By default,
	 * if a {@link FlowTestContext} is passed to the builder, this method will
	 * register all beans defined in the passed {@link FlowTestContext} with their
	 * respective names in the Flows
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * </p>
	 * <p>
	 * Note that this does not fire any initialization callbacks, all beans
	 * <b>must</b> be fully initialized.
	 * </p>
	 * This method is being called once by {@link #buildFlow()}, right before
	 * {@link #registerStubFlows(FlowDefinitionRegistry)}.
	 *
	 * @param builderContext the
	 *                       {@link org.springframework.webflow.engine.builder.FlowBuilderContext
	 *                       FlowBuilderContext} used to create the Flows
	 *                       {@link org.springframework.context.ApplicationContext}.
	 * @see MockFlowBuilderContext#registerBean(String, Object)
	 */
	protected void registerBeans(MockFlowBuilderContext builderContext) {
		if (context != null) {
			context.getBeans().forEach(builderContext::registerBean);
		}
	}

	/**
	 * Registers Sub Flows in the {@link FlowDefinitionRegistry}
	 * <p>
	 * The passed {@link FlowDefinitionRegistry} allows registering custom flow
	 * definitions. By default, this method registers each
	 * {@link org.springframework.webflow.definition.registry.FlowDefinitionHolder
	 * FlowDefinitionHolder} that has been previously registered in the
	 * {@link FlowTestContext}.
	 * </p>
	 * <p>
	 * Subclasses may override to add other implementations, like {@link Flow}s,
	 * directly to the registry. This is being called once by {@link #buildFlow()},
	 * right before {@link #registerMessages(StaticMessageSource)}
	 * </p>
	 *
	 * @param registry the {@link FlowDefinitionRegistry} present in the
	 *                 {@link org.springframework.webflow.engine.builder.FlowBuilderContext
	 *                 FlowBuilderContext} used to build the {@link Flow} to test
	 */
	protected void registerStubFlows(FlowDefinitionRegistry registry) {
		if (context != null) {
			context.getSubFlows().forEach(registry::registerFlowDefinition);
		}
	}

	/**
	 * Registers Messages in the {@link StaticMessageSource}
	 * <p>
	 * The passed {@link StaticMessageSource} allows adding of static messages to
	 * the context to avoid complicated resource loading and properties-handling. By
	 * default, this iterates over all {@link Locale} - {@link Messages} pairs and
	 * registers each {@link Message} in the context.
	 * </p>
	 * <p>
	 * Subclasses may override to add default messages to the flow. To keep the
	 * messages as default, but overridable, call
	 * {@code super.registerMessages(StaticMessageSource)} after adding the
	 * fallbacks, if the messages should be un-overridable, add them after calling
	 * {@code super}
	 * </p>
	 * <p>
	 * This is called once by {@link #buildFlow()}, right before
	 * {@link #createFlowBuilder()}
	 * </p>
	 *
	 * @param messageSource the StaticMessageSource provided by the
	 *                      {@link StaticApplicationContext}
	 */
	protected void registerMessages(StaticMessageSource messageSource) {
		if (context != null) {
			for (Entry<Locale, Messages> entry : context.getAllMessages().entrySet()) {
				Locale currentLocale = entry.getKey();
				for (Message message : entry.getValue().messageStore) {
					messageSource.addMessage(message.getKey(), currentLocale, message.getValue());
				}
			}
		}
	}

	/**
	 * Needed to create the actual {@link FlowBuilder}.
	 *
	 * The {@link FlowBuilder} must be usable, meaning it must contain
	 * <ul>
	 * <li>a reference to the main resource</li>
	 * <li>references to all required dependent resources</li>
	 * </ul>
	 * If it does not, the Flow building will fail. This is being called once during
	 * {@link #buildFlow()}, right after
	 * {@link #registerBeans(MockFlowBuilderContext)}.
	 *
	 * @return a fully configured {@link FlowBuilder}
	 */
	protected abstract FlowBuilder createFlowBuilder();

	/**
	 * Configures this builder to move all beans and SubFlows registered within the
	 * passed {@link FlowTestContext} into the Flows
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}. An
	 * existing instance of {@link FlowTestContext} will be replaced with each
	 * successive call. To remove all Beans from the context, pass {@code null}.
	 *
	 * @param context the pre-registered beans to be moved into the
	 *                {@link org.springframework.context.ApplicationContext
	 *                ApplicationContext}
	 * @return this
	 */
	public MockFlowBuilder withContext(FlowTestContext context) {
		this.context = context;
		return this;
	}
}
