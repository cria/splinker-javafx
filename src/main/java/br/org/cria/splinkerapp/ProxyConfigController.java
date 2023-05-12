package br.org.cria.splinkerapp;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

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
    void saveProxyConfig(){

    }
}
