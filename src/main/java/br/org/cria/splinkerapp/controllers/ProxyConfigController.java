package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class ProxyConfigController extends AbstractController
{

    @FXML
    Pane pane;
    @FXML
    TextField proxyUsername;
    @FXML
    PasswordField proxyPassword;
    @FXML
    TextField proxyAddress;
    @FXML
    TextField proxyPort;
    @FXML
    Button saveBtn;
    
    @FXML
    void onButtonSaveClicked()
    {
        try 
        {
            var hasConfig = DataSetService.hasConfiguration();
            var config = new ProxyConfiguration(proxyAddress.getText(), proxyPassword.getText(), 
                                        proxyPort.getText(), proxyUsername.getText());
            ProxyConfigRepository.saveProxyConfig(config);
            if(!hasConfig)
            {
                var routeName = "central-service";
                var width = 320;
                var height = 240;
                navigateTo(getStage(), routeName, width, height);
            }
            else
            {
                getStage().close();
            }
            
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
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
            var config = ProxyConfigRepository.getConfiguration();
            if(config != null)
            {
                proxyUsername.setText(config.getUsername());
                proxyAddress.setText(config.getAddress());
                proxyPort.setText(config.getPort());
                proxyPassword.setText(config.getPassword());
            }
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getMessage());
        }
        
    }
}
