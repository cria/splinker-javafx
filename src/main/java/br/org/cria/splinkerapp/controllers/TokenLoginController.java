package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import io.sentry.Sentry;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class TokenLoginController extends AbstractController {

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
                bus.post(token);
                // if(hasNewVersion)
                // {
                //     navigateOrOpenNewWindowOnExistingDataSource("splinker-update", 260, 150, true);
                // }
                var routeName = "collection-database";
                switch(dsType)
                {
                    case Access:
                        routeName = "access-db-modal";
                        break;
                    case dBase:
                    case Excel:
                    case LibreOfficeCalc:
                    case CSV:
                    case Numbers:
                        routeName = "file-selection";
                        break;
                    default:
                        break;
                }
            
                
                if(!hasConfig)
                {
                    navigateTo(getStage(), routeName); 
                }
                else
                {
                    navigateTo(getStage(), "home");
                }
            }
             
        } 
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bus = EventBusManager.getEvent(EventTypes.ADD_DATASET.name());
        bus.register(this);

     }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
