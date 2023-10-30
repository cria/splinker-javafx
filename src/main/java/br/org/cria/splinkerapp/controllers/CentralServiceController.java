package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.models.CentralService;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
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
    TextField uriField;
    
    @FXML
    void onButtonSavedClick() 
    {
       try 
       {
            var ds = DataSourceRepository.getDataSource();
            var cserv = new CentralService(uriField.getText(), urlField.getText());
            CentralServiceRepository.saveCentralServiceData(cserv);
            if(ds == null)
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
            var centralServiceConfig = CentralServiceRepository.getCentraServiceData();
            if(centralServiceConfig != null)
            {
                uriField.setText(centralServiceConfig.getCentralServiceUri());
                urlField.setText(centralServiceConfig.getCentralServiceUrl());
            }    
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }    
    }
    
}
