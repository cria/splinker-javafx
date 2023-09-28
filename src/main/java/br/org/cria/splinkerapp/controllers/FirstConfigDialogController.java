package br.org.cria.splinkerapp.controllers;
import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.services.implementations.ProxyConfigService;
import br.org.cria.splinkerapp.services.interfaces.IProxyConfigService;
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

    IProxyConfigService proxyService = new ProxyConfigService();

    @FXML
    void onYesButtonClicked(){
        var routeName = "central-service";
        var width = 350;
        var height = 200;
        var stage = getStage();

        if(ProxyConfigService.isBehindProxyServer())
        {
            routeName = "proxy-config";
            width = 440;
            height = 400;
            stage = getStage(); 
        }
        
        Router.getInstance().navigateTo(stage,routeName, width, height);
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
