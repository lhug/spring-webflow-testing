package de.lhug.webflowtester.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Convenience container class used to pass preconfigured messages to the
 * {@link ApplicationContext}
 * <p>
 * All messages are grouped by passed locale. However, the added messages are
 * not supposed to be read from outside sources, so only add-methods are
 * exposed.
 * </p>
 * <p>
 * A notable exception is {@link #getMessages(Locale)} which allows direct
 * access to the underlying {@link Messages} object. This accessor exists solely
 * to allow easy chaining of {@link Messages#addMessage(String, String)
 * addMessage} calls, if no bulk-adding is desired.
 * </p>
 * <p>
 * Examples of use:
 * </p>
 * 
 * <pre>
 * container.getMessages(Locale.GERMANY)
 *   .addMessage("message.key", "message")
 *   .addMessage("message.other", "another message);
 * 
 * container.addMessage(Locale.GERMANY, "some.key", "some message");
 * 
 * container.addMessages(Locale.GERMANY, map);
 * </pre>
 * 
 * @see #addMessage(Locale, String, String)
 * @see #addMessages(Locale, Map)
 * @see #getMessages(Locale)
 * @see Messages#addMessage(String, String)
 */
public class MessageContainer {

    /**
     * Simple container class to store and add {@link Message} objects.
     * <p>
     * This class is used to store key-value-pairs for messages to be registered in
     * the {@link StaticMessageSource} for the flow to be tested.
     * </p>
     *
     */
    @RequiredArgsConstructor
    public static class Messages {
        final Set<Message> messageStore = new HashSet<>();

        /**
         * Convenience method to add {@link Message} objects to this {@link Messages}
         * container.
         * <p>
         * This method does not do any validation, meaning it accepts {@code null} but
         * that will most likely lead to {@link NullPointerException}s later during
         * execution, although it is not guaranteed to happen.
         * </p>
         * <p>
         * This method returns {@code this} to allow for easy chaining of calls.
         * </p>
         * 
         * @param key   the String to register the message with
         * @param value the String to be associated with the passed {@code key}
         * @return {@code this}
         */
        public Messages addMessage(String key, String value) {
            this.messageStore.add(new Message(key, value));
            return this;
        }
    }

    /**
     * Simple container class for message key and value
     *
     */
    @Value
    public static class Message {
        final String key;
        final String value;
    }

    private final Map<Locale, Messages> messages = new HashMap<>();

    /**
     * Returns all registered {@link Messages} associated to their respective
     * {@link Locale}s.
     * <p>
     * The returned {@link Map} is an {@link Collections#unmodifiableMap(Map)
     * unmodifiable} view of the registered messages.
     * </p>
     * 
     * @return an unmodifiable {@code Map} containing all registered
     *         {@code Messages}
     */
    public Map<Locale, Messages> getAllMessages() {
        return Collections.unmodifiableMap(messages);
    }

    /**
     * Returns the {@link Messages} object registered for this {@link Locale}.
     * <p>
     * If no {@link Messages} object is registered, a new one will be created and
     * added.
     * </p>
     * 
     * @param locale the Locale the {@link Message}s will be registered with
     * @return an instance of {@link Messages}, never {@code null}
     */
    public final Messages getMessages(Locale locale) {
        return getOrInitMessagesFor(locale);
    }

    /**
     * Adds a single {@link Message} for the given {@link Locale}
     * <p>
     * More formally: This fetches the {@link Messages} for the desired
     * {@link Locale} and then {@link Messages#addMessage(String, String) adds} the
     * {@code key} and {@code value} arguments as {@link Message}.
     * </p>
     * 
     * @param locale the Locale the messages will be associated to
     * @param key    the String to register the message with
     * @param value  the String to be associated with the passed {@code key}
     */
    public void addMessage(Locale locale, String key, String value) {
        Messages localMessages = getMessages(locale);
        localMessages.addMessage(key, value);
    }

    private Messages getOrInitMessagesFor(Locale locale) {
        messages.putIfAbsent(locale, new Messages());
        return messages.get(locale);
    }

    /**
     * Adds multiple {@link Message} objects for the given {@link Locale}
     * <p>
     * More formally: this fetches the {@link Messages} for the desired
     * {@link Locale} and then {@link Messages#addMessage(String, String) adds} each
     * {@link Entry} as {@link Message}.
     * </p>
     * 
     * @param locale the Locale the messages will be associated to
     * @param values the Map providing key-value pairs to be converted to
     *               {@code Message}s
     */
    public void addMessages(Locale locale, Map<String, String> values) {
        Messages localMessages = getMessages(locale);
        values.entrySet().stream()
                .map(e -> new Message(e.getKey(), e.getValue()))
                .forEach(localMessages.messageStore::add);
    }
}
