package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
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
            var tokenToBeDeleted = tokenField.getText();
            var tokenIsNotEmpty = (tokenToBeDeleted != null) && (tokenToBeDeleted.trim().isEmpty());
            var tokenExists = DataSetService.getDataSet(tokenToBeDeleted) != null;
            if(tokenIsNotEmpty && tokenExists)
            {
                DataSetService.deleteDataSet(tokenToBeDeleted);
                var datasets = DataSetService.getAllDataSets();
                token = datasets.getFirst().getToken();
                TokenRepository.setCurrentToken(token);
                navigateTo(getStage(), "home");
            }
            else
            {
                showErrorModal("Token inválido ou inexistente!");
            }
            
        } catch (Exception e) {
            handleErrors(e);
        }
    }
    
    @FXML
    void onButtonAddTokenClicked() 
    {
        try 
        {
            
            var newToken = tokenField.getText();
            if(newToken == null || newToken.trim().isEmpty())
            {
                showErrorModal("Token não pode ser vazio!");
                return;
            }
            var tokenExists = DataSetService.getDataSet(newToken) != null;
            var hasConfig = DataSetService.hasConfiguration();
            if(tokenExists && hasConfig)
            {
                showErrorModal("Token já existente!");
                return;
            }
            var apiConfig = DataSetService.getConfigurationDataFromAPI(newToken);
            if(apiConfig != null)
            {
                var routeName = "collection-database";
                var collName = apiConfig.get("dataset_name").toString();
                var datasetAcronym = apiConfig.get("dataset_acronym").toString();
                var id = (int)Double.parseDouble(apiConfig.get("dataset_id").toString());
                TokenRepository.setCurrentToken(newToken);
                var dsType = DataSourceType.valueOf(apiConfig.get("data_source_type").toString());
                if (!tokenExists) {
                    DataSetService.saveDataSet(newToken, dsType, datasetAcronym, collName, id);
                }
                ConfigFacade.HandleBackendData(newToken, apiConfig);
                bus.post(newToken);
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
            handleErrors(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        super.initialize(location, resources);
        bus = EventBusManager.getEvent(EventTypes.ADD_DATASET.name());
        btnDeleteToken.setVisible(false);
        try 
        {
            token = TokenRepository.getCurrentToken();
            var hasToken = token.trim().isEmpty();
            if(hasToken)
            {
                btnDeleteToken.setVisible(true);
                
                btnAddToken.setLayoutX(56);
                return;
            }
            //centro da tela
            btnAddToken.setLayoutX(126);
            
        } catch (Exception e) {
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
