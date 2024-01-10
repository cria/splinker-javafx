package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.tasks.GenerateDarwinCoreArchiveTask;
import br.org.cria.splinkerapp.tasks.ImportDataTask;
import br.org.cria.splinkerapp.tasks.TransferFileTask;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Pane;

public class FileTransferController extends AbstractController{

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

    @Override
    public void initialize(URL location, ResourceBundle bundle)
    {   
        var stage = getStage();
        stage.onShowingProperty().set(event -> {
        try 
        {   
            token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            var dwcService = new DarwinCoreArchiveService(ds);
            var transferFileTask = new TransferFileTask(dwcService);
            var generateDWCATask = new GenerateDarwinCoreArchiveTask(dwcService);
            if(!ds.isSQLDatabase())
            {
                lblMessage.setText("Importando dados. Isso pode levar um tempo.");
                var importDataTask = new ImportDataTask(ds);
                progressBar.progressProperty().bind(importDataTask.progressProperty());
                executor.execute(importDataTask);
                executor.awaitTermination(60,TimeUnit.SECONDS);
            }
                        
            lblMessage.setText("Gerando arquivo, por favor aguarde.");
            progressBar.progressProperty().bind(generateDWCATask.progressProperty());
            executor.execute(generateDWCATask);
            executor.awaitTermination(60,TimeUnit.SECONDS);
            lblMessage.setText("Transferindo arquivo, por favor não feche o spLinker.");
            progressBar.progressProperty().bind(transferFileTask.progressProperty());
            executor.execute(transferFileTask);
            executor.awaitTermination(60,TimeUnit.SECONDS);
            executor.shutdown();
            System.gc();
            showAlert(AlertType.INFORMATION, "Transferência Concluída", "transferido com sucesso!");
            navigateTo("home");
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
            });
        //stage.show();
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
