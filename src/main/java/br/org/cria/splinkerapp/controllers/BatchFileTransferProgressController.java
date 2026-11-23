package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.TransferResult;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.tasks.GenerateDarwinCoreArchiveTask;
import br.org.cria.splinkerapp.tasks.ImportDataTask;
import br.org.cria.splinkerapp.tasks.TransferFileTask;
import br.org.cria.splinkerapp.utils.ExceptionUtils;
import io.sentry.Sentry;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Screen;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;

public class BatchFileTransferProgressController extends AbstractController {

    private enum Step { PREPARE, IMPORT, ARCHIVE, SEND }

    private enum Status {
        WAITING("batch.waiting", "waiting"),
        ACTIVE("single.active", "active"),
        COMPLETE("single.done", "complete"),
        SKIPPED("batch.skipped", "waiting"),
        SENT("batch.sent.status", "complete"),
        FAILED("batch.failed", "failed");

        private final String textKey;
        private final PseudoClass style;

        Status(String textKey, String style) {
            this.textKey = textKey;
            this.style = PseudoClass.getPseudoClass(style);
        }

        void applyTo(Label label) {
            label.setText(I18n.text(textKey));
            label.pseudoClassStateChanged(ACTIVE.style, this == ACTIVE);
            label.pseudoClassStateChanged(COMPLETE.style, this == COMPLETE || this == SENT);
            label.pseudoClassStateChanged(FAILED.style, this == FAILED);
        }
    }

    private static final PseudoClass CURRENT_COLLECTION = PseudoClass.getPseudoClass("current-collection");

    @FXML private Label lblColecaoAtual;
    @FXML private Label lblProcessadas;
    @FXML private Label lblSucesso;
    @FXML private Label lblErro;
    @FXML private Label lblFaltam;
    @FXML private Label lblCancelamento;
    @FXML private Label stepPrepare;
    @FXML private Label stepImport;
    @FXML private Label stepArchive;
    @FXML private Label stepSend;
    @FXML private ProgressBar progressBar;
    @FXML private Button btnCancelar;
    @FXML private TableView<TransferResult> tblResultadosParciais;
    @FXML private TableColumn<TransferResult, String> colToken;
    @FXML private TableColumn<TransferResult, TransferResult> colStatus;

    private final ObservableList<TransferResult> resultados = FXCollections.observableArrayList();
    private final Object taskLock = new Object();
    private volatile boolean cancelRequested;
    private Task<?> currentTask;

    private List<Label> steps;
    private int currentIndex = -1;
    private int sucesso;
    private int erro;

    @Override
    protected void setScreensize() {
        var stage = getStage();
        var bounds = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1)
                .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        stage.setWidth(Math.min(940, bounds.getWidth()));
        stage.setHeight(Math.min(660, bounds.getHeight()));
        stage.setX(Math.max(bounds.getMinX(), Math.min(stage.getX(), bounds.getMaxX() - stage.getWidth())));
        stage.setY(Math.max(bounds.getMinY(), Math.min(stage.getY(), bounds.getMaxY() - stage.getHeight())));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        steps = List.of(stepPrepare, stepImport, stepArchive, stepSend);
        steps.forEach(label -> Status.WAITING.applyTo(label));
        configurarTabela();

        List<String> colecoes = BatchTransferContext.getSelectedCollections();
        for (String colecao : colecoes) {
            resultados.add(new TransferResult(colecao, false, "", true));
        }
        atualizarResumo();

