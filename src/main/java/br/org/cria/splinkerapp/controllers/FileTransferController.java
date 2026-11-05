package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.tasks.CheckRecordCountTask;
import br.org.cria.splinkerapp.tasks.GenerateDarwinCoreArchiveTask;
import br.org.cria.splinkerapp.tasks.ImportDataTask;
import br.org.cria.splinkerapp.tasks.TransferFileTask;
import io.sentry.Sentry;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

public class FileTransferController extends AbstractController {

    int rowCount;
    DataSet ds;
    DarwinCoreArchiveService dwcService;

    @FXML
    Button btnYes;

    @FXML
    Button btnNo;

    @FXML
    Label lblMessage;

    @FXML
    Button btnCancelTransfer;

    @FXML
    ProgressBar progressBar;

    @FXML
    ProgressIndicator progressIndicator;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    ImportDataTask importDataTask;
    GenerateDarwinCoreArchiveTask generateDWCATask;
    CheckRecordCountTask checkRecordCountTask;
    TransferFileTask transferFileTask;
    String errMsg = I18n.text("transfer.error");

    void bindProgress(ReadOnlyDoubleProperty prop) {
        progressBar.progressProperty().bind(prop);
        progressIndicator.progressProperty().bind(prop);
    }

    void unbindProgress() {
        progressBar.progressProperty().unbind();
        progressIndicator.progressProperty().unbind();
    }

    void configureImportDataTask() {
        if (!ds.isSQLDatabase()) {
            lblMessage.setText(I18n.text("transfer.importing"));
            importDataTask.setOnSucceeded((handler) -> {
                System.gc();
                Platform.runLater(() ->
                {
                    unbindProgress();
                    configureGenerateDWCTask();
                    executor.execute(generateDWCATask);
                });
            });

            importDataTask.setOnFailed((handler) -> {
                var ex = importDataTask.getException();
                var errId = Sentry.captureException(ex);
                var task = I18n.text("transfer.import");
                var msg = errMsg.formatted(task, errId);
                /*    if (ex.getMessage() != null && ex.getMessage().contains("return value of \"org.dhatim.fastexcel.reader.Row.getCell(int)\" is null")) {
                         msg = I18n.text("transfer.excel.error")
                                + I18n.text("transfer.excel.fix");
                    } else {
                        handleErrors(ex);
                    }*/
                handleErrors(ex);
                //showErrorModal(msg);
                navigateTo("home");
            });

            bindProgress(importDataTask.progressProperty());
        }
    }

