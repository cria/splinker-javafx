package br.org.cria.splinkerapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class CentralServiceController extends AbstractController{

    @FXML
    Pane pane;
    @Override
    protected Pane getPane() { return this.pane; }
    @FXML
    Button btnSave;
    @FXML
    TextField urlField;
    @FXML
    TextField uriField;
    
    @FXML
    void onButtonSavedClick(){

    }
    
}
