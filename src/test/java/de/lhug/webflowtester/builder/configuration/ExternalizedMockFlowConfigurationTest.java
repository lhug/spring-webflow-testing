package de.lhug.webflowtester.builder.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExternalizedMockFlowConfigurationTest {

	private ExternalizedMockFlowConfiguration sut;

	@BeforeEach
	public void setUp() {
		sut = new ExternalizedMockFlowConfiguration("/simpleFlows/standaloneFlow.xml");
	}

	@Test
	public void shouldCreateConfigurationFromUrl() throws Exception {
		var url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

		sut = new ExternalizedMockFlowConfiguration(url);

		var mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("standaloneFlow");
		assertThat(mainResource.getPath().getURL()).isEqualTo(url);
		assertThat(mainResource.getPath().getDescription()).startsWith("URL [file:");
	}

	@Test
	public void shouldCreateConfigurationFromFile() throws Exception {
		var file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

		sut = new ExternalizedMockFlowConfiguration(file);

		var mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("flowWithDependentBeans");
		assertThat(mainResource.getPath().getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(mainResource.getPath().getDescription()).startsWith("file [");
	}

	@Test
	public void shouldCreateConfigurationFromClasspath() {
		String resource = "/simpleFlows/standaloneFlow.xml";

		sut = new ExternalizedMockFlowConfiguration(resource);

		var mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("standaloneFlow");
		assertThat(mainResource.getPath().getDescription())
				.isEqualTo("class path resource [simpleFlows/standaloneFlow.xml]");
	}

	@Test
	public void shouldCreateConfigurationWithFallingBackToString() {
		sut = new ExternalizedMockFlowConfiguration(List.of("content, more content"));

		var mainResource = sut.getResource();
		assertThat(mainResource.getId()).isEqualTo("[content, more content]");
		assertThat(mainResource.getPath().getDescription())
				.isEqualTo("class path resource [[content, more content]]");
	}

	@Test
	public void shouldCreateFlowDefinitionResourceFromURL() throws Exception {
		var url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

		var result = sut.createResource(Objects.requireNonNull(url));

		assertThat(result.getId()).isEqualTo("standaloneFlow");
		assertThat(result.getPath().getURL()).isEqualTo(url);
		assertThat(result.getPath().getDescription()).startsWith("URL [file:");
	}

	@Test
	public void shouldCreateFlowDefinitionResourceFromFile() throws Exception {
		var file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

		var result = sut.createResource(file);

		assertThat(result.getId()).isEqualTo("flowWithDependentBeans");
		assertThat(result.getPath().getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(result.getPath().getDescription()).startsWith("file [");
	}

	@Test
	public void shouldCreateFlowDefinitionResourceFromClasspathString() {
		String resource = "/simpleFlows/standaloneFlow.xml";

		var result = sut.createResource(resource);

		assertThat(result.getId()).isEqualTo("standaloneFlow");
		assertThat(result.getPath().getDescription())
				.isEqualTo("class path resource [simpleFlows/standaloneFlow.xml]");
	}

	@Test
	public void shouldCreateFlowDefinitionResourceByCallingToStringOnObject() {
		var result = sut.createResource(List.of("content"));

		assertThat(result.getId()).isEqualTo("[content]");
		assertThat(result.getPath().getDescription()).isEqualTo("class path resource [[content]]");
	}

	@Test
	public void shouldDeriveIdByResolvingWithBasePath() {
		sut = new ExternalizedMockFlowConfiguration("resources/simpleFlows/standaloneFlow.xml");

		sut.withBasePath("src/test/");

		var result = sut.getResource();
		assertThat(result.getId()).isEqualTo("resources/simpleFlows");
	}
}
