package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.repositories.TokenRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;

public class TokenLoginController extends AbstractController implements Initializable{

    @FXML
    AnchorPane pane;
    @FXML
    Button btnLogin;
    @FXML
    TextField tokenField;

    @FXML
    void onButtonLoginClicked() 
    {
        try 
        {
            var token = tokenField.getText();
            var isTokenValid = TokenRepository.validateToken(token);
            if(isTokenValid)
            {
                TokenRepository.saveToken(token);
                navigateTo(getStage(), "datasource-selection",400,300);
                return;   
            }
                showAlert(AlertType.ERROR,"Token inválido", "O token digitado é inválido");
             
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try 
        {
            var token = TokenRepository.getToken();
            if(token != null)
            {
                tokenField.setText(token);
            }

        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }    
    }


}
