package de.lhug.webflowtester.builder.configuration;

import org.springframework.webflow.config.FlowDefinitionResource;

/**
 * Interface denoting an implementation able to map any {@link Object} to a
 * {@link FlowDefinitionResource}.
 *
 * Exists to allow for easy extension of the Flow Building mechanisms.
 *
 */
@FunctionalInterface
public interface ResourceMapper {
	/**
	 * Maps the input {@link Object offer} to a {@link FlowDefinitionResource}
	 *
	 *
	 * @param offer
	 *            The {@link Object} containing information to create a
	 *            {@link FlowDefinitionResource} from
	 * @return the created {@link FlowDefinitionResource}
	 */
	FlowDefinitionResource createResource(Object offer);
}
