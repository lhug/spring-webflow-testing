package de.lhug.webflowtester.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lhug.webflowtester.builder.MessageContainer.Message;
import de.lhug.webflowtester.builder.MessageContainer.Messages;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageContainerTest {

	private final MessageContainer sut = new MessageContainer();

	@Test
	void shouldReturnMessagesMap() {
		var result = sut.getAllMessages();

		assertThat(result).isNotNull();
	}

	@Test
	void messagesMapShouldBeUnmodifiable() {
		var results = sut.getAllMessages();

		var messages = new Messages();
		assertThatThrownBy(() -> results.put(Locale.CHINESE, messages))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldReturnEmptyMessagesObjectWhenNoMessagesForLocaleHaveBeenAdded() {
		var result = sut.getMessages(Locale.TAIWAN);

		assertThat(result.messageStore).isEmpty();
	}

	@Test
	void shouldAddMessageForLocale() {
		assertThat(sut.getAllMessages()).isEmpty();

		sut.addMessage(Locale.PRC, "key", "value");

		var allMessages = sut.getAllMessages();

		assertThat(allMessages).hasSize(1);
		assertThat(allMessages.get(Locale.PRC).messageStore).containsExactly(new Message("key", "value"));
	}

	@Test
	void shouldAllowAddingMessagesForLocale() {
		var messages = sut.getMessages(Locale.CHINESE);

		messages.addMessage("left", "right");

		assertThat(messages.messageStore).containsExactly(new Message("left", "right"));
	}

	@Test
	void shouldAllowAddingMultipleMessagesForLocale() {
		var messages = sut.getMessages(Locale.GERMANY);

		messages
				.addMessage("one", "two")
				.addMessage("three", "four");

		assertThat(messages.messageStore).containsOnly(
				new Message("one", "two"),
				new Message("three", "four"));
	}

	@Test
	void shouldAllowAddingMultipleMessagesWithLocale() {
		var messages = Map.ofEntries(
				Map.entry("a", "b"),
				Map.entry("c", "d"));

		sut.addMessages(Locale.CANADA_FRENCH, messages);

		assertThat(sut.getMessages(Locale.CANADA_FRENCH).messageStore).containsOnly(
				new Message("a", "b"),
				new Message("c", "d"));
	}

}
