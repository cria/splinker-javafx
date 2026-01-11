package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import javafx.fxml.FXML;

import java.net.URL;
import java.util.ResourceBundle;

public class FirstConfigDialogController extends AbstractController {

    boolean computerHasProxyConfigured;

    @FXML
    void onYesButtonClicked() {
        try {
            var routeName = computerHasProxyConfigured ? "proxy-config" : "token-login";
            System.out.println(computerHasProxyConfigured);
            var stage = getStage();
            navigateTo(stage, routeName);
        } catch (Exception e) {
            handleErrors(e);
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        computerHasProxyConfigured = ProxyConfigRepository.isBehindProxyServer();
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
