package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
public class HomeController extends AbstractController{
    Service transferService;
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
        transferService = new DarwinCoreArchiveService().transferData();
    }
    void onCancelTransferButtonClicked()
    {
        if (transferService != null) {
            transferService.cancel();
        }
    }
    
    @FXML
    void onSyncMetadataBtnClicked(){ }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
    
}
