package br.org.cria.splinkerapp.controllers;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
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

    boolean computerHasProxyConfigured;

    @FXML
    void onYesButtonClicked()
    {
        try 
        {
            var routeName = "central-service";
            var width = 360;
            var height = 200;
            var stage = getStage();
            
            if(computerHasProxyConfigured)
            {
                routeName = "proxy-config";
                width = 350;
                height = 300;
            }
            navigateTo(stage,routeName, width, height);    
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onNoButtonClicked(){
        System.exit(0);
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        computerHasProxyConfigured = ProxyConfigRepository.isBehindProxyServer();
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
