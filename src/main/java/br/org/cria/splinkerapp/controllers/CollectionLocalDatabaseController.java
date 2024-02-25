package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
/*
 * Classe responsável pelo formulário de configuração de banco de dados
 */
public class CollectionLocalDatabaseController extends AbstractController {

    @FXML
    TextField usernameField;
    @FXML
    PasswordField passwordField;
    
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
            super.initialize(location, resources);
            var token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            if(ds != null)
            {
                usernameField.setText(ds.getDbUser());
                passwordField.setText(ds.getDbPassword());
                hostAddressField.setText(ds.getDbHost());
                dbNameField.setText(ds.getDbName());
                portField.setText(ds.getDbPort());
            }
            
        } 
        catch (Exception e) 
        {
            handleErrors(e);
        }
    }


    @FXML
    void onSaveButtonClicked()
    {
        try 
        {
            var username = usernameField.getText();
            var password = passwordField.getText();
            var hostName = hostAddressField.getText();
            var databaseName = dbNameField.getText();
            var port = portField.getText();
            token = DataSetService.getCurrentToken();
            DataSetService.saveSQLDataSource(token, hostName, port, databaseName, username, password);
            
            navigateTo(getStage(), "home");
            
        } 
        catch (Exception e) 
        {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }    
}
