package de.lhug.webflowtester.builder.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.webflow.config.FlowDefinitionResource;

public class ExternalizedMockFlowConfigurationTest {

    private ExternalizedMockFlowConfiguration sut;

    @Before
    public void setUp() {
        sut = new ExternalizedMockFlowConfiguration("/simpleFlows/standaloneFlow.xml");
    }

    @Test
    public void shouldCreateConfigurationFromUrl() throws Exception {
        URL url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

        sut = new ExternalizedMockFlowConfiguration(url);

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("standaloneFlow"));
        assertThat(mainResource.getPath().getURL(), is(url));
        assertThat(mainResource.getPath().getDescription(), startsWith("URL [file:"));
    }

    @Test
    public void shouldCreateConfigurationFromFile() throws Exception {
        File file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

        sut = new ExternalizedMockFlowConfiguration(file);

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("flowWithDependentBeans"));
        assertThat(mainResource.getPath().getFile(), is(file.getAbsoluteFile()));
        assertThat(mainResource.getPath().getDescription(), startsWith("file ["));
    }

    @Test
    public void shouldCreateConfigurationFromClasspath() throws Exception {
        String resource = "/simpleFlows/standaloneFlow.xml";

        sut = new ExternalizedMockFlowConfiguration(resource);

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("standaloneFlow"));
        assertThat(mainResource.getPath().getDescription(), is("class path resource [simpleFlows/standaloneFlow.xml]"));
    }

    @Test
    public void shouldCreateConfigurationWithFallingBacktoString() throws Exception {
        sut = new ExternalizedMockFlowConfiguration(Arrays.asList("content, more content"));

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("[content, more content]"));
        assertThat(mainResource.getPath().getDescription(), is("class path resource [[content, more content]]"));
    }

    @Test
    public void shouldCreateFlowDefinitionResourceFromURL() throws Exception {
        URL url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

        FlowDefinitionResource result = sut.createResource(url);

        assertThat(result.getId(), is("standaloneFlow"));
        assertThat(result.getPath().getURL(), is(url));
        assertThat(result.getPath().getDescription(), startsWith("URL [file:"));
    }

    @Test
    public void shouldCreateFlowDefinitionResourceFromFile() throws Exception {
        File file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

        FlowDefinitionResource result = sut.createResource(file);

        assertThat(result.getId(), is("flowWithDependentBeans"));
        assertThat(result.getPath().getFile(), is(file.getAbsoluteFile()));
        assertThat(result.getPath().getDescription(), startsWith("file ["));
    }

    @Test
    public void shouldCreateFlowDefinitionResourceFromClasspathString() throws Exception {
        String resource = "/simpleFlows/standaloneFlow.xml";

        FlowDefinitionResource result = sut.createResource(resource);

        assertThat(result.getId(), is("standaloneFlow"));
        assertThat(result.getPath().getDescription(), is("class path resource [simpleFlows/standaloneFlow.xml]"));
    }

    @Test
    public void shouldCreateFlowDefinitionResourceByCallingToStringOnObject() throws Exception {
        FlowDefinitionResource result = sut.createResource(Arrays.asList("content"));

        assertThat(result.getId(), is("[content]"));
        assertThat(result.getPath().getDescription(), is("class path resource [[content]]"));
    }

    @Test
    public void shouldDeriveIdByResolvingWithBasePath() throws Exception {
        sut = new ExternalizedMockFlowConfiguration("resources/simpleFlows/standaloneFlow.xml");

        sut.withBasePath("src/test/");

        FlowDefinitionResource result = sut.getResource();
        assertThat(result.getId(), is("resources/simpleFlows"));
    }
}
