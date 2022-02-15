package de.lhug.webflowtester.builder.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

import de.lhug.webflowtester.builder.MessageContainer.Message;
import de.lhug.webflowtester.builder.MessageContainer.Messages;
import de.lhug.webflowtester.stub.StubFlow;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.webflow.definition.registry.FlowDefinitionHolder;

public class FlowTestContextTest {

	private FlowTestContext sut = new FlowTestContext();

	private final TestBean offer = new TestBean("default bean", 4);

	@Getter
	@AllArgsConstructor
	public static class TestBean {
		private final String name;
		private final int number;
	}

	@Test
	public void shouldReportFalseWhenNoBeanHasBeenAdded() {
		boolean result = sut.containsBean(offer);

		assertThat(result).isFalse();
	}

	@Test
	public void shouldReportTrueWhenBeanHasBeenAdded() {
		sut.addBean(offer);

		boolean result = sut.containsBean(offer);

		assertThat(result).isTrue();
	}

	@Test
	public void shouldReportFalseWhenBeanWithGivenNameDoesNotExist() {
		boolean result = sut.containsBeanWithName("testBean");

		assertThat(result).isFalse();
	}

	@Test
	public void shouldReportTrueWhenBeanWithGivenNameHasBeenAdded() {
		sut.addBean("testBean", offer);

		boolean result = sut.containsBeanWithName("testBean");

		assertThat(result).isTrue();
	}

	@Test
	public void shouldAddTestBeanWithGeneratedName() {
		sut.addBean(offer);

		boolean result = sut.containsBeanWithName("testBean");

		assertThat(result).isTrue();
	}

	@Test
	public void shouldAddBeanWithGeneratedName() {
		List<String> bean = new ArrayList<>();
		bean.add("content");
		sut.addBean(bean);

		boolean result = sut.containsBeanWithName("stringList");

		assertThat(result).isTrue();
	}

	@Test
	public void shouldAddBeanWithGivenName() {
		List<String> bean = new ArrayList<>();
		bean.add("content");
		sut.addBean("strings", bean);

		boolean result = sut.containsBeanWithName("strings");

		assertThat(result).isTrue();
	}

	@Test
	public void shouldReturnViewOfRegisteredBeans() {
		sut.addBean(offer);

		Map<String, Object> result = sut.getBeans();

		assertThat(result)
				.containsExactly(entry("testBean", offer));
	}

	@Test
	public void shouldReturnUnmodifiableViewOfRegisteredBeans() {
		Map<String, Object> result = sut.getBeans();

		assertThatThrownBy(() -> result.put("key", "value"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void shouldCreateContextWithMultipleRegisteredBeansWithGeneratedNames() {
		List<String> strings = List.of("string");
		TestBean testBean = new TestBean("name", 4);

		sut = new FlowTestContext(strings, testBean);

		Map<String, Object> result = sut.getBeans();

		assertThat(result)
				.containsExactly(
						entry("stringList", strings),
						entry("testBean", testBean)
				);
	}

	@Test
	public void shouldReturnEmptyListWhenCalledBeforeAddingSubFlows() {
		List<FlowDefinitionHolder> result = sut.getSubFlows();

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldReturnUnmodifiableListOfSubFlows() {
		List<FlowDefinitionHolder> subFlows = sut.getSubFlows();

		var mock = mock(FlowDefinitionHolder.class);

		assertThatThrownBy(() -> subFlows.add(mock))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void shouldAddSubFlow() {
		StubFlow stub = new StubFlow("flow", "end");

		sut.addSubFlow(stub);

		assertThat(sut.getSubFlows()).containsExactly(stub);
	}

	@Test
	public void shouldReturnEmptyMessagesWhenNoMessagesAreConfigured() throws Exception {
		Messages results = sut.getMessages(Locale.GERMANY);

		var result = extractMessages(results);
		assertThat(result).isEmpty();

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

		var messages = extractMessages(sut.getMessages(Locale.CHINA));
		assertThat(messages).containsExactly(new Message("xi", "jinping"));
	}

	@Test
	public void shouldAddMultipleMessagesWithDesiredLocale() throws Exception {
		Map<String, String> values = new HashMap<>();
		values.put("winnie", "pooh bear");
		values.put("xi", "jinping");

		sut.addMessages(Locale.CHINESE, values);

		Set<Message> messages = extractMessages(sut.getMessages(Locale.CHINESE));
		assertThat(messages)
				.containsOnly(
						new Message("winnie", "pooh bear"),
						new Message("xi", "jinping")
				);
	}

	@Test
	public void shouldReturnAllRegisteredMessagesAsMap() throws Exception {
		sut.addMessage(Locale.GERMAN, "Glas", "Wasser");
		sut.addMessage(Locale.FRENCH, "verre", "eau");

		Map<Locale, Messages> result = sut.getAllMessages();

		assertThat(result).hasSize(2);
		assertThat(extractMessages(result.get(Locale.GERMAN))).containsExactly(new Message("Glas", "Wasser"));
		assertThat(extractMessages(result.get(Locale.FRENCH))).containsExactly(new Message("verre", "eau"));
	}

	@Test
	public void returnedMessagesMapShouldBeUnmodifiable() {
		Map<Locale, Messages> result = sut.getAllMessages();

		assertThatThrownBy(() -> result.put(Locale.GERMAN, new Messages()))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
