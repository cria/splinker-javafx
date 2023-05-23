package br.org.cria.splinkerapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class FileSelectionController extends AbstractController{

    @FXML
    Pane pane;
    @FXML
    Button btnSelectFile;
    @FXML
    Button btnSave;
    
    @Override
    protected Pane getPane() {return pane;}
    
    @FXML
    void onButtonSelectFileClicked(){}
    
    @FXML
    void onButtonSaveClicked(){}
    

}
