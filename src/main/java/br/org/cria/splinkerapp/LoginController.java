package br.org.cria.splinkerapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    AnchorPane pane;
    @FXML 
    TextField username;
    @FXML
    PasswordField password;
    @FXML
    Button loginButton;

    void doLogin(){
        Stage stage = (Stage) pane.getScene().getWindow();
        stage.close();
    }
}
