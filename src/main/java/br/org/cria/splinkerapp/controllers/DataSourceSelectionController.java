package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.models.DataSourceType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;




public class DataSourceSelectionController extends AbstractController implements Initializable {
    
    @FXML
    Pane pane;
    @FXML
    ComboBox<DataSourceType> dataSourceField; 
    @FXML
    Button btnSelectDataSource;    
    @Override
    protected Pane getPane() {return this.pane;}

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
       var options = Arrays.asList(DataSourceType.values());
       dataSourceField.setItems(FXCollections.observableArrayList(options));
       //TODO: Chamar API passando o token e puxa configuração inicial
    }

    @FXML
    void onButtonSelectDataSourceClicked()
    {
        var width = 360;
        var height = 200;
        var routeName = "file-selection";
        var selectedValue = dataSourceField.getValue();
        
        
        if(selectedValue == null)
        {
            Alert dialog = new Alert(AlertType.ERROR);
            dialog.setTitle("Fonte de dados não especificada");
            dialog.setContentText("Selecione uma fonte de dados");
            dialog.show();
            return;
        }

        switch (selectedValue) 
        {
            case MySQL:
            case PostgreSQL:
            case SQLServer:
                routeName = "collection-database";
                height = 340;
                width = 340;
                break;
            default:
                break;
        }
        navigateTo(routeName, width, height);
    }
    
}
