package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
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
import javafx.scene.control.Alert.AlertType;
public class FileTransferController extends AbstractController {

    Double rowCount;
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
    String errMsg = "Erro na %s. Contate o administrador do spLinker: Error ID %s";
    
    void bindProgress(ReadOnlyDoubleProperty prop)
    {
        progressBar.progressProperty().bind(prop);
        progressIndicator.progressProperty().bind(prop);
    }

    void unbindProgress()
    {
        progressBar.progressProperty().unbind();
        progressIndicator.progressProperty().unbind();
    }

    void configureImportDataTask()
    {
        if(!ds.isSQLDatabase())
        {
            lblMessage.setText("Importando dados. Isso pode levar um tempo.");
            importDataTask.setOnSucceeded((handler) -> {
                System.gc();
                Platform.runLater(()->
                {
                    unbindProgress();
                    configureGenerateDWCTask();
                    executor.execute(generateDWCATask);
                });
            });

            importDataTask.setOnFailed((handler)->{
                var ex = importDataTask.getException();
                var errId = Sentry.captureException(ex);
                var task = "importação dos dados";
                var msg = errMsg.formatted(task, errId);
                ApplicationLog.error(ex.getMessage());
                               
                showErrorModal(msg);
                navigateTo("home");
            });

            bindProgress(importDataTask.progressProperty());
        }
    }

    void configureGenerateDWCTask()
    {
        try 
        {
            lblMessage.setText("Gerando arquivo, por favor aguarde.");
            generateDWCATask.setOnSucceeded((handler) -> {
                System.gc();
                Platform.runLater(()->
                {
                    unbindProgress();
                    rowCount = generateDWCATask.getTotalWork();
                    configureCheckRecordCountTask();
                    executor.submit(checkRecordCountTask);
                });        
            });

            generateDWCATask.setOnFailed((handler)->{
                Platform.runLater(()->{
                    var sentrytoken = System.getProperty("SENTRY_AUTH_TOKEN");
                    showAlert(AlertType.INFORMATION, "Sentry Token","sentry token is %s".formatted(sentrytoken));
        
                    var ex = generateDWCATask.getException();
                    var errId = Sentry.captureException(ex);
                    var task = "geração do arquivo";
                    var msg = errMsg.formatted(task, errId);
                    ApplicationLog.error(ex.getMessage());
                                   
                    showErrorModal(msg);
                    navigateTo("home");
                });
            });

            bindProgress(generateDWCATask.progressProperty());
        } catch (Exception e) {
           handleErrors(e);
        }
        
    }
    
    void configureCheckRecordCountTask()
    {
        var msgLbl = "Verificando integridade dos dados, por favor aguarde";
        lblMessage.setText(msgLbl);
        checkRecordCountTask.setOnFailed((handler)->{
            Platform.runLater(()->{
                var ex = generateDWCATask.getException();
                var errId = Sentry.captureException(ex);
                var task = "verificação de registros";
                var msg = errMsg.formatted(task, errId);
                ApplicationLog.error(ex.getMessage());
                               
                showErrorModal(msg);
                navigateTo("home");
            });
        });

        checkRecordCountTask.setOnSucceeded((handler) -> {
            System.gc();
            Platform.runLater(()->
            {
                try 
                {
                    var hasRecordCountDecreased = checkRecordCountTask.get();
                    if(hasRecordCountDecreased)
                    {
                        var newMsg = "A quantidade de registros a ser enviados é menor do que a enviada anteriormente. Deseja continuar?";
                        lblMessage.setText(newMsg);
                        progressBar.setVisible(false);
                        progressIndicator.setVisible(false);
                        btnCancelTransfer.setVisible(false);
                        btnYes.setVisible(true);
                        btnNo.setVisible(true);
                        btnNo.setOnMouseClicked((___) -> {navigateTo("home");});
                        btnYes.setOnMouseClicked((____)->{
                        
                            progressBar.setVisible(true);
                            progressIndicator.setVisible(true);
                            btnCancelTransfer.setVisible(true);
                            btnYes.setVisible(false);
                            btnNo.setVisible(false);
                            configureTransferFileTask();
                            executor.execute(transferFileTask);
                        });
                    }
                } catch (Exception e) 
                {
                    handleErrors(e);
                }
            });        
        });
    }

    void configureTransferFileTask()
    {
        try 
        {
            lblMessage.setText("Transferindo arquivo, por favor não feche o spLinker.");
            progressIndicator.setVisible(false);
            transferFileTask.setOnSucceeded((handler)->
            {
                System.gc();
                Platform.runLater(()-> {
                    try 
                    {
                        var newData = new HashMap<String, String>(){{put("last_rowcount", String.valueOf(rowCount));
                                                                    put("updated_at", LocalDate.now().toString());
                                                                    put("token", token);}};
                        DataSetService.updateDataSource(newData);   
                    } catch (Exception e) {
                     handleErrors(e);
                    }
                    
                    showAlert(AlertType.INFORMATION, "Transferência Concluída","transferido com sucesso!");
                    navigateTo("home");        
                });
            });
            transferFileTask.setOnFailed((handler)->{
                System.gc();
                Platform.runLater(()->{
                var ex = transferFileTask.getException();
                var errId = Sentry.captureException(ex);
                var task = "transferência do arquivo";
                var msg = errMsg.formatted(task, errId);
                ApplicationLog.error(ex.getMessage());
                               
                showErrorModal(msg);
                navigateTo("home");
                });
            });

            bindProgress(transferFileTask.progressProperty());
            
        }  catch (Exception e) 
        {
            handleErrors(e);
        }
    }

    @FXML
    void onBtnYesClicked()
    {
        btnYes.setVisible(false);
        btnNo.setVisible(false);
        btnCancelTransfer.setVisible(true);
        progressBar.setVisible(true);
        progressIndicator.setVisible(true);

        Platform.runLater(()-> { 
            try 
            {
                generateDWCATask = new GenerateDarwinCoreArchiveTask(dwcService);
                checkRecordCountTask = new CheckRecordCountTask(dwcService);
                transferFileTask = new TransferFileTask(dwcService);
                if(ds.isFile())
                {
                    importDataTask = new ImportDataTask(ds);
                    configureImportDataTask();
                    executor.execute(importDataTask);
                }
                else
                {
                    configureGenerateDWCTask();
                    executor.execute(generateDWCATask);
                }
            } catch (Exception e) {
                handleErrors(e);
            }
        }); 
    }

    @FXML
    void onBtnNoClicked()
    {
        navigateTo("home");
    }

    @FXML
    void onButtonCancelClicked()
    {
        progressBar.progressProperty().unbind();
        progressIndicator.progressProperty().unbind();
        if(importDataTask != null && importDataTask.isRunning())
        {
            importDataTask.cancel();
        }
        if(generateDWCATask.isRunning())
        {
            generateDWCATask.cancel();
        }
        if(checkRecordCountTask.isRunning())
        {
            checkRecordCountTask.cancel();
        }
        if(transferFileTask.isRunning())
        {
            transferFileTask.cancel();
        }
        executor.shutdownNow();
        System.gc();
        navigateTo("home");            
    }
  
    @Override
    public void initialize(URL location, ResourceBundle bundle)
    {  
        btnCancelTransfer.setVisible(false);
        progressBar.setVisible(false);
        progressIndicator.setVisible(false);

        try 
        {    
            token = DataSetService.getCurrentToken();
            ds = DataSetService.getDataSet(token);
            dwcService = new DarwinCoreArchiveService(ds);
            
        } 
        catch (Exception e) 
        {
           handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        getStage().setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        getStage().setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
