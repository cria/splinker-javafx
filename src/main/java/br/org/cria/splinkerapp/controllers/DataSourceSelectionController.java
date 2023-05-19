package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;



public class DataSourceSelectionController extends AbstractController implements Initializable {
    @FXML
    Pane pane;
    @FXML
    ComboBox dataSourceField = new ComboBox();
    @FXML
    Button btnSelectDataSource;
    
    @Override
    protected Pane getPane() {return this.pane;}






    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var options = Arrays.asList("MySQL", "Postgres", 
        "MSSQL Server", "Access", "Excel", "CSV","dBase(DBF)" ,"LibreOffice Calc");
       dataSourceField.setItems(FXCollections.observableArrayList(options));
    };
    
}
