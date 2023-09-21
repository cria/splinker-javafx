package br.org.cria.splinkerapp.controllers;

import java.io.File;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.ExcelFileSourceParser;
import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.models.DataSource;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
    void onButtonSelectFileClicked() 
    {
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) 
        {
            filePath.setText(file.getAbsolutePath());
            try
            {
                var sourceParser = new ExcelFileSourceParser(file.getAbsolutePath());
                var dwcManager = new DarwinCoreArchiveService();
                sourceParser.createTableBasedOnSheet();
                sourceParser.insertDataIntoTable();
                dwcManager.readDataFromSource(new DataSource())
                .generateTXTFile()
                .generateZIPFile()
                .transferData();
                
            }
            catch (Exception ex)
            {
                    Alert dialog = new Alert(AlertType.ERROR);
                    dialog.setTitle("Erro");
                    dialog.setContentText(ex.getMessage());
                    dialog.show();
            }
        }
    }
    
    @FXML
    void onButtonSaveClicked()
    {
        var routeName = "home";
        var width = 350;
        var height = 2540;
        Router.getInstance().navigateTo(getStage(), routeName,width, height);
    }
}
