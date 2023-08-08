package br.org.cria.splinkerapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
public class HomeController extends AbstractController{
  
    @FXML
    Pane pane;

    @FXML
    MenuBar menuBar;

    @FXML
    Button syncServerBtn;
    
    @FXML
    Button syncMetaDataBtn;

    @FXML
    void onSyncServerBtnClicked() throws Exception
    {

    }
    
    @FXML
    void onSyncMetadataBtnClicked(){ }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
    
}
