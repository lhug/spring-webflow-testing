package de.lhug.webflowtester.builder.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.webflow.config.FlowDefinitionResource;

/**
 * Configuration class to easily create XML Flow definitions to test from.
 * 
 * Offers additional functionality for flow inheritance
 *
 */
public class XMLMockFlowConfiguration extends ExternalizedMockFlowConfiguration {

    private final List<Object> offers = new ArrayList<>();

    private List<FlowDefinitionResource> resources;

    /**
     * Constructs this configuration object using the given {@code offer} as the
     * main flow resource.
     * 
     * @param offer
     *            an Object representing an XML Flow Resource Location
     */
    public XMLMockFlowConfiguration(Object offer) {
        super(offer);
    }

    /**
     * Registers a resource location object to create a parent flow definition
     * from.
     * 
     * @param offer
     *            a {@link Object resource location} to create a
     *            {@link FlowDefinitionResource} from
     */
    public void addParentFlow(Object offer) {
        offers.add(offer);
    }

    /**
     * Creates the actual {@link FlowDefinitionResource} objects from the
     * registered {@link Object offers} and caches them.
     * 
     * This method can be called multiple times, but Resource locations added
     * after the first call of this method will not be added to the created
     * {@link FlowDefinitionResource}s. It also calls
     * {@link #createResource(Object)}, meaning that the caveats for
     * {@link #withBasePath(String)} apply here as well.
     * 
     * @return an {@link Collections#unmodifiableList(List) unmodifiable} view
     *         of the registered {@link FlowDefinitionResource}s
     * 
     * @see #withBasePath(String)
     * @see #createResource(Object)
     */
    public List<FlowDefinitionResource> getFlowResources() {
        if (resources == null) {
            resources = offers.stream()
                    .map(this::createResource)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(resources);
    }
}