    void configureGenerateDWCTask() {
        try {
            lblMessage.setText(I18n.text("transfer.generating"));
            generateDWCATask.setOnSucceeded((handler) -> {
                System.gc();
                Platform.runLater(() ->
                {
                    unbindProgress();
                    configureCheckRecordCountTask();
                    executor.submit(checkRecordCountTask);
                });
            });

            generateDWCATask.setOnFailed((handler) -> Platform.runLater(() -> {
                var ex = generateDWCATask.getException();
                var errId = Sentry.captureException(ex);
                var task = I18n.text("transfer.generate");
                var msg = errMsg.formatted(task, errId);
                /*if(ex.getMessage().contains("no such table")) {
                    String tableName = ex.getMessage().split(":")[1].replace(")", "").trim();
                    msg = I18n.text("transfer.query.error") + tableName + I18n.text("transfer.query.missing");
                } else if(ex.getMessage().contains("no such column")) {
                    String columnName = ex.getMessage().split(":")[1].replace(")", "").trim();
                    msg = I18n.text("transfer.column.error") + columnName + I18n.text("transfer.column.missing");
                } else {
                    handleErrors(ex);
                }*/
                handleErrors(ex);
                //showErrorModal(msg);
                navigateTo("home");
            }));

            bindProgress(generateDWCATask.progressProperty());
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void configureCheckRecordCountTask() {
        var msgLbl = I18n.text("transfer.checking");
        lblMessage.setText(msgLbl);
        progressBar.setVisible(false);
        progressIndicator.setVisible(false);

        checkRecordCountTask.setOnFailed((handler) -> Platform.runLater(() -> {
            var ex = checkRecordCountTask.getException();
            var errId = Sentry.captureException(ex);
            var task = I18n.text("transfer.check");
            var msg = errMsg.formatted(task, errId);
            handleErrors(ex);

            //showErrorModal(msg);
            navigateTo("home");
        }));

        checkRecordCountTask.setOnSucceeded((handler) -> {
            System.gc();
            Platform.runLater(() ->
            {
                progressBar.setVisible(false);
                progressIndicator.setVisible(false);
                try {
                    var hasRecordCountDecreased = checkRecordCountTask.get();
                    if (!hasRecordCountDecreased) {
                        progressBar.setVisible(true);
                        configureTransferFileTask();
                        executor.execute(transferFileTask);
                        return;
                    }

                    var newMsg = I18n.text("transfer.fewer");
                    btnCancelTransfer.setVisible(false);
                    lblMessage.setText(newMsg);
                    btnYes.setText(I18n.text("button.continue"));

                    btnNo.setOnMouseClicked((__) -> navigateTo("home"));
                    btnYes.setOnMouseClicked((__) -> {
                        progressBar.setVisible(true);
                        btnCancelTransfer.setVisible(true);
                        btnYes.setVisible(false);
                        btnNo.setVisible(false);
                        configureTransferFileTask();
                        executor.execute(transferFileTask);
                    });
                    btnYes.setVisible(true);
                    btnNo.setVisible(true);
                } catch (Exception e) {
                    handleErrors(e);
                }
            });
        });
    }

    void configureTransferFileTask() {
        try {
            lblMessage.setText(I18n.text("transfer.sending"));
            transferFileTask.setOnSucceeded((handler) ->
            {
                System.gc();
                Platform.runLater(() -> {
                    try {
                        rowCount = dwcService.getTotalRowCount();
                        progressBar.setVisible(false);
                        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        var updatedAt = LocalDateTime.now().format(fmt);
                        lblMessage.setText(I18n.text("transfer.sent"));
                        var newData = new HashMap<String, String>() {{
                            put("last_rowcount", String.valueOf(rowCount));
                            put("updated_at", updatedAt);
                            put("token", token);
                        }};

                        DataSetService.updateDataSource(newData);
                        DataSetService.insertTransferHistory(newData);
                        dwcService.cleanData();
                        executor.close();
                        btnCancelTransfer.setText(I18n.text("button.finish"));
                        btnCancelTransfer.getStyleClass().add("primary-button");
                        btnCancelTransfer.setOnMouseClicked((__) ->
                                navigateTo("home"));

                    } catch (Exception e) {
                        handleErrors(e);
                    }
                });
            });
            transferFileTask.setOnFailed((handler) -> {
                System.gc();
                Platform.runLater(() -> {
                    var ex = transferFileTask.getException();
                    var errId = Sentry.captureException(ex);
                    var task = I18n.text("transfer.file");
                    var msg = errMsg.formatted(task, errId);
                    handleErrors(ex);
                    //showErrorModal(msg);
                    navigateTo("home");
                });
            });

            bindProgress(transferFileTask.progressProperty());

        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @FXML
    void onBtnYesClicked() {
        try {
            boolean hasConfiguration = DataSetService.hasConfiguration(TokenRepository.getCurrentToken());
            if (!hasConfiguration) {
                showErrorModal(I18n.text("dataset.configure"));
                navigateTo("home");
                return;
            }
            btnYes.setVisible(false);
            btnNo.setVisible(false);
            btnCancelTransfer.setVisible(true);
            progressBar.setVisible(true);
            progressIndicator.setVisible(true);

            generateDWCATask = new GenerateDarwinCoreArchiveTask(dwcService);
            checkRecordCountTask = new CheckRecordCountTask(dwcService);
            transferFileTask = new TransferFileTask(dwcService);
            if (ds.isFile() || ds.isAccessDb()) {
                importDataTask = new ImportDataTask(ds);
                configureImportDataTask();
                executor.execute(importDataTask);
            } else {
                configureGenerateDWCTask();
                executor.execute(generateDWCATask);
            }
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @FXML
    void onBtnNoClicked() {
        navigateTo("home");
    }

    @FXML
    void onButtonCancelClicked() {
        progressBar.progressProperty().unbind();
        progressIndicator.progressProperty().unbind();
        if (importDataTask != null && importDataTask.isRunning()) {
            importDataTask.cancel();
        }
        if (generateDWCATask.isRunning()) {
            generateDWCATask.cancel();
        }
        if (checkRecordCountTask.isRunning()) {
            checkRecordCountTask.cancel();
        }
        if (transferFileTask.isRunning()) {
            transferFileTask.cancel();
        }
        executor.shutdownNow();
        System.gc();
        navigateTo("home");
    }

    @Override
    public void initialize(URL location, ResourceBundle bundle) {
        btnCancelTransfer.setVisible(false);
        progressBar.setVisible(false);
        progressIndicator.setVisible(false);

        try {
            token = TokenRepository.getCurrentToken();
            ds = DataSetService.getDataSet(token);
            dwcService = new DarwinCoreArchiveService(ds);

        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        getStage().setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        getStage().setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
