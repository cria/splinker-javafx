package br.org.cria.splinkerapp.controllers;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.config.LockFileManager;
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
            var routeName = computerHasProxyConfigured ? "proxy-config" : "central-service";
            var stage = getStage();
            navigateTo(stage, routeName);    
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onNoButtonClicked(){
        LockFileManager.deleteLockfile();
        System.exit(0);
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
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
