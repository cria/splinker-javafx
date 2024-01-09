package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class FileSelectionController extends AbstractController {

    @FXML
    Pane pane;
    @FXML
    TextField filePath;
    @FXML
    Button btnSelectFile;
    @FXML
    Button btnSave;
    File file ;
    FileChooser fileChooser = new FileChooser();
    DataSet ds;
    
    @Override
    protected Pane getPane() {return this.pane;}
    
    @FXML
    void onButtonSelectFileClicked() throws Exception
    {
        file = fileChooser.showOpenDialog(getStage());
        if (file != null) 
        {
            filePath.setText(file.getAbsolutePath());
            btnSave.setDisable(false);
        }
    }
    
    @FXML
    void onButtonSaveClicked()
    {
        try 
        {
            DataSetService.saveSpreadsheetDataSource(token, filePath.getText());
            navigateTo(getStage(),"home");
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }     
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try 
        {
            super.initialize(location, resources);
            token = DataSetService.getCurrentToken();
            filePath.setEditable(false);
            ds = DataSetService.getDataSet(token);
            if(ds.getDataSetFilePath() == null)
            {
                btnSave.setDisable(true);            
                return;
            }
            filePath.setText(ds.getDataSetFilePath());
        
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
