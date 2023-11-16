package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;

public class TokenLoginController extends AbstractController {

    @FXML
    AnchorPane pane;
    @FXML
    Button btnLogin;
    @FXML
    TextField tokenField;

    @FXML
    void onButtonLoginClicked() 
    {
        try 
        {
            var token = tokenField.getText();
            var apiConfig = TokenRepository.getConfigurationDataFromAPI(token);
            if(apiConfig != null)
            {
                var version = apiConfig.get("version").toString();
                var currentVersion = TokenRepository.getCurrentVersion();
                var hasNewVersion = Double.parseDouble(version) > Double.parseDouble(currentVersion);
                TokenRepository.saveBasicConfiguration(token, version);
                ConfigFacade.handleConfiguration(apiConfig);
                var dsType = DataSourceType.valueOf(apiConfig.get("data_source_type").toString());
                DataSourceRepository.saveDataSource(dsType, null, null, null, null, null, null, null);
                if(hasNewVersion)
                {
                    navigateOrOpenNewWindowOnExistingDataSource("splinker-update", 260, 150, true);
                }
                var routeName = "collection-database";
                var width = 364;
                var height = 360;
                switch(dsType)
                {
                    case Access:
                        routeName = "access-db-modal";
                        width = 424;
                        height = 200;
                        break;
                    case dBase:
                    case Excel:
                    case LibreOfficeCalc:
                    case CSV:
                        routeName = "file-selection";
                        width = 369;
                        height = 127;
                        break;
                    default:
                        break;
                }
        
                navigateTo(getStage(), routeName, width, height);
                return;
            }
                showAlert(AlertType.ERROR,"Token inválido", "O token digitado é inválido");
             
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try 
        {
            var token = TokenRepository.getToken();
            if(token != null)
            {
                tokenField.setText(token);
            }

        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }    
    }


}
