package br.org.cria.splinkerapp.dialogs;
import br.org.cria.splinkerapp.AbstractController;
import br.org.cria.splinkerapp.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

public class SpLinkerFirstConfigDialogController extends AbstractController{
    @FXML
    AnchorPane pane;

    @FXML
    Button btnNo;

    @FXML
    Button btnYes;

    @FXML
    void onYesClicked(){
        var stage = super.getStage();
        Router.getInstance().navigateTo(stage,"proxy-config",500,500);
    }

    @FXML
    void onNoClicked(){
        System.exit(0);
    }
    
}
