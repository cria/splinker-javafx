package br.org.cria.splinkerapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

public class FieldMappingController extends AbstractController{

    @FXML
    Pane pane;
    @FXML
    TableView<String> fieldMappinTable;
    @FXML
    Button btnSave;

    @FXML
    void onButtonSaveClick(){}

    @Override
    protected Pane getPane() 
    {
        return this.pane;
    }
    
}
