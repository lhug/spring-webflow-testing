package de.lhug.webflowtester.builder.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;
import org.springframework.webflow.engine.Flow;

import lombok.NoArgsConstructor;

/**
 * A utility class to hold beans to be accessible in a tested flow.
 * 
 * Basically, this accepts beans which will in turn be passed to the
 * {@link Flow}s internal {@link ApplicationContext}
 *
 */
@NoArgsConstructor
public class FlowTestContext {

    private final Map<String, Object> beans = new HashMap<>();
    private final List<FlowDefinitionHolder> subFlows = new ArrayList<>();

    /**
     * Creates an instance with the passed spring beans already registered.
     * 
     * The names of the spring beans are being derived from
     * {@link Conventions#getVariableName(Object) spring conventions}.
     * 
     * @see {@link #addBean(Object)}
     * @param offers
     *            The spring beans to be registered with the flow context
     */
    public FlowTestContext(Object... offers) {
        for (Object offer : offers) {
            this.addBean(offer);
        }
    }

    /**
     * Used to determine if a bean has already been registered in the context
     * 
     * More formally, this checks if a bean that {@link Object#equals(Object)
     * equals} the offer is already present, regardless of the expected or given
     * name.
     * 
     * @param offer
     *            The spring bean to be checked
     * @return {@code true} if the bean is already registered with any name,
     *         {@code false} if not.
     */
    public boolean containsBean(Object offer) {
        return beans.containsValue(offer);
    }

    /**
     * Used to determine if a bean with the given name has already been
     * registeres
     * 
     * More formally, this checks if a bean name that
     * {@link String#equals(Object) equals} the offer has already been
     * registered. This does not check for the actual bean type.
     * 
     * @param name
     *            The name to be checked
     * @return {@code true} if the name is already registered in the context,
     *         {@code false} if not
     */
    public boolean containsBeanWithName(String name) {
        return beans.containsKey(name);
    }

    /**
     * Adds a bean with a generated name to be registered.
     * 
     * The name is being generated by using
     * {@link Conventions#getVariableName(Object) spring conventions}.
     * 
     * @param offer
     *            The bean to be registered
     * 
     * @see #addBean(String, Object)
     */
    public void addBean(Object offer) {
        this.addBean(Conventions.getVariableName(offer), offer);
    }

    /**
     * Adds a bean with the given name to be registered.
     * 
     * This adds a bean object with the given name into the data structure that
     * will later be extracted and used to create the {@link ApplicationContext}
     * for the tested {@link Flow}. This readily accepts mock objects to allow
     * easy dependency configuration.
     * 
     * @param name
     *            The name the bean will be registered with
     * @param offer
     *            the actual bean to be registered
     */
    public void addBean(String name, Object offer) {
        beans.put(name, offer);
    }

    /**
     * Returns an unmodifiable view of the registered beans. Note that Changes
     * made to the contents might still write-through to the registered objects.
     * 
     * @return an {@link Collections#unmodifiableMap(Map) unmodifiable} view of
     *         the registered beans, never {@code null}
     */
    public Map<String, Object> getBeans() {
        return Collections.unmodifiableMap(beans);
    }

    /**
     * Returns an unmodifiable view of the registered Subflows.
     * 
     * Note that changes made to the contents might still write-through to the
     * registered objects.
     * 
     * @return a {@link Collections#unmodifiableList(List) unmodifiable}
     *         {@link List} of the registered SubFlows, never {@code null}
     */
    public List<FlowDefinitionHolder> getSubFlows() {
        return Collections.unmodifiableList(subFlows);
    }

    public void addSubFlow(FlowDefinitionHolder subFlow) {
        subFlows.add(subFlow);
    }
}
