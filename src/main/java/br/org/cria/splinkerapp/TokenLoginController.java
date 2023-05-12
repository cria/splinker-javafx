package br.org.cria.splinkerapp;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;

public class TokenLoginController extends AbstractController{

    @FXML
    Pane pane;
    @FXML
    Button btnLogin;
    @FXML
    TextField token;

    @FXML
    void onButtonLoginClicked(){
        var stage = getStage();
        Router.getInstance()
            .navigateTo(stage, "proxy-config",400,300);
    }


}
