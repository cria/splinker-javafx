package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class SpLinkerUpdate  extends AbstractController{

    @FXML
    Pane pane;
    @FXML
    Button btnYes;
    @FXML
    Button btnNo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    @Override
    protected Pane getPane() {
        return pane;
    }

    @FXML
    void onBtnYesClicked()
    {

    }
    
    @FXML
    void onBtnNoClicked()
    {
        this.getStage().close();
    }
    
}
