import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.Language;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import br.org.cria.splinkerapp.controllers.AbstractController;
import br.org.cria.splinkerapp.managers.EventBusManager;
import com.google.common.eventbus.Subscribe;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import br.org.cria.splinkerapp.i18n.CountryFlag;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalizedViewsTest {
    public static class SelectorController extends AbstractController {
        int received;

        @Override protected void setScreensize() { }
        @FXML public void onYesButtonClicked() { }
        @Subscribe public void receive(String event) { received++; }
    }

    @BeforeClass
    public static void startJavaFx() throws Exception {
        var started = new CompletableFuture<Void>();
        Platform.startup(() -> started.complete(null));
        started.get(15, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
    }

    @Test
    public void allLayoutsLoadInEveryLanguageWithoutRunningBusinessActions() throws Exception {
        var completed = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try (var files = Files.list(Path.of("src/main/resources/br/org/cria/splinkerapp"))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".fxml")).toList()) {
                    // Controllers perform network requests and start transfers; test the actual
                    // FXML/resources in isolation, leaving those operations out of a UI test.
                    String fxml = Files.readString(file)
                            .replaceAll("\\s+fx:controller=\"[^\"]+\"", "")
                            .replaceAll("\\s+on\\w+=\"#[^\"]+\"", "");
                    var url = getClass().getResource("/br/org/cria/splinkerapp/" + file.getFileName());
                    for (Language language : Language.values()) {
                        FXMLLoader loader = new FXMLLoader(url, I18n.bundle(language));
                        Parent root = loader.load(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
                        assertNotNull(file + ": " + language, root);
                        new Scene(root);
                        root.applyCss();
                        root.layout();
                    }
                }
                completed.complete(null);
            } catch (Throwable e) {
                completed.completeExceptionally(e);
            }
        });
        completed.get(40, TimeUnit.SECONDS);
    }

    @Test
    public void injectsInheritedSelectorAndReleasesSubscriptionsOnViewDisposal() throws Exception {
        var completed = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try {
                var loader = LocalizedViews.loader(getClass().getResource("/br/org/cria/splinkerapp/first-config-dialog.fxml"));
                var controller = new SelectorController();
                loader.setControllerFactory(type -> controller);
                Parent root = LocalizedViews.load(loader);
                MenuButton selector = (MenuButton) loader.getNamespace().get("languageSelector");
                assertEquals(3, selector.getItems().size());
                assertTrue(selector.getGraphic() instanceof CountryFlag);
                assertEquals(I18n.getLanguage().toString(), selector.getItems().get(I18n.getLanguage().ordinal()).getText());
                assertTrue(selector.getItems().stream().allMatch(item -> item.getGraphic() instanceof CountryFlag));
                var bus = EventBusManager.getEvent("i18n-view-disposal-test");
                bus.register(controller);
                bus.post("before");
                assertEquals(1, controller.received);
                LocalizedViews.dispose(root);
                bus.post("after");
                assertEquals(1, controller.received);
                completed.complete(null);
            } catch (Throwable e) {
                completed.completeExceptionally(e);
            }
        });
        completed.get(15, TimeUnit.SECONDS);
    }
}
