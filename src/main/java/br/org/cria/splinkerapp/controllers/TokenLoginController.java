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
    Button btnDeleteToken;

    @FXML
    TextField tokenField;

    @FXML
    void onButtonDeleteTokenClicked()
    {
        try 
        {
            var token = tokenField.getText();
            var hasToken = (token != null) && (token != "");
            if(hasToken)
            {
                DataSetService.deleteDataSet(token);
                navigateTo(getStage(), "home");
            }
            else
            {
                showErrorModal("Token inválido");
            }
            
        } catch (Exception e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
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
                var routeName = "collection-database";
                var collName = apiConfig.get("dataset_name").toString();
                var datasetAcronym = apiConfig.get("dataset_acronym").toString();
                var id = (int)Double.parseDouble(apiConfig.get("dataset_id").toString());
                DataSetService.setCurrentToken(token);
                var dsType = DataSourceType.valueOf(apiConfig.get("data_source_type").toString());
                DataSetService.saveDataSet(token, dsType, datasetAcronym, collName, id);
                ConfigFacade.HandleBackendData(token, apiConfig);
                bus.post(token);
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
        btnDeleteToken.setVisible(false);
        // bus.register(this);
        try 
        {
            token = DataSetService.getCurrentToken();
            var hasToken = token != "";    
            if(hasToken)
            {
                btnDeleteToken.setVisible(true);
                
                btnAddToken.setLayoutX(56);
                return;
            }
            //centro da tela
            btnAddToken.setLayoutX(126);
            
        } catch (Exception e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());    
        }
     }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
