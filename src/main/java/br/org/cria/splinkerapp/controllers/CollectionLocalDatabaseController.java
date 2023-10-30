package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
/*
 * Classe responsável pelo formulário de configuração de banco de dados
 */
public class CollectionLocalDatabaseController extends AbstractController {

    @FXML
    AnchorPane pane;
    @FXML
    TextField usernameField;
    @FXML
    PasswordField passwordField;
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
        var allValues = DataSourceType.values();
        var fileTypes = Arrays.asList(DataSourceType.CSV, DataSourceType.Excel, 
                        DataSourceType.LibreOfficeCalc, DataSourceType.dBase, 
                        DataSourceType.Access);
        var options = Arrays.asList(allValues).stream().filter(e-> !fileTypes.contains(e)).toList();
        databaseTypeField.setItems(FXCollections.observableArrayList(options));
        
        try 
        {
            var ds = DataSourceRepository.getDataSource();
            if(ds != null)
            {
                usernameField.setText(ds.getDbUser());
                passwordField.setText(ds.getDbPassword());
                tablenameField.setText(ds.getDbTableName());
                hostAddressField.setText(ds.getDbHost());
                dbNameField.setText(ds.getDbName());
                portField.setText(ds.getDbPort());
                databaseTypeField.setValue(ds.getType());    
            }
            
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
            var ds = DataSourceRepository.getDataSource();
            DataSourceRepository.saveDataSource(type,null, hostName , port,databaseName,tableName,username,password);
            var hasFilePath = ds.getDataSourceFilePath() != null;
            var hasUserAndPass = ds.getDbUser() != null && ds.getDbPassword() != null;
            var hasConfig = hasFilePath || hasUserAndPass;
            if(!hasConfig)
            {
                var routeName ="home";
                var width = 231;
                var height = 222;
                navigateTo(getStage(), routeName, width, height);
            }
            else
            {
                var stage = getStage();
                stage.close();
            }
            
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() { return this.pane; }
    
}
