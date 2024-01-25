package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Pane;

public class FileTransferController extends AbstractController {

    DataSet ds;
    DarwinCoreArchiveService dwcService;

    @FXML
    Button btnYes;

    @FXML
    Button btnNo;
    @FXML
    Pane pane;

    @FXML
    Label lblMessage;
    
    @FXML
    Button btnCancelTransfer;

    @FXML
    ProgressBar progressBar;

    @FXML
    ProgressIndicator progressIndicator;

    ExecutorService executor = Executors.newFixedThreadPool(8);
            
    @FXML
    void onBtnYesClicked()
    {
        var importDataEvent = new ImportDataEvent(ds);
        var importDataBus = EventBusManager.getEvent(EventTypes.IMPORT_DATA.name());

        btnYes.setVisible(false);
        btnNo.setVisible(false);
        btnCancelTransfer.setVisible(true);
        progressBar.setVisible(true);
        progressIndicator.setVisible(true);

        Platform.runLater(()-> { 
            try 
            {
                importDataBus.post(importDataEvent);     
            } catch (Exception e) {
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
        executor.shutdownNow();
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
                
                Task<Void> importDataTask = event.getTask();
                importDataTask.setOnSucceeded((handler) -> {
                    System.gc();
                    Platform.runLater(()->
                    {
                        progressBar.progressProperty().unbind();
                        progressIndicator.progressProperty().unbind();
                        generateDWCFileBus.post(generateDWCEvent);
                    });
                });

                importDataTask.setOnFailed((handler)->{
                    executor.shutdown();
                    var msg = importDataTask.getException().getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                    
                });

                progressBar.progressProperty().bind(importDataTask.progressProperty());
                progressIndicator.progressProperty().bind(importDataTask.progressProperty());
                var thread = new Thread(importDataTask);
                thread.setDaemon(true);
                executor.execute(thread);
            }
            else
            {
                generateDWCFileBus.post(generateDWCEvent);
            }
        } catch (Exception e)
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
    @Subscribe
    void onGenerateDWCFileEventTrigger(GenerateDWCFileEvent event)
    {
        try 
        {
            Task<Void> generateDWCATask = event.getTask();
            var thread = new Thread(generateDWCATask);

            lblMessage.setText("Gerando arquivo, por favor aguarde.");

            generateDWCATask.setOnSucceeded((handler) -> {
                System.gc();
                Platform.runLater(()->
                {
                    progressBar.progressProperty().unbind();
                    progressIndicator.progressProperty().unbind();
                    var transferDWCFileEvent = new TransferFileEvent(dwcService);
                    var transferDWCFileBus = EventBusManager.getEvent(EventTypes.TRANSFER_DATA.name());
                    
                    transferDWCFileBus.post(transferDWCFileEvent);
                });        
            });

            generateDWCATask.setOnFailed((handler)->{
                Platform.runLater(()->{
                    executor.shutdown();
                    var msg = generateDWCATask.getException().getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                });
            });

            progressBar.progressProperty().bind(generateDWCATask.progressProperty());
            progressIndicator.progressProperty().bind(generateDWCATask.progressProperty());
                
            
            thread.setDaemon(true);
            executor.execute(thread);

        } catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
    @Subscribe
    void onTransferFileEventTrigger(TransferFileEvent event)
    {
        try 
        {
            var transferFileTask = event.getTask();
            lblMessage.setText("Transferindo arquivo, por favor não feche o spLinker.");
            progressIndicator.setVisible(false);
            transferFileTask.setOnSucceeded((handler)->
            {
                System.gc();
                Platform.runLater(()-> {
                    executor.shutdown();
                    lblMessage.setText("Arquivo transferido.");
                    this.dialog.setOnCloseRequest((closeRequestHandler) -> 
                    {
                        navigateTo("home");
                    });
                    showAlert(AlertType.INFORMATION, "Transferência Concluída",
                                                     "transferido com sucesso!");
                });
            });
            transferFileTask.setOnFailed((handler)->{
                System.gc();
                Platform.runLater(()->{
                    executor.shutdown();
                    var msg = transferFileTask.getException().getLocalizedMessage();
                    ApplicationLog.error(msg);
                    showErrorModal(msg);
                });
            });

            var thread = new Thread(transferFileTask);
            thread.setDaemon(true);
            executor.execute(thread);
            
        }  catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle bundle)
    {  
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

            generateDWCBus.register(this);
            transferFileBus.register(this);
            importDataBus.register(this);
            
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    protected void setScreensize() {
        getStage().setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        getStage().setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
