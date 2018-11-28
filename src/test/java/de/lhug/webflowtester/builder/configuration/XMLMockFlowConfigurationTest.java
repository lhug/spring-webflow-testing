package de.lhug.webflowtester.builder.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.webflow.config.FlowDefinitionResource;

public class XMLMockFlowConfigurationTest {

    private XMLMockFlowConfiguration sut;

    @Before
    public void setUp() {
        sut = new XMLMockFlowConfiguration("/inheritanceFlows/childFlow.xml");
    }

    @Test
    public void shouldCreateConfigurationFromUrl() throws Exception {
        URL url = getClass().getResource("/simpleFlows/standaloneFlow.xml");

        sut = new XMLMockFlowConfiguration(url);

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("standaloneFlow"));
        assertThat(mainResource.getPath().getURL(), is(url));
        assertThat(mainResource.getPath().getDescription(), startsWith("URL [file:"));
    }

    @Test
    public void shouldCreateConfigurationFromFile() throws Exception {
        File file = new File("src/test/resources/simpleFlows/flowWithDependentBeans.xml");

        sut = new XMLMockFlowConfiguration(file);

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("flowWithDependentBeans"));
        assertThat(mainResource.getPath().getFile(), is(file.getAbsoluteFile()));
        assertThat(mainResource.getPath().getDescription(), startsWith("file ["));
    }

    @Test
    public void shouldCreateConfigurationFromClasspath() throws Exception {
        String resource = "/simpleFlows/standaloneFlow.xml";

        sut = new XMLMockFlowConfiguration(resource);

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("standaloneFlow"));
        assertThat(mainResource.getPath().getDescription(), is("class path resource [simpleFlows/standaloneFlow.xml]"));
    }

    @Test
    public void shouldCreateConfigurationWithFallingBacktoString() throws Exception {
        sut = new XMLMockFlowConfiguration(Arrays.asList("content, more content"));

        FlowDefinitionResource mainResource = sut.getResource();
        assertThat(mainResource.getId(), is("[content, more content]"));
        assertThat(mainResource.getPath().getDescription(), is("class path resource [[content, more content]]"));
    }

    @Test
    public void shouldAddParentResourceFromURL() throws Exception {
        URL parentFlow = getClass().getResource("/inheritanceFlows/parentFlow.xml");

        sut.addParentFlow(parentFlow);

        assertThat(sut.getFlowResources(), contains(
                hasProperty("path",
                        hasProperty("URL", is(parentFlow)))));
    }

    @Test
    public void shouldAddParentResourceFromFile() throws Exception {
        File file = new File("src/test/resources/inheritanceFlows/parentFlow.xml");

        sut.addParentFlow(file);

        assertThat(sut.getFlowResources(), contains(
                hasProperty("path",
                        hasProperty("file", is(file.getAbsoluteFile())))));
    }

    @Test
    public void shouldAddParentResourceFromClasspathString() throws Exception {
        String classpath = "/inheritanceFlows/parentFlow.xml";

        sut.addParentFlow(classpath);

        assertThat(sut.getFlowResources(), contains(
                hasProperty("path",
                        hasProperty("description", is("class path resource [inheritanceFlows/parentFlow.xml]")))));
    }

    @Test
    public void shouldAddParentResourceWithFallingBackToString() throws Exception {
        sut.addParentFlow(Arrays.asList("seven, nineteen"));

        assertThat(sut.getFlowResources(), contains(
                hasProperty("path",
                        hasProperty("description", is("class path resource [[seven, nineteen]]")))));
    }

    @Test
    public void shouldAddMultipleParentResources() throws Exception {
        sut.addParentFlow("/inheritanceFlows/parentFlow.xml");
        sut.addParentFlow("/simpleFlows/standaloneFlow.xml");

        List<FlowDefinitionResource> result = sut.getFlowResources();

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getId(), is("parentFlow"));
        assertThat(result.get(1).getId(), is("standaloneFlow"));
    }

    @Test
    public void shouldReturnEmptyListWhenNoParentResourcesWereAdded() throws Exception {
        assertThat(sut.getFlowResources(), is(empty()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableListOfResources() throws Exception {
        List<FlowDefinitionResource> result = sut.getFlowResources();

        result.add(mock(FlowDefinitionResource.class));
    }

    @Test
    public void shouldDeriveIdByResolvingWithBasePath() throws Exception {
        sut.addParentFlow("resources/inheritanceFlows/parentFlow.xml");

        sut.withBasePath("src/test");

        List<FlowDefinitionResource> result = sut.getFlowResources();
        assertThat(result, contains(hasProperty("id", is("resources/inheritanceFlows"))));
    }
}
