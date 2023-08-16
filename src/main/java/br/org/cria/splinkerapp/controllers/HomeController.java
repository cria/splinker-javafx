package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HomeController extends AbstractController{
    Service transferService;
    Stage modalStage;

    @FXML
    Pane pane;

    @FXML
    MenuBar menuBar;

    @FXML
    Button syncServerBtn;
    
    @FXML
    Button syncMetaDataBtn;

    Label lblMessage;

    public HomeController() {
        modalStage = new Stage();
    }

    @FXML
    void onSyncServerBtnClicked() throws Exception
    {
        transferService = new DarwinCoreArchiveService().transferData();
        if (transferService != null)
        {

            transferService.setOnFailed(event -> {
                var exception = transferService.getException();
                modalStage.hide();
                modalStage.close();
                showErrorModal(exception.getMessage());

            });
            transferService.setOnSucceeded(event -> {
                modalStage.hide();
                modalStage.close();

            });
            transferService.start();
            showTransferModal();

        }
    }
    void onCancelTransferButtonClicked()
    {
        if (transferService != null) {
            transferService.cancel();
        }
    }
    
    @FXML
    void onSyncMetadataBtnClicked(){ }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    void showErrorModal(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    void showTransferModal(){
        Stage modalStage = new Stage();
        modalStage.initOwner(getStage());
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
        Label label = new Label("Transferindo dados");
        label.setStyle("-fx-font-size: 24;");
        StackPane modalLayout = new StackPane(label);
        modalLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);"); 
        Scene modalScene = new Scene(modalLayout, 300, 200, Color.TRANSPARENT);
        modalStage.setScene(modalScene);
        modalStage.showAndWait();

    }
}
