package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.RepositoryTestBase;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.MetadataSyncService;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PrincipalControllerTest extends RepositoryTestBase {
    @BeforeClass public static void startJavaFx() throws Exception {
        var ready = new CompletableFuture<Void>();
        try {
            Platform.startup(() -> ready.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> ready.complete(null));
        }
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

    @Test public void showsLocalDataWithoutWaitingAndIgnoresPreviousCollectionResponse() throws Exception {
        DataSetService.saveDataSet("A", DataSourceType.CSV, "ACR-A", "Local A", 1);
        DataSetService.saveDataSet("B", DataSourceType.CSV, "ACR-B", "Local B", 2);
        TokenRepository.setCurrentToken("A");
        var first = new CompletableFuture<Void>();
        var second = new CompletableFuture<Void>();
        var retry = new CompletableFuture<Void>();
        var service = mock(MetadataSyncService.class);
        when(service.refresh("A", false)).thenReturn(first);
        when(service.refresh("B", false)).thenReturn(second, retry);
        PrincipalController controller = onFx(() -> {
            try (var singleton = mockStatic(MetadataSyncService.class)) {
                singleton.when(MetadataSyncService::getInstance).thenReturn(service);
                var loader = LocalizedViews.loader(getClass().getResource(
                        "/br/org/cria/splinkerapp/principal.fxml"));
                Parent root = LocalizedViews.load(loader);
                new Scene(root);
                root.applyCss();
                root.layout();
                PrincipalController loaded = loader.getController();
                assertNull(root.lookup("#btnRefreshMetadata"));
                assertEquals("Local A", loaded.lblCollectionName.getText());
                assertFalse(first.isDone());
                verify(service, times(1)).refresh("A", false);
                loaded.cmbCollection.setValue("ACR-B");
                assertEquals("Local B", loaded.lblCollectionName.getText());
                verify(service, times(1)).refresh("B", false);
                return loaded;
            }
        });
        try {
            first.complete(null);
            onFx(() -> {
                assertEquals("Local B", controller.lblCollectionName.getText());
                assertEquals(I18n.text("dashboard.refreshing"), controller.lblMetadataStatus.getText());
                return null;
            });
            second.completeExceptionally(new IOException("offline"));
            onFx(() -> {
                assertEquals("Local B", controller.lblCollectionName.getText());
                assertEquals(I18n.text("dashboard.refresh.failed"), controller.lblMetadataStatus.getText());
                try (var singleton = mockStatic(MetadataSyncService.class)) {
                    singleton.when(MetadataSyncService::getInstance).thenReturn(service);
                    controller.syncMetaData();
                    verify(service, times(2)).refresh("B", false);
                }
                return null;
            });
            var fields = new HashMap<String, String>();
            fields.put("token", "B");
            fields.put("dataset_name", "Server B");
            fields.put("dataset_acronym", "NEW-B");
            DataSetService.updateDataSource(fields);
            retry.complete(null);
            onFx(() -> {
                assertEquals("Server B", controller.lblCollectionName.getText());
                assertEquals("NEW-B", controller.cmbCollection.getValue());
                assertEquals("", controller.lblMetadataStatus.getText());
                assertFalse(controller.lblMetadataStatus.isManaged());
                // Updating the selector after success must not trigger another request.
                verify(service, times(2)).refresh("B", false);
                return null;
            });
        } finally {
            onFx(() -> { LocalizedViews.dispose(controller.pane); return null; });
        }
    }
}
