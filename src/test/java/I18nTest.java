import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.Language;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.*;

public class I18nTest {
    @Test
    public void catalogContainsEveryLanguageAndNoDuplicateKeys() throws Exception {
        var seen = new HashSet<Object>();
        Properties messages = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                assertTrue("Duplicate translation: " + key, seen.add(key));
                return super.put(key, value);
            }
        };
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/messages.properties"), StandardCharsets.UTF_8)) {
            messages.load(reader);
        }
        for (String key : messages.stringPropertyNames()) {
            String suffix = key.substring(key.indexOf('.') + 1);
            for (Language language : Language.values()) {
                String translated = messages.getProperty(language.code() + "." + suffix);
                assertNotNull(language + ": " + suffix, translated);
                assertFalse(suffix, translated.isBlank());
                assertFalse(suffix, translated.contains("Ã"));
                // Validate printf placeholders used by progress/error messages.
                assertEquals(suffix, messages.getProperty("pt." + suffix).split("%s", -1).length,
                        translated.split("%s", -1).length);
            }
        }
    }

    @Test
    public void everyFxmlAndJavaTranslationReferenceExists() throws Exception {
        Pattern fxmlKeys = Pattern.compile("=\"%([^\"]+)\"");
        Pattern javaKeys = Pattern.compile("I18n\\.text\\(\"([^\"]+)\"\\)");
        try (var paths = Files.walk(Path.of("src/main"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String name = path.toString();
                if (!name.endsWith(".java") && !name.endsWith(".fxml")) continue;
                var matcher = (name.endsWith(".fxml") ? fxmlKeys : javaKeys)
                        .matcher(Files.readString(path));
                while (matcher.find()) {
                    for (Language language : Language.values()) {
                        assertNotNull(path + ": " + matcher.group(1), I18n.text(language, matcher.group(1)));
                    }
                }
            }
        }
    }

    @Test
    public void preservesAccentsLineBreaksAndInterpolation() {
        assertEquals("Configurações", I18n.text(Language.PORTUGUESE, "settings"));
        assertEquals("Español", Language.SPANISH.toString());
        assertTrue(I18n.text(Language.ENGLISH, "stage.start.running").endsWith("\n"));
        assertTrue(I18n.text(Language.ENGLISH, "transfer.error").formatted("data import", "123").contains("123"));
        assertEquals("Failed to send email: ", I18n.text(Language.ENGLISH, "email.failed"));
        assertEquals(Language.PORTUGUESE, Language.fromCode("invalid"));
        assertEquals(Language.ENGLISH, Language.fromCode("en"));
    }
}
