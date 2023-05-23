package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import br.org.cria.splinkerapp.Router;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;



public class DataSourceSelectionController extends AbstractController implements Initializable {
    
    private List<String> dbOptions = Arrays.asList("MySQL", "Postgres", "MSSQL Server");
    private List<String> fileOptions = Arrays.asList( "Access", "Excel",
                                    "CSV","dBase(DBF)","LibreOffice Calc");
        
    @FXML
    Pane pane;
    @FXML
    ComboBox<String> dataSourceField; 
    @FXML
    Button btnSelectDataSource;    
    @Override
    protected Pane getPane() {return this.pane;}

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
       var options = Stream.concat(dbOptions.stream(), fileOptions.stream()).toList();
       dataSourceField.setItems(FXCollections.observableArrayList(options));
    };
    
    @FXML
    void onButtonSelectDataSourceClicked()
    {
        var width = 360;
        var height = 200;
        var routeName = "file-selection";
        var selectedValue = dataSourceField.getValue();
        var isDatabaseSource = dbOptions.contains(selectedValue);
        
        if(selectedValue == null)
        {
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle("Fonte de dados não especificada");
            a.setContentText("Selecione uma fonte de dados");
            a.show();
            return;
        }

        if(isDatabaseSource)
        {
            routeName =  "collection-database";
            height = 400;
        }

        Router.getInstance().navigateTo(getStage(), routeName, width, height);
    }
    
}
