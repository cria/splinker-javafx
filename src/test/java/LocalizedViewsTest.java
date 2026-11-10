import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.Language;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import br.org.cria.splinkerapp.controllers.AbstractController;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.utils.SupportBrowser;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
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
    public void supportBrowserRendersStyledContentInsideTheMainView() throws Exception {
        var completed = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try {
                var browser = new SupportBrowser();
                var host = new javafx.scene.layout.StackPane(browser);
                new Scene(host, 700, 500);
                assertNull(browser.lookup("#openExternalBrowser"));
                var error = browser.lookup("#supportLoadError");
                assertFalse(error.isVisible());
                var webView = (WebView) browser.lookup("#supportWebView");
                var engine = webView.getEngine();
                assertSame(engine, engine.getCreatePopupHandler().call(null));
                var handled = new java.util.concurrent.atomic.AtomicBoolean();
                engine.getLoadWorker().stateProperty().addListener((observable, previous, state) -> {
                    if (state == Worker.State.SUCCEEDED && handled.compareAndSet(false, true)) {
                        try {
                            assertEquals("Local preview", engine.executeScript("document.body.textContent.trim()"));
                            assertEquals("grid", engine.executeScript("getComputedStyle(document.body).display"));
                            assertEquals("rgb(0, 129, 198)", engine.executeScript("getComputedStyle(document.body).color"));
                            assertFalse(error.isVisible());
                            host.getChildren().clear();
                            assertNull(browser.getScene());
                            assertFalse(engine.getLoadWorker().isRunning());
                            completed.complete(null);
                        } catch (Throwable e) {
                            browser.close();
                            completed.completeExceptionally(e);
                        }
                    } else if (state == Worker.State.FAILED && handled.compareAndSet(false, true)) {
                        var failure = engine.getLoadWorker().getException();
                        browser.close();
                        completed.completeExceptionally(failure);
                    }
                });
                // The donation site uses Tailwind 4: CSS layers, nesting and registered properties.
                engine.loadContent("""
                        <html><head><style>
                        @property --donation-color { syntax: '<color>'; inherits: false; initial-value: #0081c6; }
                        @layer utilities { body { display: grid; & { color: var(--donation-color); } } }
                        </style></head><body>Local preview</body></html>
                        """);
            } catch (Throwable e) {
                completed.completeExceptionally(e);
            }
        });
        completed.get(20, TimeUnit.SECONDS);
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
                assertNull(selector.getGraphic());
                assertEquals(I18n.getLanguage().code().toUpperCase(java.util.Locale.ROOT), selector.getText());
                assertEquals(I18n.getLanguage().toString(), selector.getItems().get(I18n.getLanguage().ordinal()).getText());
                assertTrue(selector.getItems().stream().allMatch(item -> item.getGraphic() == null));
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
