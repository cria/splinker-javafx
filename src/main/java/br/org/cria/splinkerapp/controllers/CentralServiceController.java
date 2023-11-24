package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class CentralServiceController extends AbstractController {

    @FXML
    Pane pane;
    @Override
    protected Pane getPane() { 
        return this.pane; 
    }
    @FXML
    Button btnSave;
    @FXML
    TextField urlField;
    
    @FXML
    void onButtonSavedClick() 
    {
       try 
       {
            var hasConfig = DataSetService.hasConfiguration();
            var systemVersion = CentralServiceRepository.getCurrentVersion();
            CentralServiceRepository.saveCentralServiceData(urlField.getText(), systemVersion);
            if(!hasConfig)
            {
                 navigateTo(this.getStage(), "token-login",280,150);
            }
            else
            {
                getStage().close();
            }
       } 
       catch (Exception e) 
       {
            showErrorModal(e.getMessage());
       }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try 
        {
            var centralServiceConfig = CentralServiceRepository.getCentralServiceData();
            if(centralServiceConfig != null)
            {
                urlField.setText(centralServiceConfig.getCentralServiceUrl());
            }    
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }    
    }
    
}
