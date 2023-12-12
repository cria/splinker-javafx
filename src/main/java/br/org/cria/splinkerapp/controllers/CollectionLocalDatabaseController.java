package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    Button saveBtn;


    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        
        try 
        {
            var token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            if(ds != null)
            {
                usernameField.setText(ds.getDbUser());
                passwordField.setText(ds.getDbPassword());
                tablenameField.setText(ds.getDbTableName());
                hostAddressField.setText(ds.getDbHost());
                dbNameField.setText(ds.getDbName());
                portField.setText(ds.getDbPort());
            }
            
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
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
            var ds = DataSetService.getDataSet(token);
            DataSetService.saveSQLDataSource(token, hostName, port, databaseName, tableName, username, password);
            var hasFilePath = ds.getDataSetFilePath() != null;
            var hasUserAndPass = ds.getDbUser() != null && ds.getDbPassword() != null;
            var hasConfig = hasFilePath || hasUserAndPass;
            if(!hasConfig)
            {
                navigateTo(getStage(), "home", 
                            WindowSizes.LARGE_SQUARE_SCREEN_WIDTH,
                            WindowSizes.LARGE_SQUARE_SCREEN_HEIGHT);
            }
            else
            {
                var stage = getStage();
                stage.close();
            }
            
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() { return this.pane; }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }    
}
