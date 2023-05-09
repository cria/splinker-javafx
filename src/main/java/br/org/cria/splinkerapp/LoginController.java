package br.org.cria.splinkerapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
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
    @FXML
    void doLogin(MouseEvent event){
        try {
            Stage newStage = new Stage();
            newStage.setScene(Router.getInstance().routes.get("proxy-config"));
            newStage.setHeight(500);
            newStage.setWidth(500);
            newStage.show();
            var oldStage = (Stage) pane.getScene().getWindow();
            oldStage.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(1);
        }
        
    }
}
