package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class AccessDbModalController extends AbstractController  implements Initializable {
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
            DataSourceRepository.saveDataSource(DataSourceType.Access, path, userName, password);
            var routeName ="home";
            var width = 231;
            var height = 222;
            navigateTo(getStage(), routeName, width, height);
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
        try 
        {
            var ds = DataSourceRepository.getDataSource();
            if(ds!= null)
            {
                var hasUserName = ds.getDbUser() != null;
                var hasPassword = ds.getDbPassword() != null;
                var hasFilePath = ds.getDataSourceFilePath() != null;
                if(hasFilePath)
                {
                    accessFilePathField.setText(ds.getDataSourceFilePath());
                }
                if(hasUserName)
                {
                    accessUsernameField.setText(ds.getDbUser());
                }
                if(hasPassword)
                {
                    accessPasswordField.setText(ds.getDbPassword());
                }
            }
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
        
        
        btnSave.setDisable(true);    
    }
    
}
