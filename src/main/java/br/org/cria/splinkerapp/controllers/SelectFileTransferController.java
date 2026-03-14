package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.TransferResult;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.tasks.GenerateDarwinCoreArchiveTask;
import br.org.cria.splinkerapp.tasks.ImportDataTask;
import br.org.cria.splinkerapp.tasks.TransferFileTask;
import io.sentry.Sentry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import org.controlsfx.control.CheckComboBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectFileTransferController extends AbstractController {

    @FXML
    private Button btnEnviar;

    @FXML
    private CheckComboBox<String> cmbToken;

    @FXML
    private Label lblMessage;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ProgressIndicator progressIndicator;

    private volatile boolean processing = false;

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            cmbToken.getItems().addAll(TokenRepository.getAcronyms());
            cmbToken.setTitle("Selecione as coleções");
            cmbToken.setShowCheckedCount(true);

            if (lblMessage != null) {
                lblMessage.setText("");
            }

            if (progressBar != null) {
                progressBar.setVisible(false);
                progressBar.setProgress(0);
            }

            if (progressIndicator != null) {
                progressIndicator.setVisible(false);
                progressIndicator.setProgress(0);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        btnEnviar.setOnAction(e -> enviar());
    }

    private void enviar() {
        if (processing) {
            return;
        }

        List<String> colecoesSelecionados = new ArrayList<>(cmbToken.getCheckModel().getCheckedItems());

        if (colecoesSelecionados.isEmpty()) {
            showAlert("Erro", "Por favor, selecione pelo menos uma coleção.");
            return;
        }

        processing = true;
        btnEnviar.setDisable(true);
        cmbToken.setDisable(true);

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setProgress(0);
        }

        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
            progressIndicator.setProgress(0);
        }

        atualizarMensagem("Iniciando envio das coleções...");

        Thread worker = new Thread(() -> processarColecoes(colecoesSelecionados));
        worker.setDaemon(true);
        worker.start();
    }

    private void processarColecoes(List<String> colecoesSelecionados) {
        List<TransferResult> resultados = new ArrayList<>();
        int total = colecoesSelecionados.size();

        for (int i = 0; i < total; i++) {
            String colecao = colecoesSelecionados.get(i);
            int indiceAtual = i + 1;

            atualizarMensagem("Processando coleção " + indiceAtual + " de " + total + ": " + colecao);
            atualizarProgresso((double) i / total);

            try {
                TransferResult resultado = processarColecao(colecao);
                resultados.add(resultado);
            } catch (Exception e) {
                var errId = Sentry.captureException(e);
                resultados.add(new TransferResult(colecao, false, "Falha inesperada. Error ID: " + errId));
            }
        }

        atualizarMensagem("Finalizando processamento...");
        atualizarProgresso(1.0);

        Platform.runLater(() -> {
            processing = false;
            btnEnviar.setDisable(false);
            cmbToken.setDisable(false);

            BatchFileTransferResultController.setResults(resultados);
            loadPage("batch-file-transfer-result");
        });
    }

    private TransferResult processarColecao(String acronimo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            String token = TokenRepository.getCurrentTokenByAcronym(acronimo);
            boolean hasConfiguration = DataSetService.hasConfiguration(token);

            if (!hasConfiguration) {
                return new TransferResult(acronimo, false,
                        "Coleção não possui dataset configurado. Acesse Configuração -> Dados para realizar a configuração.");
            }

            DataSet ds = DataSetService.getDataSet(token);
            DarwinCoreArchiveService dwcService = new DarwinCoreArchiveService(ds);

            if (ds.isFile() || ds.isAccessDb()) {
                atualizarMensagem("[" + acronimo + "] Importando dados da coleção...");
                ImportDataTask importDataTask = new ImportDataTask(ds);
                executor.submit(importDataTask);
                importDataTask.get();
            }

            atualizarMensagem("[" + acronimo + "] Gerando arquivo com os dados da coleção...");
            GenerateDarwinCoreArchiveTask generateTask = new GenerateDarwinCoreArchiveTask(dwcService);
            executor.submit(generateTask);
            generateTask.get();

            atualizarMensagem("[" + acronimo + "] Transferindo arquivo com dados da coleção...");
            TransferFileTask transferTask = new TransferFileTask(dwcService);
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

            return new TransferResult(acronimo, true, "Envio realizado com sucesso.");
        } catch (Exception e) {
            var errId = Sentry.captureException(e);
            return new TransferResult(acronimo, false, "Falha no envio. Error ID: " + errId);
        } finally {
            executor.shutdownNow();
            System.gc();
        }
    }

    private void atualizarMensagem(String mensagem) {
        if (lblMessage == null) {
            return;
        }

        Platform.runLater(() -> lblMessage.setText(mensagem));
    }

    private void atualizarProgresso(double progresso) {
        Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progresso);
            }

            if (progressIndicator != null) {
                progressIndicator.setProgress(progresso);
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}