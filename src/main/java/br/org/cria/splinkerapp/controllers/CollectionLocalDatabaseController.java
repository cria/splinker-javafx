package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
/*
 * Classe responsável pelo formulário de configuração de banco de dados
 */
public class CollectionLocalDatabaseController extends AbstractController implements Initializable{

    @FXML
    AnchorPane pane;
    @FXML
    TextField usernameField;
    @FXML
    TextField passwordField;
    @FXML
    TextField tablenameField;
    @FXML
    TextField portField;
    @FXML
    TextField hostAddressField;
    @FXML
    TextField dbNameField;
    @FXML
    ComboBox<DataSourceType>  databaseTypeField;
    @FXML
    Button saveBtn;


    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        var fileTypes = Arrays.asList(DataSourceType.CSV.name(), DataSourceType.Excel.name(), 
                        DataSourceType.LibreOfficeCalc.name(), DataSourceType.dBase.name());
        var options = Arrays.asList(DataSourceType.values()).stream().filter(e-> !fileTypes.contains(e)).toList();
        databaseTypeField.setItems(FXCollections.observableArrayList(options));
        
        try 
        {
            var ds = DataSourceRepository.getDataSource();
            databaseTypeField.setValue(ds.getType());    
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
        
    }


    @FXML
    void onSaveButtonClicked()
    {
            try 
        {
            var username = usernameField.getText();
            var password = passwordField.getText();
            var tableName = tablenameField.getText();
            var hostName = hostAddressField.getText();
            var databaseName = dbNameField.getText();
            var port = portField.getText();
            var type = databaseTypeField.getValue();
            DataSourceRepository.saveDataSource(type, hostName , port,databaseName,tableName,username,password);
            var routeName ="home";
            var width = 350;
            var height = 200;
            navigateTo(getStage(), routeName, width, height);
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() { return this.pane; }
    
}
