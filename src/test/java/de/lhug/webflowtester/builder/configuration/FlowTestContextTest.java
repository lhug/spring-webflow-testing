package de.lhug.webflowtester.builder.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;

import de.lhug.webflowtester.builder.MessageContainer.Message;
import de.lhug.webflowtester.builder.MessageContainer.Messages;
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
    public void shouldReturnUnmodifiableListOfSubFlows() throws Exception {
        List<FlowDefinitionHolder> subFlows = sut.getSubFlows();

        subFlows.add(mock(FlowDefinitionHolder.class));
    }

    @Test
    public void shouldAddSubFlow() throws Exception {
        StubFlow stub = new StubFlow("flow", "end");

        sut.addSubFlow(stub);

        assertThat(sut.getSubFlows(), contains(sameInstance(stub)));
    }

    @Test
    public void shouldReturnEmptyMessagesWhenNoMessagesAreConfigured() throws Exception {
        Messages result = sut.getMessages(Locale.GERMANY);

        assertThat(extractMessages(result), is(empty()));

    }

    @SuppressWarnings("unchecked")
    private Set<Message> extractMessages(Messages offer) throws Exception {
        Field field = Messages.class.getDeclaredField("messageStore");
        field.setAccessible(true);
        return (Set<Message>) field.get(offer);

    }

    @Test
    public void shouldAddSingleMessageWithDesiredLocale() throws Exception {
        sut.addMessage(Locale.CHINA, "xi", "jinping");

        Set<Message> messages = extractMessages(sut.getMessages(Locale.CHINA));
        assertThat(messages,
                contains(new Message("xi", "jinping")));
    }

    @Test
    public void shouldAddMultipleMessagesWithDesiredLocale() throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("winnie", "pooh bear");
        values.put("xi", "jinping");

        sut.addMessages(Locale.CHINESE, values);

        Set<Message> messages = extractMessages(sut.getMessages(Locale.CHINESE));
        assertThat(messages, containsInAnyOrder(
                new Message("winnie", "pooh bear"),
                new Message("xi", "jinping")));
    }

    @Test
    public void shouldReturnAllRegisteredMessagesAsMap() throws Exception {
        sut.addMessage(Locale.GERMAN, "glas", "wasser");
        sut.addMessage(Locale.FRENCH, "verre", "eau");

        Map<Locale, Messages> result = sut.getAllMessages();

        assertThat(result.size(), is(2));

        assertThat(extractMessages(result.get(Locale.GERMAN)), contains(new Message("glas", "wasser")));
        assertThat(extractMessages(result.get(Locale.FRENCH)), contains(new Message("verre", "eau")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void returnedMessagesMapShouldBeUnmodifiable() throws Exception {
        Map<Locale, Messages> result = sut.getAllMessages();

        result.put(Locale.GERMAN, new Messages());
    }
}
