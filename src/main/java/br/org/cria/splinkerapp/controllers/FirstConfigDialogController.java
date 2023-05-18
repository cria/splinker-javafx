package br.org.cria.splinkerapp.controllers;
import br.org.cria.splinkerapp.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class FirstConfigDialogController extends AbstractController{
    @FXML
    AnchorPane pane;

    @FXML
    Button btnNo;

    @FXML
    Button btnYes;

    @FXML
    void onYesButtonClicked(){
        var routeName = "proxy-config";
        var width = 440;
        var height = 400;
        var stage = getStage();
        Router.getInstance().navigateTo(stage,routeName, width, height);
    }

    @FXML
    void onNoButtonClicked(){
        System.exit(0);
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
    
}
