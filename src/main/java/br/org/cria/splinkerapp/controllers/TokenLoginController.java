package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.Router;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;

public class TokenLoginController extends AbstractController{

    @FXML
    AnchorPane pane;
    @FXML
    Button btnLogin;
    @FXML
    TextField token;

    @FXML
    void onButtonLoginClicked(){
        Router.getInstance()
            .navigateTo(getStage(), "proxy-config",400,300);
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }


}
