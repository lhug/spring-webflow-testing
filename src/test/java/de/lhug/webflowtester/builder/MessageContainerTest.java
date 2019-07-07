package de.lhug.webflowtester.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import de.lhug.webflowtester.builder.MessageContainer;
import de.lhug.webflowtester.builder.MessageContainer.Message;
import de.lhug.webflowtester.builder.MessageContainer.Messages;

public class MessageContainerTest {

    private MessageContainer sut = new MessageContainer();

    @Test
    public void shouldReturnMessagesMap() throws Exception {
        Map<Locale, Messages> result = sut.getAllMessages();

        assertThat(result, is(not(nullValue())));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void messagesMapShouldBeUnmodifiable() throws Exception {
        Map<Locale, Messages> messages = sut.getAllMessages();

        messages.put(Locale.CHINESE, new Messages());
    }

    @Test
    public void shouldReturnEmptyMessagesObjectWhenNoMessagesForLocaleHaveBeenAdded() throws Exception {
        Messages result = sut.getMessages(Locale.TAIWAN);

        assertThat(result.messageStore, is(empty()));
    }

    @Test
    public void shouldAddMessageForLocale() throws Exception {
        assertThat(sut.getAllMessages().size(), is(0));

        sut.addMessage(Locale.PRC, "key", "value");

        Map<Locale, Messages> allMessages = sut.getAllMessages();
        assertThat(allMessages.size(), is(1));
        assertThat(allMessages.get(Locale.PRC).messageStore, contains(new Message("key", "value")));
    }

    @Test
    public void shouldAllowAddingMessagesForLocale() throws Exception {
        Messages messages = sut.getMessages(Locale.CHINESE);

        messages.addMessage("left", "right");

        assertThat(messages.messageStore, contains(new Message("left", "right")));
    }

    @Test
    public void shouldAllowAddingMultipleMessagesForLocale() throws Exception {
        Messages messages = sut.getMessages(Locale.GERMANY);

        messages
                .addMessage("one", "two")
                .addMessage("three", "four");

        assertThat(messages.messageStore, containsInAnyOrder(
                new Message("one", "two"),
                new Message("three", "four")));
    }

    @Test
    public void shouldAllowAddingMultipleMessagesWithLocale() throws Exception {
        Map<String, String> messages = new HashMap<>();
        messages.put("a", "b");
        messages.put("c", "d");

        sut.addMessages(Locale.CANADA_FRENCH, messages);

        assertThat(sut.getMessages(Locale.CANADA_FRENCH).messageStore, containsInAnyOrder(
                new Message("a", "b"),
                new Message("c", "d")));
    }

}