        Platform.runLater(() -> {
            if (pane.getScene() != null && pane.getScene().getWindow() != null) {
                setScreensize();
            }
            Thread worker = new Thread(() -> processarColecoes(colecoes), "batch-collection-transfer");
            worker.setDaemon(true);
            worker.start();
        });
    }

    private void configurarTabela() {
        colToken.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getToken()));
        colStatus.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("batch-status");
            }

            @Override
            protected void updateItem(TransferResult result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setGraphic(null);
                    return;
                }

                Status status;
                if (getIndex() == currentIndex) {
                    status = Status.ACTIVE;
                } else if (result.isPendent()) {
                    status = Status.WAITING;
                } else {
                    status = result.isSuccess() ? Status.SENT : Status.FAILED;
                }
                status.applyTo(badge);
                setGraphic(badge);
            }
        });
        tblResultadosParciais.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(TransferResult result, boolean empty) {
                super.updateItem(result, empty);
                pseudoClassStateChanged(CURRENT_COLLECTION, !empty && getIndex() == currentIndex);
            }
        });
        tblResultadosParciais.setItems(resultados);
    }

    private void processarColecoes(List<String> colecoes) {
        for (int index = 0; index < colecoes.size() && !cancelRequested; index++) {
            String colecao = colecoes.get(index);
            int resultIndex = index;
            Platform.runLater(() -> mostrarColecao(resultIndex, colecao));

            TransferResult resultado = processarColecao(colecao);
            Platform.runLater(() -> {
                currentIndex = -1;
                if (!resultado.isPendent()) {
                    if (resultado.isSuccess()) {
                        sucesso++;
                    } else {
                        erro++;
                    }
                }
                resultados.set(resultIndex, resultado);
                atualizarResumo();
            });
        }

        Platform.runLater(() -> {
            BatchTransferContext.setResults(resultados);
            navigateTo("batch-file-transfer-result");
        });
    }

    private TransferResult processarColecao(String acronimo) {
        try {
            verificarCancelamento();
            String token = TokenRepository.getCurrentTokenByAcronym(acronimo);
            if (!DataSetService.hasConfiguration(token)) {
                return new TransferResult(acronimo, false, I18n.text("dataset.unconfigured"),
                        false, I18n.text("dataset.configure"));
            }

            verificarCancelamento();
            var ds = DataSetService.getDataSet(token);
            var dwcService = new DarwinCoreArchiveService(ds);
            boolean importar = ds.isFile() || ds.isAccessDb();
            if (importar) {
                atualizarEtapa(Step.IMPORT, true);
                executarTask(new ImportDataTask(ds));
            }

            atualizarEtapa(Step.ARCHIVE, importar);
            executarTask(new GenerateDarwinCoreArchiveTask(dwcService));

            atualizarEtapa(Step.SEND, importar);
            var transferTask = new TransferFileTask(dwcService);
            try {
                executarTask(transferTask);
            } catch (CancellationException e) {
                String mensagem = I18n.text(transferTask.isCancelled()
                        ? "single.cancelled.remote" : "transfer.cancelled");
                return new TransferResult(acronimo, false, mensagem, true);
            }

            var newData = new HashMap<String, String>();
            newData.put("last_rowcount", String.valueOf(dwcService.getTotalRowCount()));
            newData.put("updated_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            newData.put("token", token);
            DataSetService.updateDataSource(newData);
            DataSetService.insertTransferHistory(newData);
            dwcService.cleanData();

            return new TransferResult(acronimo, true, I18n.text("transfer.success"), false);
        } catch (CancellationException e) {
            return new TransferResult(acronimo, false, I18n.text("transfer.cancelled"), true);
        } catch (Exception e) {
            var errId = Sentry.captureException(e);
            return new TransferResult(acronimo, false, I18n.text("transfer.unexpected") + errId,
                    false, ExceptionUtils.getStackTraceAsString(e));
        }
    }

    private void executarTask(Task<?> task) throws Exception {
        try {
            synchronized (taskLock) {
                verificarCancelamento();
                currentTask = task;
            }
            task.run();
            task.get();
        } finally {
            synchronized (taskLock) {
                currentTask = null;
            }
            EventBusManager.unregister(task);
        }
    }

    private void verificarCancelamento() {
        if (cancelRequested) {
            throw new CancellationException();
        }
    }

    @FXML
    private void cancelarProcesso() {
        synchronized (taskLock) {
            cancelRequested = true;
            if (currentTask != null) {
                currentTask.cancel();
            }
        }
        btnCancelar.setDisable(true);
        btnCancelar.setText(I18n.text("single.cancelling"));
        lblCancelamento.setVisible(true);
    }

    private void mostrarColecao(int index, String colecao) {
        if (cancelRequested) {
            return;
        }
        currentIndex = index;
        lblColecaoAtual.setText(colecao);
        mostrarEtapa(Step.PREPARE, true);
        tblResultadosParciais.refresh();
    }

    private void atualizarEtapa(Step step, boolean importar) {
        Platform.runLater(() -> {
            if (!cancelRequested) {
                mostrarEtapa(step, importar);
            }
        });
    }

    private void mostrarEtapa(Step step, boolean importar) {
        for (Step item : Step.values()) {
            Status status = Status.WAITING;
            if (item == step) {
                status = Status.ACTIVE;
            } else if (item == Step.IMPORT && !importar) {
                status = Status.SKIPPED;
            } else if (item.ordinal() < step.ordinal()) {
                status = Status.COMPLETE;
            }
            status.applyTo(steps.get(item.ordinal()));
        }
    }

    private void atualizarResumo() {
        int processadas = sucesso + erro;
        int total = resultados.size();
        lblProcessadas.setText(I18n.text("batch.processed").formatted(processadas, total));
        lblSucesso.setText(String.valueOf(sucesso));
        lblErro.setText(String.valueOf(erro));
        lblFaltam.setText(String.valueOf(total - processadas));
        progressBar.setProgress(total == 0 ? 0 : (double) processadas / total);
    }
}
