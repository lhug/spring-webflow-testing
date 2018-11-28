package de.lhug.webflowtester.builder.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;

import de.lhug.webflowtester.stub.StubFlow;
import lombok.Value;

public class FlowTestContextTest {

    private FlowTestContext sut = new FlowTestContext();

    private TestBean offer = new TestBean("default bean", 4);

    @Value
    public class TestBean {
        private final String name;
        private final int number;
    }

    @Test
    public void shouldReportFalseWhenNoBeanHasBeenAdded() throws Exception {
        boolean result = sut.containsBean(offer);

        assertThat(result, is(false));
    }

    @Test
    public void shouldReportTrueWhenBeanHasBeenAdded() throws Exception {
        sut.addBean(offer);

        boolean result = sut.containsBean(offer);

        assertThat(result, is(true));
    }

    @Test
    public void shouldReportFalseWhenBeanWithGivenNameDoesNotExist() throws Exception {
        boolean result = sut.containsBeanWithName("testBean");

        assertThat(result, is(false));
    }

    @Test
    public void shouldReportTrueWhenBeanWithGivenNameHasBeenAdded() throws Exception {
        sut.addBean("testBean", offer);

        boolean result = sut.containsBeanWithName("testBean");

        assertThat(result, is(true));
    }

    @Test
    public void shouldAddTestBeanWithGeneratedName() throws Exception {
        sut.addBean(offer);

        boolean result = sut.containsBeanWithName("testBean");

        assertThat(result, is(true));
    }

    @Test
    public void shouldAddBeanWithGeneratedName() throws Exception {
        List<String> bean = new ArrayList<>();
        bean.add("content");
        sut.addBean(bean);

        boolean result = sut.containsBeanWithName("stringList");

        assertThat(result, is(true));
    }

    @Test
    public void shouldAddBeanWithGivenName() throws Exception {
        List<String> bean = new ArrayList<>();
        bean.add("content");
        sut.addBean("strings", bean);

        boolean result = sut.containsBeanWithName("strings");

        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnViewOfRegisteredBeans() throws Exception {
        sut.addBean(offer);

        Map<String, Object> result = sut.getBeans();

        assertThat(result, hasEntry("testBean", offer));
        assertThat(result.size(), is(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnmodifiableViewOfRegisteredBeans() throws Exception {
        Map<String, Object> result = sut.getBeans();

        result.put("key", "value");
    }

    @Test
    public void shouldCreateContextWithMultipleRegisteredBeansWithGeneratedNames() throws Exception {
        List<String> strings = Arrays.asList("string");
        TestBean testBean = new TestBean("name", 4);

        sut = new FlowTestContext(strings, testBean);

        Map<String, Object> result = sut.getBeans();

        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("stringList", strings));
        assertThat(result, hasEntry("testBean", testBean));
    }

    @Test
    public void shouldReturnEmptyListWhenCalledBeforeAddingSubflows() throws Exception {
        List<FlowDefinitionHolder> result = sut.getSubFlows();

        assertThat(result, is(empty()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testName() throws Exception {
        sut.getSubFlows().add(mock(FlowDefinitionHolder.class));
    }

    @Test
    public void shouldAddSubFlow() throws Exception {
        StubFlow stub = new StubFlow("flow", "end");

        sut.addSubFlow(stub);

        assertThat(sut.getSubFlows(), contains(sameInstance(stub)));
    }
}
