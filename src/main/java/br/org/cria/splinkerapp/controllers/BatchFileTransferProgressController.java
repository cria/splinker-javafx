package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchFileTransferProgressController extends AbstractController {

    @FXML
    private Label lblMessage;

    @FXML
    private Label lblColecaoAtual;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblSucesso;

    @FXML
    private Label lblErro;

    @FXML
    private Label lblFaltam;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Button btnCancelar;

    @FXML
    private TableView<TransferResult> tblResultadosParciais;

    @FXML
    private TableColumn<TransferResult, String> colStatusIcon;

    @FXML
    private TableColumn<TransferResult, String> colToken;

    private final ObservableList<TransferResult> resultados = FXCollections.observableArrayList();

    private volatile boolean cancelRequested = false;
    private volatile boolean processing = false;

    private volatile ImportDataTask importDataTask;
    private volatile GenerateDarwinCoreArchiveTask generateTask;
    private volatile TransferFileTask transferTask;
    private volatile ExecutorService executor;

    private int total;
    private int sucesso;
    private int erro;
    private int processadas;

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabela();

        progressBar.setProgress(0);
        progressIndicator.setProgress(0);

        List<String> colecoes = BatchTransferContext.getSelectedCollections();
        total = colecoes.size();

        inicializarResultadosParciais(colecoes);

        atualizarResumo();
        lblColecaoAtual.setText("-");

        btnCancelar.setOnAction(e -> cancelarProcesso());

        iniciarProcessamento(colecoes);
    }

    private void configurarTabela() {
        colStatusIcon.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
        colToken.setCellValueFactory(new PropertyValueFactory<>("token"));

        tblResultadosParciais.setItems(resultados);

        tblResultadosParciais.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TransferResult item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else if (item.isPendent()) {
                    setStyle("-fx-background-color: #fff8e1;");
                } else if (item.isSuccess()) {
                    setStyle("-fx-background-color: #e8f5e9;");
                } else {
                    setStyle("-fx-background-color: #fdecea;");
                }
            }
        });
    }

    private void inicializarResultadosParciais(List<String> colecoesSelecionadas) {
        resultados.clear();

        for (String colecao : colecoesSelecionadas) {
            resultados.add(new TransferResult(colecao, false, "", true));
        }

        Platform.runLater(() -> tblResultadosParciais.refresh());
    }

    private void iniciarProcessamento(List<String> colecoesSelecionadas) {
        if (processing) {
            return;
        }

        processing = true;

        Thread worker = new Thread(() -> {
            try {
                processarColecoes(colecoesSelecionadas);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void processarColecoes(List<String> colecoesSelecionadas) throws InterruptedException {
        for (String colecao : colecoesSelecionadas) {
            if (cancelRequested) {
                break;
            }

            atualizarColecaoAtual(colecao);
            atualizarMensagem(I18n.text("stage.start.running") +
                    I18n.text("stage.import.pending") +
                    I18n.text("stage.generate.pending") +
                    I18n.text("stage.send.pending"));
            Thread.sleep(1000);

            TransferResult resultado;

            try {
                resultado = processarColecao(colecao);
            } catch (Exception e) {
                var errId = Sentry.captureException(e);
                String errorLog = br.org.cria.splinkerapp.utils.ExceptionUtils.getStackTraceAsString(e);
                resultado = new TransferResult(
                        colecao,
                        false,
                        I18n.text("transfer.unexpected") + errId,
                        false,
                        br.org.cria.splinkerapp.utils.ExceptionUtils.getStackTraceAsString(e)
                );
            }

            if (resultado.isSuccess()) {
                sucesso++;
            } else {
                erro++;
            }

            processadas++;

            atualizarResultadoParcial(resultado);
            atualizarResumo();
            atualizarProgresso();

            if (cancelRequested) {
                break;
            }
        }

        BatchTransferContext.setResults(resultados);

        Platform.runLater(() -> {
            processing = false;
            loadPage("batch-file-transfer-result");
        });
    }

    private TransferResult processarColecao(String acronimo) {
        executor = Executors.newSingleThreadExecutor();

        try {
            String token = TokenRepository.getCurrentTokenByAcronym(acronimo);
            boolean hasConfiguration = DataSetService.hasConfiguration(token);

            if (!hasConfiguration) {
                return new TransferResult(
                        acronimo,
                        false,
                        I18n.text("dataset.unconfigured"),
                        false, I18n.text("dataset.configure")
                );
            }

            DataSet ds = DataSetService.getDataSet(token);
            DarwinCoreArchiveService dwcService = new DarwinCoreArchiveService(ds);

            if (cancelRequested) {
                return new TransferResult(acronimo, false, I18n.text("transfer.cancelled"), true);
            }

            if (ds.isFile() || ds.isAccessDb()) {
                atualizarMensagem(I18n.text("stage.start.done") +
                        I18n.text("stage.import.running") +
                        I18n.text("stage.generate.pending") +
                        I18n.text("stage.send.pending"));
                Thread.sleep(500);
                importDataTask = new ImportDataTask(ds);
                executor.submit(importDataTask);
                importDataTask.get();
            }

            if (cancelRequested) {
                return new TransferResult(acronimo, false, I18n.text("transfer.cancelled"), true);
            }

            atualizarMensagem(I18n.text("stage.start.done") +
                    I18n.text("stage.import.done") +
                    I18n.text("stage.generate.running") +
                    I18n.text("stage.send.pending"));
            Thread.sleep(500);
            generateTask = new GenerateDarwinCoreArchiveTask(dwcService);
            executor.submit(generateTask);
            generateTask.get();

            if (cancelRequested) {
                return new TransferResult(acronimo, false, I18n.text("transfer.cancelled"), true);
            }

            atualizarMensagem(I18n.text("stage.start.done") +
                    I18n.text("stage.import.done") +
                    I18n.text("stage.generate.done") +
                    I18n.text("stage.send.running"));
            Thread.sleep(500);
            transferTask = new TransferFileTask(dwcService);
            executor.submit(transferTask);
            transferTask.get();

            int rowCount = dwcService.getTotalRowCount();
            String updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            var newData = new HashMap<String, String>() {{
                put("last_rowcount", String.valueOf(rowCount));
                put("updated_at", updatedAt);
                put("token", token);
            }};

            DataSetService.updateDataSource(newData);
            DataSetService.insertTransferHistory(newData);
            dwcService.cleanData();

            atualizarMensagem(I18n.text("stage.start.done") +
                    I18n.text("stage.import.done") +
                    I18n.text("stage.generate.done") +
                    I18n.text("stage.send.done"));
            Thread.sleep(500);

            return new TransferResult(acronimo, true, I18n.text("transfer.success"), false);
        } catch (Exception e) {
            var errId = Sentry.captureException(e);
            return new TransferResult(
                    acronimo,
                    false,
                    I18n.text("transfer.unexpected") + errId,
                    false,
                    br.org.cria.splinkerapp.utils.ExceptionUtils.getStackTraceAsString(e)
            );
        }
    }

    private void atualizarResultadoParcial(TransferResult resultado) {
        for (int i = 0; i < resultados.size(); i++) {
            TransferResult atual = resultados.get(i);
            if (atual.getToken().equals(resultado.getToken())) {
                resultados.set(i, resultado);
                break;
            }
        }
        Platform.runLater(() -> {
            tblResultadosParciais.refresh();
        });
    }

    private void cancelarProcesso() {
        cancelRequested = true;
        btnCancelar.setDisable(true);
        atualizarMensagem(I18n.text("transfer.cancelling"));
        cancelarTasksAtuais();
    }

    private void cancelarTasksAtuais() {
        if (importDataTask != null && importDataTask.isRunning()) {
            importDataTask.cancel();
        }
        if (generateTask != null && generateTask.isRunning()) {
            generateTask.cancel();
        }
        if (transferTask != null && transferTask.isRunning()) {
            transferTask.cancel();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void atualizarMensagem(String mensagem) {
        Platform.runLater(() -> lblMessage.setText(mensagem));
    }

    private void atualizarColecaoAtual(String colecao) {
        Platform.runLater(() -> lblColecaoAtual.setText(colecao));
    }

    private void atualizarProgresso() {
        double progresso = total == 0 ? 0 : (double) processadas / total;

        Platform.runLater(() -> {
            progressBar.setProgress(progresso);
            progressIndicator.setProgress(progresso);
        });
    }

    private void atualizarResumo() {
        int faltam = Math.max(total - processadas, 0);

        Platform.runLater(() -> {
            lblTotal.setText(String.valueOf(total));
            lblSucesso.setText(String.valueOf(sucesso));
            lblErro.setText(String.valueOf(erro));
            lblFaltam.setText(String.valueOf(faltam));
        });
    }
}