package br.org.cria.splinkerapp.controllers;
import br.org.cria.splinkerapp.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class SpLinkerFirstConfigDialogController extends AbstractController{
    @FXML
    AnchorPane pane;

    @FXML
    Button btnNo;

    @FXML
    Button btnYes;

    @FXML
    void onYesButtonClicked(){
        var stage = super.getStage();
        Router.getInstance().navigateTo(stage,"proxy-config",500,500);
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
