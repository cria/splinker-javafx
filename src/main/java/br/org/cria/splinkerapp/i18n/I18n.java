package br.org.cria.splinkerapp.i18n;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/** All UI translations live in the UTF-8 messages.properties catalog. */
public final class I18n {
    private static final Properties MESSAGES = loadMessages();
    private static volatile Language language = loadLanguage();

    private I18n() { }

    private static Properties loadMessages() {
        Properties messages = new Properties();
        try (var stream = I18n.class.getResourceAsStream("/messages.properties")) {
            if (stream == null) throw new IllegalStateException("Missing messages.properties");
            messages.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return messages;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Language loadLanguage() {
        try {
            return Language.fromCode(preferences().get("language", "pt"));
        } catch (SecurityException e) {
            return Language.PORTUGUESE;
        }
    }

    private static Preferences preferences() {
        return Preferences.userNodeForPackage(I18n.class);
    }

    public static Language getLanguage() { return language; }

    public static void setLanguage(Language selected) {
        if (selected == null) throw new IllegalArgumentException("Language is required");
        language = selected;
        try {
            preferences().put("language", selected.code());
        } catch (SecurityException e) {
            // Translation remains available when preferences cannot be persisted.
        }
    }

    public static String text(String key) {
        return text(language, key);
    }

    public static String text(Language selected, String key) {
        String value = MESSAGES.getProperty(selected.code() + "." + key,
                MESSAGES.getProperty("pt." + key));
        if (value == null) throw new MissingResourceException("Missing translation", I18n.class.getName(), key);
        return value;
    }

    /** Snapshot so every control in an FXML load uses the same language. */
    public static ResourceBundle bundle() {
        return bundle(language);
    }

    public static ResourceBundle bundle(Language selected) {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) { return text(selected, key); }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(MESSAGES.stringPropertyNames().stream()
                        .filter(key -> key.startsWith("pt."))
                        .map(key -> key.substring(3)).toList());
            }
        };
    }
}
