package br.org.cria.splinkerapp.controllers;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class FirstConfigDialogController extends AbstractController{
    @FXML
    Pane pane;

    @FXML
    Button btnNo;

    @FXML
    Button btnYes;

    @FXML
    void onYesButtonClicked()
    {
        var routeName = "central-service";
        var width = 350;
        var height = 200;
        var stage = getStage();
        var computerHasProxyConfigured = ProxyConfigRepository.isBehindProxyServer();
        
        if(computerHasProxyConfigured)
        {
            routeName = "proxy-config";
            width = 440;
            height = 400;
        }
        navigateTo(stage,routeName, width, height);
    }

    @FXML
    void onNoButtonClicked(){
        System.exit(0);
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
    
}
