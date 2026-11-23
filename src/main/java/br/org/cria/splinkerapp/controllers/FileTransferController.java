package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.tasks.*;
import io.sentry.Sentry;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/** Confirmation, progress and outcome of a single collection transfer. */
public class FileTransferController extends AbstractController {
    enum State { READY, RUNNING, REVIEW, CANCELLING, SUCCESS, CANCELLED, FAILED }

    @FXML Label lblCollection;
    @FXML Label lblTitle;
    @FXML Label lblMessage;
    @FXML Label lblProgress;
    @FXML Label stepImport;
    @FXML Label stepGenerate;
    @FXML Label stepCheck;
    @FXML Label stepSend;
    @FXML ProgressBar progressBar;
    @FXML ProgressIndicator progressIndicator;
    @FXML Button btnYes;
    @FXML Button btnNo;
    @FXML Button btnCancelTransfer;

    private DataSet ds;
    private DarwinCoreArchiveService service;
    private Thread worker;
    private volatile boolean cancelRequested;
    private volatile boolean transferStarted;
    private volatile boolean transferConfirmed;
    private State state = State.READY;
    private Stage window;
    private int completedSteps;
    private final EventHandler<WindowEvent> closeHandler = event -> {
        if (isBusy()) {
            event.consume();
            onButtonCancelClicked();
        }
    };

