package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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

    ExecutorService executor = Executors.newFixedThreadPool(10);
            
    @FXML
    void onBtnYesClicked()
    {
        var importDataEvent = new ImportDataEvent(ds);
        var importDataBus = EventBusManager.getEvent(EventTypes.IMPORT_DATA.name());

        btnYes.setVisible(false);
        btnNo.setVisible(false);
        btnCancelTransfer.setVisible(true);
        progressBar.setVisible(true);
        importDataBus.post(importDataEvent); 
    }

    @FXML
    void onBtnNoClicked()
    {
        try {
            navigateTo("home");
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        };
    }

    @FXML
    void onButtonCancelClicked()
    {
        try 
        {
            executor.shutdownNow();
            navigateTo("home");    
        } catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    
    @Subscribe
    void onImportDataEventTrigger(ImportDataEvent event)
    {
        try 
        {
            if(!ds.isSQLDatabase())
            {
                var importDataTask = event.getTask();
                var generateDWCFileBus = EventBusManager.getEvent(EventTypes.GENERATE_DWC_FILES.name());
                var generateDWCEvent = new GenerateDWCFileEvent(dwcService);
       
                importDataTask.setOnSucceeded((handler) -> {
                    generateDWCFileBus.post(generateDWCEvent);
                });
                lblMessage.setText("Importando dados. Isso pode levar um tempo.");
                progressBar.progressProperty().bind(importDataTask.progressProperty());
                executor.execute(importDataTask);
                executor.awaitTermination(360,TimeUnit.SECONDS);
                
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
            var generateDWCATask = event.getTask();
            var transferDWCFileEvent = new TransferFileEvent(dwcService);
            var transferDWCFileBus = EventBusManager.getEvent(EventTypes.TRANSFER_DATA.name());
            
            generateDWCATask.setOnSucceeded((handler) -> {
                    transferDWCFileBus.post(transferDWCFileEvent);
                });
            lblMessage.setText("Gerando arquivo, por favor aguarde.");
            progressBar.progressProperty().bind(generateDWCATask.progressProperty());
            executor.execute(generateDWCATask);
            executor.awaitTermination(360,TimeUnit.SECONDS);    
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
            progressBar.progressProperty().bind(transferFileTask.progressProperty());
            executor.execute(transferFileTask);
            executor.awaitTermination(60,TimeUnit.SECONDS);
            executor.shutdown();
            System.gc();
            showAlert(AlertType.INFORMATION, "Transferência Concluída", "transferido com sucesso!");
            navigateTo("home");    
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
        try 
        {   
            token = DataSetService.getCurrentToken();
            ds = DataSetService.getDataSet(token);
            dwcService = new DarwinCoreArchiveService(ds);
            //var importDataEvent = new ImportDataEvent(ds);
            // var generateDwcFileEvent = new GenerateDWCFileEvent(dwcService);
            // var transferDWCFileEvent = new TransferFileEvent(dwcService);
            // onImportDataEventTrigger(importDataEvent);
            // onGenerateDWCFileEventTrigger(generateDwcFileEvent);
            // onTransferFileEventTrigger(transferDWCFileEvent);
            var importDataBus = EventBusManager.getEvent(EventTypes.IMPORT_DATA.name());
            var generateDWCBus = EventBusManager.getEvent(EventTypes.GENERATE_DWC_FILES.name());
            var transferFileBus = EventBusManager.getEvent(EventTypes.TRANSFER_DATA.name());
            
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
