package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.Subscribe;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.events.GenerateDWCFileEvent;
import br.org.cria.splinkerapp.events.ImportDataEvent;
import br.org.cria.splinkerapp.events.TransferFileEvent;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.tasks.GenerateDarwinCoreArchiveTask;
import br.org.cria.splinkerapp.tasks.ImportDataTask;
import br.org.cria.splinkerapp.tasks.TransferFileTask;
import io.sentry.Sentry;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
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
    
    Future taskRunner;
    ImportDataTask importDataTask;
    GenerateDarwinCoreArchiveTask generateDWCATask;
    TransferFileTask transferFileTask;
    
         
    void submitTask(Runnable task)
    {
        //taskRunner = executor.submit(Thread.ofPlatform().start(task));
        taskRunner = executor.submit(task);
    }
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
            importDataTask.setOnCancelled((handler) -> {
                ApplicationLog.info("Import data cancelled.");
            });
            importDataTask.setOnSucceeded((handler) -> {
                ApplicationLog.info("Import data successfully");
                System.gc();
                Platform.runLater(()->
                {
                    unbindProgress();
                });
            });

            importDataTask.setOnFailed((handler)->{
                ApplicationLog.info("Import data failed");
                var ex = importDataTask.getException();
                var msg = ex.getLocalizedMessage();
                executor.shutdownNow();
                ApplicationLog.error(msg);
                showErrorModal(msg);
            });

            bindProgress(importDataTask.progressProperty());
            ApplicationLog.info("Starting import data thread");
        }
    }
    void configureGenerateDWCTask()
    {
        try 
        {
            lblMessage.setText("Gerando arquivo, por favor aguarde.");
            generateDWCATask.setOnCancelled((handler) ->{
                ApplicationLog.info("Generate file task cancelled.");
        });
            generateDWCATask.setOnSucceeded((handler) -> {
                System.gc();
                ApplicationLog.info("Generate file successfully.");
                Platform.runLater(()->
                {
                    unbindProgress();
                    rowCount = generateDWCATask.getTotalWork();
                });        
            });

            generateDWCATask.setOnFailed((handler)->{
                ApplicationLog.info("Generate file task failed.");
                Platform.runLater(()->{
                    executor.shutdownNow();
                    var ex = generateDWCATask.getException();
                    var msg = ex.getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                });
            });

            bindProgress(generateDWCATask.progressProperty());
                
            ApplicationLog.info("Starting Generate DWC file thread");
    }
    void configureSendFileTask()
    {
        try 
        {
            lblMessage.setText("Transferindo arquivo, por favor não feche o spLinker.");
            progressIndicator.setVisible(false);
            transferFileTask.setOnCancelled((handler) -> {
                executor.shutdownNow();
                ApplicationLog.info("Transfer file task cancelled.");
            });
            transferFileTask.setOnSucceeded((handler)->
            {
                System.gc();
                ApplicationLog.info("Transfer file successfully.");
                Platform.runLater(()-> {
                    try 
                    {
                        var newData = new HashMap<String, String>(){{put("last_rowcount", String.valueOf(rowCount));
                                                                    put("updated_at", LocalDate.now().toString());
                                                                    put("token", token);}};
                        DataSetService.updateDataSource(newData);   
                    } catch (Exception e) {
                        Sentry.captureException(e);
                        ApplicationLog.error(e.getLocalizedMessage());
                        showErrorModal(e.getLocalizedMessage());
                    }
                    
                    showAlert(AlertType.INFORMATION, "Transferência Concluída","transferido com sucesso!");
                    navigateTo("home");        
                });
            });
            transferFileTask.setOnFailed((handler)->{
                ApplicationLog.info("Transfer file task failed.");
                System.gc();
                Platform.runLater(()->{
                    executor.shutdownNow();
                    var msg = transferFileTask.getException().getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                });
            });

            ApplicationLog.info("Starting send file thread");
            bindProgress(transferFileTask.progressProperty());
            
        }  catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onBtnYesClicked()
    {
        ApplicationLog.info("Botão Sim clickado. Iniciando eventos para o token %s".formatted(ds.getToken()));
        btnYes.setVisible(false);
        btnNo.setVisible(false);
        btnCancelTransfer.setVisible(true);
        progressBar.setVisible(true);
        progressIndicator.setVisible(true);

        Platform.runLater(()-> { 
            try 
            {
                ApplicationLog.info("Dispatching event importDataEvent");
                
                
            } catch (Exception e) {
                Sentry.captureException(e);
                ApplicationLog.error(e.getLocalizedMessage());
                showErrorModal(e.getLocalizedMessage());
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
        if(generateDWCATask != null && generateDWCATask.isRunning())
        {
            generateDWCATask.cancel();
        }
        if(transferFileTask != null && transferFileTask.isRunning())
        {
            transferFileTask.cancel();
        }
        //executor.shutdownNow();
        //executor.close();
        
        System.gc();
        navigateTo("home");            
    }
    
    @Subscribe
    void onImportDataEventTrigger(ImportDataEvent event)
    {
        try 
        { 
            var generateDWCFileBus = EventBusManager.getEvent(EventTypes.GENERATE_DWC_FILES.name());
            var generateDWCEvent = new GenerateDWCFileEvent(dwcService);
                        
            if(!ds.isSQLDatabase())
            {
                lblMessage.setText("Importando dados. Isso pode levar um tempo.");
                
                importDataTask = event.getTask();
                importDataTask.setOnCancelled((handler) -> {
                    ApplicationLog.info("Import data cancelled.");
                });
                importDataTask.setOnSucceeded((handler) -> {
                    ApplicationLog.info("Import data successfully");
                    System.gc();
                    Platform.runLater(()->
                    {
                        unbindProgress();
                        generateDWCFileBus.post(generateDWCEvent);
                    });
                });

                importDataTask.setOnFailed((handler)->{
                    ApplicationLog.info("Import data failed");
                    var ex = importDataTask.getException();
                    var msg = ex.getLocalizedMessage();
                    executor.shutdownNow();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                    
                });

                bindProgress(importDataTask.progressProperty());
                ApplicationLog.info("Starting import data thread");
                
                //submitTask(importDataTask);
            }
            else
            {
                ApplicationLog.info("Starting import data from SQL thread");
                generateDWCFileBus.post(generateDWCEvent);
            }
        } catch (Exception e)
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
    @Subscribe
    void onGenerateDWCFileEventTrigger(GenerateDWCFileEvent event)
    {
        try 
        {
            generateDWCATask = event.getTask();
            lblMessage.setText("Gerando arquivo, por favor aguarde.");
            generateDWCATask.setOnCancelled((handler) ->{
                ApplicationLog.info("Generate file task cancelled.");
        });
            generateDWCATask.setOnSucceeded((handler) -> {
                System.gc();
                ApplicationLog.info("Generate file successfully.");
                Platform.runLater(()->
                {
                    unbindProgress();
                    var transferDWCFileEvent = new TransferFileEvent(dwcService);
                    var transferDWCFileBus = EventBusManager.getEvent(EventTypes.TRANSFER_DATA.name());
                    rowCount = generateDWCATask.getTotalWork();
                    transferDWCFileBus.post(transferDWCFileEvent);
                });        
            });

            generateDWCATask.setOnFailed((handler)->{
                ApplicationLog.info("Generate file task failed.");
                Platform.runLater(()->{
                    executor.shutdownNow();
                    var ex = generateDWCATask.getException();
                    var msg = ex.getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                });
            });

            bindProgress(generateDWCATask.progressProperty());
                
            ApplicationLog.info("Starting Generate DWC file thread");
            submitTask(generateDWCATask);

        } catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
    @Subscribe
    void onTransferFileEventTrigger(TransferFileEvent event)
    {
        try 
        {
            transferFileTask = event.getTask();
            lblMessage.setText("Transferindo arquivo, por favor não feche o spLinker.");
            progressIndicator.setVisible(false);
            transferFileTask.setOnCancelled((handler) -> {
                executor.shutdownNow();
                ApplicationLog.info("Transfer file task cancelled.");
            });
            transferFileTask.setOnSucceeded((handler)->
            {
                System.gc();
                ApplicationLog.info("Transfer file successfully.");
                Platform.runLater(()-> {
                    
                    try 
                    {
                        
                        //executor.shutdown();        
                        var newData = new HashMap<String, String>(){{put("last_rowcount", String.valueOf(rowCount));
                                                                    put("updated_at", LocalDate.now().toString());
                                                                    put("token", token);}};
                        DataSetService.updateDataSource(newData);   
                    } catch (Exception e) {
                        Sentry.captureException(e);
                        ApplicationLog.error(e.getLocalizedMessage());
                        showErrorModal(e.getLocalizedMessage());
                    }
                    
                    showAlert(AlertType.INFORMATION, "Transferência Concluída","transferido com sucesso!");
                    navigateTo("home");

                    
                });
            });
            transferFileTask.setOnFailed((handler)->{
                ApplicationLog.info("Transfer file task failed.");
                System.gc();
                Platform.runLater(()->{
                    executor.shutdownNow();
                    var msg = transferFileTask.getException().getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                });
            });

            ApplicationLog.info("Starting send file thread");
            bindProgress(transferFileTask.progressProperty());
            submitTask(transferFileTask);
            
        }  catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle bundle)
    {  
        ApplicationLog.info("Tela de envio aberta");
        btnCancelTransfer.setVisible(false);
        progressBar.setVisible(false);
        progressIndicator.setVisible(false);

        try 
        {    
            var importDataBus = EventBusManager.getEvent(EventTypes.IMPORT_DATA.name());
            var generateDWCBus = EventBusManager.getEvent(EventTypes.GENERATE_DWC_FILES.name());
            var transferFileBus = EventBusManager.getEvent(EventTypes.TRANSFER_DATA.name());
            
            token = DataSetService.getCurrentToken();
            ds = DataSetService.getDataSet(token);
            dwcService = new DarwinCoreArchiveService(ds);
            importDataTask = new ImportDataTask(ds);
            generateDWCATask = new GenerateDarwinCoreArchiveTask(dwcService);
            transferFileTask = new TransferFileTask(dwcService);
        } 
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected void setScreensize() {
        getStage().setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        getStage().setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
