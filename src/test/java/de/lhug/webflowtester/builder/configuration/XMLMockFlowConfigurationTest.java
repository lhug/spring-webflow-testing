package de.lhug.webflowtester.builder.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.webflow.config.FlowDefinitionResource;

class XMLMockFlowConfigurationTest {

	private XMLMockFlowConfiguration sut;

	@BeforeEach
	void setUp() {
		sut = new XMLMockFlowConfiguration("/inheritanceFlows/childFlow.xml");
	}

	@Test
	void shouldCreateConfigurationFromUrl() throws Exception {
		URL url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

		sut = new XMLMockFlowConfiguration(url);

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("standaloneFlow");
		assertThat(mainResource.getPath().getURL()).isEqualTo(url);
		assertThat(mainResource.getPath().getDescription()).startsWith("URL [file:");
	}

	@Test
	void shouldCreateConfigurationFromFile() throws Exception {
		File file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

		sut = new XMLMockFlowConfiguration(file);

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("flowWithDependentBeans");
		assertThat(mainResource.getPath().getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(mainResource.getPath().getDescription()).startsWith("file [");
	}

	@Test
	void shouldCreateConfigurationFromClasspath() {
		String resource = "/simpleFlows/standaloneFlow.xml";

		sut = new XMLMockFlowConfiguration(resource);

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("standaloneFlow");
		assertThat(mainResource.getPath().getDescription())
				.isEqualTo("class path resource [simpleFlows/standaloneFlow.xml]");
	}

	@Test
	void shouldCreateConfigurationWithFallingBackToString() {
		sut = new XMLMockFlowConfiguration(List.of("content, more content"));

		FlowDefinitionResource mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("[content, more content]");
		assertThat(mainResource.getPath().getDescription())
				.isEqualTo("class path resource [[content, more content]]");
	}

	@Test
	void shouldAddParentResourceFromURL() {
		URL parentFlow = getClass().getResource("/inheritanceFlows/parentFlow.xml");

		sut.addParentFlow(parentFlow);

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getURL)
				.containsExactly(parentFlow);
	}

	@Test
	void shouldAddParentResourceFromFile() {
		File file = new File("src/test/resources/inheritanceFlows/parentFlow.xml");

		sut.addParentFlow(file);

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getFile)
				.containsExactly(file.getAbsoluteFile());
	}

	@Test
	void shouldAddParentResourceFromClasspathString() {
		String classpath = "/inheritanceFlows/parentFlow.xml";

		sut.addParentFlow(classpath);

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getDescription)
				.containsExactly("class path resource [inheritanceFlows/parentFlow.xml]");
	}

	@Test
	void shouldAddParentResourceWithFallingBackToString() {
		sut.addParentFlow(List.of("seven, nineteen"));

		assertThat(sut.getFlowResources())
				.extracting(FlowDefinitionResource::getPath)
				.extracting(Resource::getDescription)
				.containsExactly("class path resource [[seven, nineteen]]");
	}

	@Test
	void shouldAddMultipleParentResources() {
		sut.addParentFlow("/inheritanceFlows/parentFlow.xml");
		sut.addParentFlow("/simpleFlows/standaloneFlow.xml");

		List<FlowDefinitionResource> result = sut.getFlowResources();

		assertThat(result)
				.extracting(FlowDefinitionResource::getId)
				.containsExactly("parentFlow", "standaloneFlow");
	}

	@Test
	void shouldReturnEmptyListWhenNoParentResourcesWereAdded() {
		assertThat(sut.getFlowResources()).isEmpty();
	}

	@Test
	void shouldReturnUnmodifiableListOfResources() {
		List<FlowDefinitionResource> result = sut.getFlowResources();

		var mock = mock(FlowDefinitionResource.class);

		assertThatThrownBy(() -> result.add(mock))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldDeriveIdByResolvingWithBasePath() {
		sut.addParentFlow("resources/inheritanceFlows/parentFlow.xml");

		sut.withBasePath("src/test");

		List<FlowDefinitionResource> result = sut.getFlowResources();

		assertThat(result)
				.extracting(FlowDefinitionResource::getId)
				.containsExactly("resources/inheritanceFlows");
	}
}
