package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class CentralServiceController extends AbstractController {

    @FXML
    Button btnSave;
    @FXML
    TextField urlField;

    @FXML
    void onButtonSavedClick() {
        try {
            var centralServiceURL = urlField.getText();
            var isUrlEmpty = (centralServiceURL == null) || centralServiceURL.trim().isEmpty();
            if (isUrlEmpty) {
                showErrorModal("URL do serviço central não pode ser vazia");
                return;
            }
            var hasConfig = DataSetService.hasConfiguration();
            var routeName = hasConfig ? "home" : "token-login";
            var systemVersion = CentralServiceRepository.getCurrentVersion();

            CentralServiceRepository.saveCentralServiceData(centralServiceURL, systemVersion);
            navigateTo(this.getStage(), routeName);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            super.initialize(location, resources);
            var centralServiceConfig = CentralServiceRepository.getCentralServiceData();
            urlField.setText(centralServiceConfig.getCentralServiceUrl());

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
