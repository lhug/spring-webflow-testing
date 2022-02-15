package de.lhug.webflowtester.builder.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.webflow.config.FlowDefinitionResource;

public class XMLMockFlowConfigurationTest {

	private XMLMockFlowConfiguration sut;

	@BeforeEach
	public void setUp() {
		sut = new XMLMockFlowConfiguration("/inheritanceFlows/childFlow.xml");
	}

	@Test
	public void shouldCreateConfigurationFromUrl() throws Exception {
		URL url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

		sut = new XMLMockFlowConfiguration(url);

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("standaloneFlow");
		assertThat(mainResource.getPath().getURL()).isEqualTo(url);
		assertThat(mainResource.getPath().getDescription()).startsWith("URL [file:");
	}

	@Test
	public void shouldCreateConfigurationFromFile() throws Exception {
		File file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

		sut = new XMLMockFlowConfiguration(file);

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("flowWithDependentBeans");
		assertThat(mainResource.getPath().getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(mainResource.getPath().getDescription()).startsWith("file [");
	}

	@Test
	public void shouldCreateConfigurationFromClasspath() throws Exception {
		String resource = "/simpleFlows/standaloneFlow.xml";

		sut = new XMLMockFlowConfiguration(resource);

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("standaloneFlow");
		assertThat(mainResource.getPath().getDescription())
				.isEqualTo("class path resource [simpleFlows/standaloneFlow.xml]");
	}

	@Test
	public void shouldCreateConfigurationWithFallingBacktoString() throws Exception {
		sut = new XMLMockFlowConfiguration(Arrays.asList("content, more content"));

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("[content, more content]");
		assertThat(mainResource.getPath().getDescription())
				.isEqualTo("class path resource [[content, more content]]");
	}

	@Test
	public void shouldAddParentResourceFromURL() throws Exception {
		URL parentFlow = getClass().getResource("/inheritanceFlows/parentFlow.xml");

		sut.addParentFlow(parentFlow);

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getURL)
				.containsExactly(parentFlow);
	}

	@Test
	public void shouldAddParentResourceFromFile() throws Exception {
		File file = new File("src/test/resources/inheritanceFlows/parentFlow.xml");

		sut.addParentFlow(file);

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getFile)
				.containsExactly(file.getAbsoluteFile());
	}

	@Test
	public void shouldAddParentResourceFromClasspathString() throws Exception {
		String classpath = "/inheritanceFlows/parentFlow.xml";

		sut.addParentFlow(classpath);

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getDescription)
				.containsExactly("class path resource [inheritanceFlows/parentFlow.xml]");
	}

	@Test
	public void shouldAddParentResourceWithFallingBackToString() throws Exception {
		sut.addParentFlow(Arrays.asList("seven, nineteen"));

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getDescription)
				.containsExactly("class path resource [[seven, nineteen]]");
	}

	@Test
	public void shouldAddMultipleParentResources() throws Exception {
		sut.addParentFlow("/inheritanceFlows/parentFlow.xml");
		sut.addParentFlow("/simpleFlows/standaloneFlow.xml");

		List<FlowDefinitionResource> result = sut.getFlowResources();

		assertThat(result)
				.extracting(FlowDefinitionResource::getId)
				.containsExactly("parentFlow", "standaloneFlow");
	}

	@Test
	public void shouldReturnEmptyListWhenNoParentResourcesWereAdded() throws Exception {
		assertThat(sut.getFlowResources()).isEmpty();
	}

	@Test
	public void shouldReturnUnmodifiableListOfResources() throws Exception {
		List<FlowDefinitionResource> result = sut.getFlowResources();

		var mock = mock(FlowDefinitionResource.class);

		assertThatThrownBy(() -> result.add(mock(FlowDefinitionResource.class)))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void shouldDeriveIdByResolvingWithBasePath() throws Exception {
		sut.addParentFlow("resources/inheritanceFlows/parentFlow.xml");

		sut.withBasePath("src/test");

		List<FlowDefinitionResource> result = sut.getFlowResources();

		assertThat(result)
				.extracting(FlowDefinitionResource::getId)
				.containsExactly("resources/inheritanceFlows");
	}
}
