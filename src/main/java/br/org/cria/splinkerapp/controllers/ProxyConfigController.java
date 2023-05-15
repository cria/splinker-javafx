package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.services.ProxyConfigService;
import br.org.cria.splinkerapp.services.interfaces.IProxyConfigService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class ProxyConfigController extends AbstractController{

    @FXML
    AnchorPane pane;
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
    
    IProxyConfigService proxyService;

    public ProxyConfigController(){
        proxyService = new ProxyConfigService();
        proxyService.getConfiguration();
    }


    @FXML
    void onLinkNoProxyClicked(){

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
}
