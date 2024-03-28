package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.enums.WindowSizes;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;
public class SpLinkerUpdateController extends AbstractController{

    @FXML
    Button btnYes;
    @FXML
    Button btnNo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
    }

    @FXML
    void onBtnYesClicked()
    {
        SpLinkerUpdateService.verifyOSVersion();
        SpLinkerUpdateService.runSoftwareUpdate();
    }
    
    @FXML
    void onBtnNoClicked()
    {
        this.getStage().close();
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
    
}
