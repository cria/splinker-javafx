package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import org.apache.poi.util.StringUtil;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
public class ProxyConfigController extends AbstractController
{

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
            var dataIsValid = validateData();
            if(!dataIsValid)
            {
                showErrorModal("Todos os campos são obrigatórios");
                return;
            }
            var hasConfig = DataSetService.hasConfiguration();
            var routeName = hasConfig ? "home" : "central-service";
            var config = new ProxyConfiguration(proxyAddress.getText(), proxyPassword.getText(), 
                                        proxyPort.getText(), proxyUsername.getText());
            ProxyConfigRepository.saveProxyConfig(config);
            navigateTo(getStage(), routeName);
            
        } 
        catch (Exception e) 
        {
            handleErrors(e);
        }
    }

    boolean validateData()
    {
        var hasAddress = StringUtil.isNotBlank(proxyAddress.getText());
        var hasPassword = StringUtil.isNotBlank(proxyPassword.getText());
        var hasUsername = StringUtil.isNotBlank(proxyUsername.getText());
        var hasPort = StringUtil.isNotBlank(proxyPort.getText());
        var isValid = hasAddress && hasPort && hasUsername && hasPassword;
        return isValid;
        
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try 
        {
            super.initialize(location, resources);
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
            handleErrors(e);
        }
        
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_SQUARE_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_SQUARE_SCREEN_HEIGHT);
    }
}