    @Override public void initialize(URL location, ResourceBundle resources) {
        // Router attaches the scene and window after FXML initialization.
        pane.sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) {
                if (window != null) window.removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, closeHandler);
                return;
            }
            scene.windowProperty().addListener((property, oldWindow, newWindow) -> {
                if (newWindow instanceof Stage stage) attachWindow(stage);
            });
            if (scene.getWindow() instanceof Stage stage) attachWindow(stage);
        });
        try {
            token = TokenRepository.getCurrentToken();
            ds = loadCollection();
            if (ds == null) throw new IllegalArgumentException(I18n.text("dataset.configure"));
            lblCollection.setText(ds.getDataSetAcronym() + " · " + ds.getDataSetName());
            showState(State.READY, "single.ready", "single.confirm");
        } catch (Exception error) {
            showFailure(error);
        }
    }

    DataSet loadCollection() throws Exception { return DataSetService.getDataSet(token); }
    boolean hasConfiguration() throws Exception { return DataSetService.hasConfiguration(token); }
    DarwinCoreArchiveService createService() throws Exception { return new DarwinCoreArchiveService(ds); }
    Task<?> importTask() throws Exception { return new ImportDataTask(ds); }
    Task<?> generateTask() { return new GenerateDarwinCoreArchiveTask(service); }
    Task<Boolean> checkTask() { return new CheckRecordCountTask(service); }
    Task<?> transferTask() { return new TransferFileTask(service); }

    private void attachWindow(Stage stage) {
        if (window == stage) return;
        window = stage;
        setScreensize();
        stage.removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, closeHandler);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, closeHandler);
    }

    @Override protected void setScreensize() {
        var bounds = Screen.getScreensForRectangle(window.getX(), window.getY(), 1, 1)
                .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        window.setMinWidth(Math.min(620, bounds.getWidth()));
        window.setMinHeight(Math.min(600, bounds.getHeight()));
        window.setWidth(Math.min(740, bounds.getWidth()));
        window.setHeight(Math.min(680, bounds.getHeight()));
        window.setResizable(true);
        window.setX(Math.max(bounds.getMinX(), Math.min(window.getX(), bounds.getMaxX() - window.getWidth())));
        window.setY(Math.max(bounds.getMinY(), Math.min(window.getY(), bounds.getMaxY() - window.getHeight())));
    }

    @FXML void onBtnYesClicked() {
        if (state != State.READY && state != State.REVIEW) return;
        boolean resume = state == State.REVIEW;
        cancelRequested = false;
        showState(State.RUNNING, "single.running", "single.preparing");
        worker = new Thread(() -> process(resume), "single-collection-transfer");
        worker.setDaemon(true);
        worker.start();
    }

    private void process(boolean resume) {
        try {
            if (!resume) {
                if (!hasConfiguration()) throw new IllegalArgumentException(I18n.text("dataset.configure"));
                service = createService();
                checkCancellation();
                if (ds.isFile() || ds.isAccessDb()) {
                    runStep(importTask(), 0);
                } else {
                    Platform.runLater(() -> markStep(0, "single.skipped", "complete"));
                }
                runStep(generateTask(), 1);
                boolean fewer = runStep(checkTask(), 2);
                checkCancellation();
                if (fewer) {
                    Platform.runLater(() -> {
                        if (cancelRequested) showCancelled();
                        else showState(State.REVIEW, "single.review", "transfer.fewer");
                    });
                    return;
                }
            }
            checkCancellation();
            transferStarted = true;
            runStep(transferTask(), 3);
            // After server acknowledgement, complete local bookkeeping even when
            // cancellation races with acknowledgement. Never report this as unsent.
            transferConfirmed = true;
            Thread.interrupted();
            Platform.runLater(() -> {
                btnCancelTransfer.setDisable(true);
                lblMessage.setText(I18n.text("single.finishing"));
            });
            int count = service.getTotalRowCount();
            saveResult(count);
            service.cleanData();
            Platform.runLater(() -> {
                showState(State.SUCCESS, "single.success", "single.complete");
                lblMessage.setText(I18n.text("single.success.detail").formatted(count,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                progressBar.setProgress(1);
                lblProgress.setText(I18n.text("single.complete"));
            });
        } catch (Exception error) {
            Thread.interrupted();
            Platform.runLater(() -> {
                if (transferConfirmed) {
                    Sentry.captureException(error);
                    showState(State.FAILED, "single.sent.warning", "single.sent.warning.detail");
                } else if (cancelRequested || error instanceof CancellationException || error instanceof InterruptedException) {
                    showCancelled();
                } else {
                    showFailure(error instanceof ExecutionException && error.getCause() != null ? error.getCause() : error);
                }
            });
        }
    }

    void saveResult(int count) throws Exception {
        var fields = new HashMap<String, String>();
        fields.put("token", token);
        fields.put("last_rowcount", String.valueOf(count));
        fields.put("updated_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        DataSetService.updateDataSource(fields);
        DataSetService.insertTransferHistory(fields);
    }

    private <T> T runStep(Task<T> task, int index) throws Exception {
        try {
            checkCancellation();
            Platform.runLater(() -> {
                markStep(index, "single.active", "active");
                lblProgress.setText(I18n.text("single.step").formatted(index + 1));
                if (!cancelRequested) lblMessage.setText(I18n.text(new String[] {"transfer.importing", "transfer.generating",
                        "transfer.checking", "transfer.sending"}[index]));
                // Work without measured progress stays indeterminate.
                if (!cancelRequested) progressBar.progressProperty().bind(task.progressProperty());
            });
            // Running on this worker means cancellation waits for the actual operation
            // to return, including libraries that ignore interruption.
            task.run();
            T result = task.get();
            Platform.runLater(() -> {
                progressBar.progressProperty().unbind();
                if (!cancelRequested) progressBar.setProgress((index + 1) / 4.0);
                markStep(index, "single.done", "complete");
            });
            return result;
        } finally {
            EventBusManager.unregister(task);
        }
    }

    private void checkCancellation() {
        if (cancelRequested) throw new CancellationException();
    }

    @FXML void onButtonCancelClicked() {
        if (state != State.RUNNING || transferConfirmed) return;
        cancelRequested = true;
        showState(State.CANCELLING, "single.cancelling", "single.cancelling.detail");
        if (worker != null) worker.interrupt();
    }

    @FXML void onBtnNoClicked() {
        if (isBusy()) return;
        if (window != null) {
            window.removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, closeHandler);
        }
        navigateTo("home");
    }

    private boolean isBusy() { return state == State.RUNNING || state == State.CANCELLING; }

    private Label[] steps() { return new Label[] {stepImport, stepGenerate, stepCheck, stepSend}; }

    private void markStep(int index, String status, String style) {
        Label label = steps()[index];
        label.setText(I18n.text("single.stage." + index) + " — " + I18n.text(status));
        label.getStyleClass().removeAll("active", "complete", "failed");
        label.getStyleClass().add(style);
        if (style.equals("complete")) completedSteps = Math.max(completedSteps, index + 1);
    }

    private void showCancelled() {
        showState(State.CANCELLED, "single.cancelled",
                transferStarted ? "single.cancelled.remote" : "single.cancelled.detail");
    }

    private void showFailure(Throwable error) {
        showState(State.FAILED, "single.failed", "single.failed.detail");
        if (error instanceof IllegalArgumentException && error.getMessage() != null) {
            lblMessage.setText(error.getMessage());
        } else {
            var id = Sentry.captureException(error);
            lblMessage.setText(I18n.text("single.failed.detail") + "\n" + I18n.text("single.error.id").formatted(id));
        }
    }

    private void showState(State next, String title, String message) {
        state = next;
        lblTitle.setText(I18n.text(title));
        lblMessage.setText(I18n.text(message));
        lblMessage.getStyleClass().removeAll("transfer-success", "transfer-error");
        if (next == State.SUCCESS) lblMessage.getStyleClass().add("transfer-success");
        if (next == State.FAILED) lblMessage.getStyleClass().add("transfer-error");
        btnYes.setVisible(next == State.READY || next == State.REVIEW);
        btnYes.setText(I18n.text(next == State.REVIEW ? "button.continue" : "single.start"));
        btnNo.setVisible(!isBusy());
        btnNo.setText(I18n.text(next == State.READY || next == State.REVIEW ? "button.cancel" : "single.back"));
        btnCancelTransfer.setVisible(isBusy());
        btnCancelTransfer.setDisable(next == State.CANCELLING);
        progressIndicator.setVisible(isBusy());
        progressBar.setVisible(next != State.FAILED);
        if (next != State.RUNNING) {
            progressBar.progressProperty().unbind();
            if (next == State.CANCELLING) progressBar.setProgress(-1);
        }
        if (next == State.CANCELLED || next == State.FAILED) {
            progressBar.setProgress(completedSteps / 4.0);
            lblProgress.setText(I18n.text(next == State.CANCELLED ? "single.cancelled" : "single.stopped"));
            for (var label : steps()) {
                if (label.getStyleClass().contains("active")) {
                    label.setText(label.getText().replace(I18n.text("single.active"), I18n.text("single.stopped")));
                    label.getStyleClass().remove("active");
                    if (next == State.FAILED) label.getStyleClass().add("failed");
                }
            }
        }
    }
}
