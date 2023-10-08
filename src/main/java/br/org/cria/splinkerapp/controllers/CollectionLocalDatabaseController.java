package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.managers.DatabaseSourceManager;
import br.org.cria.splinkerapp.models.DataSourceType;
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
       //TODO: Chamar API passando o token e puxa configuração inicial
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
            DatabaseSourceManager.processData(DataSourceType.MySQL, hostName, databaseName, 
                                                        tableName, username, password, port);
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() { return this.pane; }
    
}
