package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
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
    DataSource ds;
    @Override
    protected Pane getPane() {return pane;}
    
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
            var ds = DataSourceRepository.getDataSource();  
            DataSourceRepository.saveDataSource(ds.getType(), filePath.getText());
            if(!ds.isFile() && ds.getDataSourceFilePath() == null)
            {
                navigateTo(getStage(), "home", 231, 222);
            }
            
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getLocalizedMessage());
        }     
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try 
        {
            filePath.setEditable(false);
            ds = DataSourceRepository.getDataSource();
            if(ds.getDataSourceFilePath() == null)
            {
                btnSave.setDisable(true);            
                return;
            }
            filePath.setText(ds.getDataSourceFilePath());
        
        } 
        catch (Exception e) 
        {
            // TODO: handle exception
        }
    }
}
