package de.lhug.webflowtester.builder.configuration;

import java.io.File;
import java.net.URL;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.webflow.config.FlowDefinitionResource;
import org.springframework.webflow.config.FlowDefinitionResourceFactory;

import lombok.RequiredArgsConstructor;

/**
 * Configuration class for a single external flow resource.
 * 
 * Holds configuration information for a single external flow resource. The
 * given {@link Object} given as a possible resource will be examined and a
 * {@link FlowDefinitionResource} is being created from it. The actual
 * conversion is done when {@link #getResource()} is called.
 * 
 * @see #getResource()
 * @see #withBasePath(String)
 *
 */
@RequiredArgsConstructor
public class ExternalizedMockFlowConfiguration {

    private final Object resourceLocation;

    private FlowDefinitionResource resource;
    private FlowDefinitionResourceFactory resourceFactory;
    private String basePath;

    /**
     * lazy-gets the {@link FlowDefinitionResourceFactory}.
     * 
     * More formally, if no {@link FlowDefinitionResourceFactory} exists, a new
     * instance is being created, utilizing a {@link DefaultResourceLoader} to
     * load the given resources. If a {@link #withBasePath(String) base path} is
     * given, it will be registered with the factory at this point. This implies
     * that the base path can not be automatically set by this configuration
     * object and instead must be changed by calling{@link FlowDefinitionResourceFactory#setBasePath(String)}.
     * As this method is {@code package-private}, this is not intended to happen.
     * 
     * @return the initialized {@link FlowDefinitionResourceFactory}
     */
    final FlowDefinitionResourceFactory getResourceFactory() {
        if (resourceFactory == null) {
            resourceFactory = new FlowDefinitionResourceFactory(new DefaultResourceLoader());
            if (basePath != null) {
                resourceFactory.setBasePath(basePath);
            }
        }
        return resourceFactory;
    }

    /**
     * Loads and caches the main flow resource.
     * 
     * More formally: If a {@link FlowDefinitionResource} has not yet been
     * created, the given resource location is being examined, and a suitable
     * {@link ResourceMapper} is being selected. This then fetches the
     * {@link FlowDefinitionResourceFactory} and passes the resource to it after
     * mapping its contents to an appropriate form. The result is then cached
     * and the cached result returned. This allows for safe successive calls.
     * 
     * @return the {@link FlowDefinitionResource} as defined by the passed
     *         resource location
     * 
     * @see #createResource(Object)
     * @see #determineResourceMapper(Class)
     * @see #withBasePath(String)
     */
    public final FlowDefinitionResource getResource() {
        if (resource == null) {
            resource = createResource(resourceLocation);
        }
        return resource;
    }

    /**
     * Creates a resource from a given resource location {@link Object}.
     * 
     * The given {@code resource location} is being examined and an appropriate
     * {@link ResourceMapper} is selected. The
     * {@link FlowDefinitionResourceFactory} is then being fetched and used to
     * actually create the {@link FlowDefinitionResource}. Subclasses can
     * utilize this method to create {@link FlowDefinitionResource}s from
     * resource locations
     * 
     * @param resource
     *            the {@link Object resource location}
     * @return the {@link FlowDefinitionResource} from the given
     *         {@code resource location}
     * 
     * @see #determineResourceMapper(Class)
     * @see #withBasePath(String)
     */
    protected final FlowDefinitionResource createResource(Object resource) {
        ResourceMapper resourceMapper = determineResourceMapper(resource.getClass());
        return resourceMapper.createResource(resource);
    }

    /**
     * Fetches a {@link ResourceMapper} instance.
     * 
     * Specifically, this does the following checks:
     * <dl>
     * <dt>can the {@code offer} be assigned to {@link URL}?</dt>
     * <dd>create the {@link FlowDefinitionResource} utilizing
     * {@link FlowDefinitionResourceFactory#createResource(String)
     * createResource()} and {@link URL#toExternalForm() toExternalForm()}</dd>
     * <dt>can the {@code offer} be assigned to {@link File}?</dt>
     * <dd>create the {@link FlowDefinitionResource} utilizing
     * {@link FlowDefinitionResourceFactory#createFileResource(String)
     * createFileResource()} and {@link File#getAbsolutePath()
     * getAbsolutePath()}</dd>
     * <dt>can the {@code offer} be assigned to {@link String}?</dt>
     * <dd>the given String will be interpreted as a class path resource, which
     * needs to be accessible from this class</dd>
     * <dt>when all checks failed:</dt>
     * <dd>the {@link FlowDefinitionResource} will be created utilizing
     * {@link FlowDefinitionResourceFactory#createResource(String)
     * createResource()} and calling {@link #toString()} on the
     * {@code offer}</dd>
     * </dl>
     * 
     * Can be overridden by subclasses to allow further specifications.
     * {@code super.determineResourceMapper} should be called last in this
     * chain, as it ends with the described {@link #toString()}-fallback
     * solution. The returned {@link ResourceMapper} must create the
     * {@link FlowDefinitionResource} by itself, as the factory used in this
     * class is not intended to be used outside this class.
     * 
     * @param offer
     *            the {@link Class} of the {@code resource location} passed to
     *            {@link #createResource(Object)} determined by calling
     *            {@link #getClass()}
     * @return a {@link ResourceMapper} implementation able to map the resource
     *         location to a {@link FlowDefinitionResource}
     * 
     * @see #withBasePath(String)
     */
    protected ResourceMapper determineResourceMapper(Class<?> offer) {
        if (URL.class.isAssignableFrom(offer)) {
            return object -> {
                URL resourceUrl = (URL) object;
                return getResourceFactory().createResource(resourceUrl.toExternalForm());
            };
        } else if (File.class.isAssignableFrom(offer)) {
            return object -> {
                File resourceUrl = (File) object;
                return getResourceFactory().createFileResource(resourceUrl.getAbsolutePath());
            };
        } else if (String.class.isAssignableFrom(offer)) {
            return object -> {
                String resourceUrl = (String) object;
                return getResourceFactory().createClassPathResource(resourceUrl, getClass());
            };
        } else {
            return object -> getResourceFactory().createResource(object.toString());
        }
    }

    /**
     * Sets the base path used to derive the flow ids from the resource
     * locations.
     * 
     * This parameter is evaluated <b>once</b> when the factory creating the
     * {@link FlowDefinitionResource}s is called the first time, meaning on
     * {@link #createResource(Object)} and, by proxy, {@link #getResource()}.
     * 
     * The derived flow id becomes the portion of the path between the basePath
     * and the filename. If no directory structure is available then the
     * filename without the extension is used. For example,
     * {@code ${basePath}/booking.xml} becomes {@code booking} and
     * {@code ${basePath}/hotels/booking/booking.xml} becomes
     * {@code hotels/booking}
     * 
     * @param basePath
     *            the base path {@link String} used to resolve the paths to
     *            generate the flow id
     * 
     * @see #getResource()
     * @see #createResource(Object)
     */
    public void withBasePath(String basePath) {
        this.basePath = basePath;
    }
}
