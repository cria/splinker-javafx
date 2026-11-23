package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FileTransferControllerTest {
    static class Fixture extends FileTransferController {
        final DataSet collection = mock(DataSet.class);
        final DarwinCoreArchiveService archive = mock(DarwinCoreArchiveService.class);
        boolean fewer;
        boolean configured = true;
        int saved;
        int generated;
        int transferred;
        Callable<Void> generate = () -> null;
        Callable<Void> transfer = () -> null;

        Fixture() {
            when(collection.getDataSetAcronym()).thenReturn("TEST");
            when(collection.getDataSetName()).thenReturn("Coleção de teste");
            when(archive.getTotalRowCount()).thenReturn(12);
        }
        @Override DataSet loadCollection() { return collection; }
        @Override boolean hasConfiguration() { return configured; }
        @Override DarwinCoreArchiveService createService() { return archive; }
        @Override Task<?> generateTask() { return task(() -> { generated++; return generate.call(); }); }
        @Override Task<Boolean> checkTask() { return task(() -> fewer); }
        @Override Task<?> transferTask() { return task(() -> { transferred++; return transfer.call(); }); }
        @Override void saveResult(int count) { saved = count; }
    }

    private static <T> Task<T> task(Callable<T> operation) {
        return new Task<>() { @Override protected T call() throws Exception { return operation.call(); } };
    }

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

    private Fixture load() throws Exception {
        return onFx(() -> {
            var fixture = new Fixture();
            try (var tokens = mockStatic(TokenRepository.class)) {
                tokens.when(TokenRepository::getCurrentToken).thenReturn("test");
                var loader = LocalizedViews.loader(getClass().getResource("/br/org/cria/splinkerapp/file-transfer.fxml"));
                loader.setControllerFactory(type -> fixture);
                Parent root = LocalizedViews.load(loader);
                new Scene(root, 740, 640);
                root.applyCss();
                root.layout();
                assertTrue(fixture.lblCollection.getText().contains("TEST"));
                assertTrue(fixture.btnYes.isVisible());
                assertFalse(fixture.btnCancelTransfer.isVisible());
            }
            return fixture;
        });
    }

    private CompletableFuture<Void> until(Fixture fixture, String title) throws Exception {
        return onFx(() -> {
            var reached = new CompletableFuture<Void>();
            fixture.lblTitle.textProperty().addListener((property, before, after) -> {
                if (after.equals(I18n.text(title))) reached.complete(null);
            });
            return reached;
        });
    }

    @Test public void successfulTransferShowsSummaryAndPreventsDuplicateStarts() throws Exception {
        var fixture = load();
        var done = until(fixture, "single.success");
        onFx(() -> { fixture.onBtnYesClicked(); fixture.onBtnYesClicked(); return null; });
        done.get(10, TimeUnit.SECONDS);
        onFx(() -> {
            assertEquals(12, fixture.saved);
            assertEquals(1, fixture.generated);
            assertEquals(1, fixture.transferred);
            assertTrue(fixture.lblMessage.getText().contains("12"));
            assertEquals(1, fixture.progressBar.getProgress(), 0);
            assertTrue(fixture.progressBar.isVisible());
            assertFalse(fixture.progressIndicator.isVisible());
            assertTrue(fixture.btnNo.isVisible());
            assertFalse(fixture.btnYes.isVisible());
            return null;
        });
        verify(fixture.archive).cleanData();
    }

    @Test public void fewerRecordsWaitForExplicitConfirmationWithoutRegenerating() throws Exception {
        var fixture = load();
        fixture.fewer = true;
        var review = until(fixture, "single.review");
        var done = until(fixture, "single.success");
        onFx(() -> { fixture.onBtnYesClicked(); return null; });
        review.get(10, TimeUnit.SECONDS);
        onFx(() -> {
            assertEquals(0, fixture.transferred);
            assertTrue(fixture.btnYes.isVisible());
            assertFalse(fixture.btnCancelTransfer.isVisible());
            fixture.onBtnYesClicked();
            return null;
        });
        done.get(10, TimeUnit.SECONDS);
        onFx(() -> { assertEquals(1, fixture.generated); assertEquals(1, fixture.transferred); return null; });
    }

    @Test public void cancellationWaitsForOperationToStopAndDoesNotSend() throws Exception {
        var fixture = load();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        fixture.generate = () -> {
            entered.countDown();
            boolean stopped = false;
            while (!stopped) {
                try { stopped = release.await(10, TimeUnit.SECONDS); }
                catch (InterruptedException ignored) { /* Simulate a library that cannot stop immediately. */ }
            }
            return null;
        };
        var cancelled = until(fixture, "single.cancelled");
        onFx(() -> { fixture.onBtnYesClicked(); return null; });
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        try {
            onFx(() -> {
                fixture.onButtonCancelClicked();
                assertEquals(I18n.text("single.cancelling"), fixture.lblTitle.getText());
                assertTrue(fixture.btnCancelTransfer.isDisabled());
                assertFalse(fixture.btnNo.isVisible());
                return null;
            });
            assertFalse(cancelled.isDone());
        } finally { release.countDown(); }
        cancelled.get(10, TimeUnit.SECONDS);
        onFx(() -> {
            assertEquals(0, fixture.transferred);
            assertEquals(0, fixture.saved);
            assertTrue(fixture.btnNo.isVisible());
            assertFalse(fixture.progressBar.progressProperty().isBound());
            return null;
        });
        verify(fixture.archive, never()).cleanData();
    }

    @Test public void failedTransferKeepsFeedbackOnScreenWithoutRecordingSuccess() throws Exception {
        var fixture = load();
        fixture.transfer = () -> { throw new IllegalArgumentException("Servidor indisponível"); };
        var failed = until(fixture, "single.failed");
        onFx(() -> { fixture.onBtnYesClicked(); return null; });
        failed.get(10, TimeUnit.SECONDS);
        onFx(() -> {
            assertEquals("Servidor indisponível", fixture.lblMessage.getText());
            assertEquals(0, fixture.saved);
            assertFalse(fixture.btnCancelTransfer.isVisible());
            assertTrue(fixture.stepSend.getStyleClass().contains("failed"));
            assertFalse(fixture.progressBar.isVisible());
            assertFalse(fixture.progressBar.isManaged());
            return null;
        });
        verify(fixture.archive, never()).cleanData();
    }

    @Test public void serverAcknowledgementWinsOverLateCancellation() throws Exception {
        var fixture = load();
        var entered = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        fixture.transfer = () -> {
            entered.countDown();
            try { assertTrue(interrupted.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException expected) { /* Server has already accepted the upload. */ }
            return null;
        };
        var done = until(fixture, "single.success");
        onFx(() -> { fixture.onBtnYesClicked(); return null; });
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        onFx(() -> { fixture.onButtonCancelClicked(); return null; });
        done.get(10, TimeUnit.SECONDS);
        onFx(() -> { assertEquals(12, fixture.saved); return null; });
    }

    @Test public void cancellationDuringUploadDoesNotClaimDataWasNeverSent() throws Exception {
        var fixture = load();
        var entered = new CountDownLatch(1);
        fixture.transfer = () -> {
            entered.countDown();
            new CountDownLatch(1).await(10, TimeUnit.SECONDS);
            throw new IllegalStateException("Expected cancellation");
        };
        var done = until(fixture, "single.cancelled");
        onFx(() -> { fixture.onBtnYesClicked(); return null; });
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        onFx(() -> { fixture.onButtonCancelClicked(); return null; });
        done.get(10, TimeUnit.SECONDS);
        onFx(() -> {
            assertEquals(0, fixture.saved);
            assertEquals(I18n.text("single.cancelled.remote"), fixture.lblMessage.getText());
            return null;
        });
    }
}
