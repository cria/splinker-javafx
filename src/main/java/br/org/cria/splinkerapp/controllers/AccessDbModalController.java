package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class AccessDbModalController extends AbstractController {
    @FXML
    Pane pane;
    @FXML
    TextField accessUsernameField;
    @FXML
    PasswordField accessPasswordField;
    @FXML
    TextField accessFilePathField;
    @FXML
    Button btnSave;
    @FXML
    Button btnSelectFile;
    File file;
    FileChooser fileChooser = new FileChooser();
    @Override
    protected Pane getPane() {
        return pane;
    }
    @FXML
    void onBtnSaveClicked()
    {
        try
        {
            var path = file.getAbsolutePath();
            var userName = accessUsernameField.getText();
            var password = accessPasswordField.getText();
            var ds = DataSourceRepository.getDataSource();  
            DataSourceRepository.saveDataSource(DataSourceType.Access, path, userName, password);
            if(ds.getDataSourceFilePath() == null)
            {
                navigateTo(getStage(), "home", 350, 200);
            }
            
            
        } 
        catch(Exception ex)
        {
            showErrorModal(ex.getMessage());
        }
    }

    @FXML
    void onBtnSelectFileClick()
    {
        file = fileChooser.showOpenDialog(getStage());
        if (file != null) 
        {
            accessFilePathField.setText(file.getAbsolutePath());
            btnSave.setDisable(false);    
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        accessFilePathField.setDisable(true);
        try 
        {
            var ds = DataSourceRepository.getDataSource();
            
            if(ds.getDataSourceFilePath() != null)
            {
                file = new File(ds.getDataSourceFilePath());
                accessFilePathField.setText(ds.getDataSourceFilePath());
                accessUsernameField.setText(ds.getDbUser());
                accessPasswordField.setText(ds.getDbPassword());
            }
            else
            {
                btnSave.setDisable(true);
            }
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }
    
}
