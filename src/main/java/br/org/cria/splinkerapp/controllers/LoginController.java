package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
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
        navigateTo(getStage(), "token-login",300,200);
    }
    @FXML
    void onLoginButtonClicked(MouseEvent event){
        String route = "home";
        Stage stage = getStage();
        int width = 400;
        int height = 300;
        try 
        {
            var config = ProxyConfigRepository.getConfiguration(); 
            if(config == null)
            {
                route = "first-config-dialog";
                width = 330;
                height = 150;
            }
            
            Router.getInstance().navigateTo(stage, route, width,height);
        } 
        catch (Exception e) 
        {
            System.out.println(e);
            System.exit(1);
        }
        
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
}
