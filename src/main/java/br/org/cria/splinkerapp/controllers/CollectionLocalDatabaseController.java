package br.org.cria.splinkerapp.controllers;

import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class CollectionLocalDatabaseController extends AbstractController{


    private static List<String> options = Arrays.asList("Access", "MySQL", "Postgres", 
                                                            "MSSQL Server", "Access", "Excel");

    public CollectionLocalDatabaseController(){
        datasourceField.setItems(FXCollections.observableArrayList(options));    
    }
    
    @FXML
    AnchorPane pane;
    @FXML
    TextField usernameField;
    @FXML
    TextField passwordField;
    @FXML
    ComboBox<String> datasourceField;
    @FXML
    TextField tablenameField;
    @FXML
    Button saveBtn;


    @FXML
    void onSaveButtonClicked(){}
    @Override
    protected Pane getPane() { return this.pane; }
    
}
