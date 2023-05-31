package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.services.ProxyConfigService;
import br.org.cria.splinkerapp.services.interfaces.IProxyConfigService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class ProxyConfigController extends AbstractController implements Initializable{

    @FXML
    Pane pane;
    @FXML
    TextField proxyUsername;
    @FXML
    TextField proxyPassword;
    @FXML
    TextField proxyAddress;
    @FXML
    TextField proxyPort;
    @FXML
    Button saveBtn;
    
    @FXML
    Hyperlink lnkNoProxy;
    
    IProxyConfigService proxyService = new ProxyConfigService();

    @FXML
    void onLinkNoProxyClicked(){
        var routeName = "central-service";
        var width = 320;
        var height = 240;
        Router.getInstance().navigateTo(getStage(), routeName, width, height);
    }
    @FXML
    void onButtonSaveClicked(){
        var config = new ProxyConfiguration(proxyAddress.getText(), proxyPassword.getText(), 
                                        proxyPort.getText(), proxyUsername.getText());

        proxyService.saveProxyConfig(config);
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        var config = proxyService.getConfiguration();
        if(config != null)
        {
            proxyUsername.setText(config.getUsername());
            proxyAddress.setText(config.getAddress());
            proxyPort.setText(config.getPort());
            proxyPassword.setText(config.getPassword());
        }
    }
}
