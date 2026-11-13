package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.LocalizedViews;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class HomeControllerTest {
    @BeforeClass public static void startJavaFx() throws Exception {
        var ready = new CompletableFuture<Void>();
        try { Platform.startup(() -> ready.complete(null)); }
        catch (IllegalStateException started) { Platform.runLater(() -> ready.complete(null)); }
        ready.get(15, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        var result = new CompletableFuture<T>();
        Platform.runLater(() -> {
            try { result.complete(action.call()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result.get(15, TimeUnit.SECONDS);
    }

    private HomeController loadHome() throws Exception {
        // Keep the actual Home FXML and lifecycle, without database or API access.
        var controller = new HomeController() {
            @Override protected void loadPage(String pageName) { }
        };
        var loader = LocalizedViews.loader(getClass().getResource("/br/org/cria/splinkerapp/home.fxml"));
        loader.setControllerFactory(type -> controller);
        Parent root = LocalizedViews.load(loader);
        new Scene(root);
        return controller;
    }

    @Test public void returningHomeAppliesItsSizeAfterSceneAttachment() throws Exception {
        var stage = onFx(() -> {
            var window = new Stage();
            window.setX(Screen.getPrimary().getVisualBounds().getMinX());
            window.setY(Screen.getPrimary().getVisualBounds().getMinY());
            window.setScene(new Scene(new Pane()));
            window.setMinWidth(620);
            window.setMinHeight(600);
            window.setWidth(740);
            window.setHeight(680);
            window.setResizable(true);
            var home = loadHome();
            window.setScene(home.pane.getScene());
            return window;
        });
        try {
            onFx(() -> {
                var bounds = Screen.getPrimary().getVisualBounds();
                assertFalse(stage.isResizable());
                assertEquals(Math.min(940, bounds.getWidth()), stage.getWidth(), 0.1);
                assertEquals(Math.min(660, bounds.getHeight()), stage.getHeight(), 0.1);
                assertEquals(0, stage.getMinWidth(), 0);
                assertEquals(0, stage.getMinHeight(), 0);
                return null;
            });
        } finally { onFx(() -> { stage.close(); return null; }); }
    }

    @Test public void staleHomeCallbackDoesNotResizeAnotherScreen() throws Exception {
        var window = onFx(() -> {
            var stage = new Stage();
            stage.setScene(loadHome().pane.getScene());
            stage.setScene(new Scene(new Pane()));
            stage.setWidth(740);
            stage.setHeight(680);
            return stage;
        });
        try {
            onFx(() -> {
                assertEquals(740, window.getWidth(), 0.1);
                assertEquals(680, window.getHeight(), 0.1);
                return null;
            });
        } finally { onFx(() -> { window.close(); return null; }); }
    }

    @Test public void gettingStagePreservesResizableState() throws Exception {
        onFx(() -> {
            var controller = new HomeController();
            controller.pane = new Pane();
            var stage = new Stage();
            try {
                stage.setScene(new Scene(controller.pane));
                stage.setResizable(true);
                assertSame(stage, controller.getStage());
                assertTrue(stage.isResizable());
            } finally { stage.close(); }
            return null;
        });
    }
}
