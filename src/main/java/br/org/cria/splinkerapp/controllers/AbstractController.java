package br.org.cria.splinkerapp.controllers;

import javafx.concurrent.Service;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class AbstractController{
    protected Service transferService;
    protected Stage modalStage = new Stage();;
    
    protected abstract Pane getPane();

    protected Stage getStage(){
        
        try {
            var pane = getPane();
            var scene = pane.getScene();
            var stage = (Stage)scene.getWindow();
            stage.setResizable(false);
            return stage;    
        } catch (Exception e) {
            System.out.println("ERROR!\n");
            System.out.println(e);
            System.out.println("\n END ERROR!\n");
            e.printStackTrace();
        }
        return new Stage();
    }
     void showTransferModal(String modalText){
        modalStage.initOwner(getStage());
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
        Label label = new Label(modalText);
        label.setStyle("-fx-font-size: 24;");
        StackPane modalLayout = new StackPane(label);
        modalLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);"); 
        Scene modalScene = new Scene(modalLayout, 300, 200, Color.TRANSPARENT);
        modalStage.setScene(modalScene);
        modalStage.showAndWait();

    }
        void showErrorModal(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

}
