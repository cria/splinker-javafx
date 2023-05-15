package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class LoginController extends AbstractController{
    @FXML
    AnchorPane pane;
    @FXML 
    TextField username;
    @FXML
    PasswordField password;
    @FXML
    Button loginButton;

    @FXML
    Hyperlink lnkForgotPassword;
    @FXML
    Hyperlink lnkTokenLogin;

    @FXML
    void onForgotPasswordLoginClicked(){

    }

    @FXML
    void onTokenLoginClicked(){
        Router.getInstance().navigateTo(getStage(), "token-login");
    }
    @FXML
    void onLoginButtonClicked(MouseEvent event){
        try {
            Router.getInstance().navigateTo((Stage)pane.getScene().getWindow(), 
                                "proxy-config", 400, 300);
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(1);
        }
        
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
}
