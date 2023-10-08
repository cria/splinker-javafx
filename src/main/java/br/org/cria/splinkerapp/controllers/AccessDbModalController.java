package br.org.cria.splinkerapp.controllers;

import java.io.File;

import br.org.cria.splinkerapp.managers.DatabaseSourceManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class AccessDbModalController extends AbstractController {
    @FXML
    Pane pane;
    @FXML
    TextField accessUsernameField;
    @FXML
    TextField accessPasswordField;
    @FXML
    TextField accessFilePathField;
    @FXML
    Button btnSave;
    @FXML
    Button btnSelectFile;
    FileChooser fileChooser = new FileChooser();
    @Override
    protected Pane getPane() {
        return pane;
    }

    @FXML
    void onBtnSelectFileClick()
    {
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) 
        {
            accessFilePathField.setText(file.getAbsolutePath());
            try
            {
                var path = file.getAbsolutePath();
                var userName = accessUsernameField.getText();
                var password = accessPasswordField.getText();
                transferService = DatabaseSourceManager.processData(path, userName, password);
            } 
            catch(Exception ex)
            {
                showErrorModal(ex.getMessage());
            }
        }
    }
    
}
