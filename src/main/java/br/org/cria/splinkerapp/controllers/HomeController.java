package br.org.cria.splinkerapp.controllers;

import com.github.perlundq.yajsync.ui.YajsyncClient;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

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
