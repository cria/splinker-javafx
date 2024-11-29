package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.enums.WindowSizes;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;

public class SpLinkerUpdateController extends AbstractController{

    @FXML
    Label lblMessage;
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
        var msg = 
                """
                O spLinker será fechado e a atualização ocorrerá em segundo plano.\n
                Você verá uma nova janela de instalação do spLinker em breve.
                """;
        lblMessage.setText(msg);
        btnYes.setVisible(false);
        btnNo.setVisible(false);
        getStage().setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        
        try 
        {
            new Thread(new Runnable(){

                @Override
                public void run() {
                    try {
                        Thread.sleep(9000);
                        new SpLinkerUpdateService().runSoftwareUpdate();        
                    } catch (Exception e) {
                        Platform.runLater(()->{
                            handleErrors(e);
                            throw new RuntimeException(e);
                        });
                    }
                    
                }}).run();
            
        } catch (Exception e) {
            handleErrors(e);
        }
    }
    
    @FXML
    void onBtnNoClicked()
    {
        try 
        {
            LockFileManager.deleteLockfile();
            System.exit(0);    
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
