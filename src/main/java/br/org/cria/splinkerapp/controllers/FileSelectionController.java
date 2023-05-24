package br.org.cria.splinkerapp.controllers;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class FileSelectionController extends AbstractController{

    @FXML
    Pane pane;
    @FXML
    TextField filePath;
    @FXML
    Button btnSelectFile;
    @FXML
    Button btnSave;
    FileChooser fileChooser = new FileChooser();
    @Override
    protected Pane getPane() {return pane;}
    
    @FXML
    void onButtonSelectFileClicked(){
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
        filePath.setText(file.getAbsolutePath());
        }
    }
    
    @FXML
    void onButtonSaveClicked(){}
    

}
