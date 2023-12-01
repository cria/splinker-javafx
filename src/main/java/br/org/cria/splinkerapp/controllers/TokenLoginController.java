package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;

public class TokenLoginController extends AbstractController {

    @FXML
    AnchorPane pane;
    @FXML
    Button btnAddToken;
    @FXML
    TextField tokenField;


    
    @FXML
    void onButtonAddTokenClicked() 
    {
        try 
        {
            var token = tokenField.getText();
            var hasConfig = DataSetService.hasConfiguration();
            var apiConfig = DataSetService.getConfigurationDataFromAPI(token);
            if(apiConfig != null)
            {
                // var version = apiConfig.get("version").toString();
                // var currentVersion = DataSetService.getCurrentVersion();
                // var hasNewVersion = Double.parseDouble(version) > Double.parseDouble(currentVersion);
                var collName = apiConfig.get("dataset_name").toString();
                var datasetAcronym = apiConfig.get("dataset_acronym").toString();
                var id = (int)Double.parseDouble(apiConfig.get("dataset_id").toString());
                DataSetService.setCurrentToken(token);
                var dsType = DataSourceType.valueOf(apiConfig.get("data_source_type").toString());
                DataSetService.saveDataSet(token, dsType, datasetAcronym, collName, id);
                ConfigFacade.HandleBackendData(token, apiConfig);
                // if(hasNewVersion)
                // {
                //     navigateOrOpenNewWindowOnExistingDataSource("splinker-update", 260, 150, true);
                // }
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
                    case Numbers:
                        routeName = "file-selection";
                        width = 369;
                        height = 127;
                        break;
                    default:
                        break;
                }
            
                
                if(!hasConfig)
                {
                    navigateTo(getStage(), routeName, width, height); 
                }
                else
                {
                    getStage().close();
                }
            }
             
        } 
        catch (Exception e) 
        {
            logger.error(e.getLocalizedMessage());
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) { }
}
