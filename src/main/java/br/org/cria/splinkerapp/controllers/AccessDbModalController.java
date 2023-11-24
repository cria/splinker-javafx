package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
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
            token = System.getProperty("token");
            var path = file.getAbsolutePath();
            var userName = accessUsernameField.getText();
            var password = accessPasswordField.getText();
            var ds = DataSetService.getDataSet(token);
            DataSetService.saveAccessDataSource(token, path, userName, password);
            
            if(ds.getDataSetFilePath() == null && ds.isAccessDb())
            {
                navigateTo(getStage(), "home", 231, 222);
            }
            else
            {
                var stage = getStage();
                stage.close();
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
            token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            if(ds.getDataSetFilePath() != null && ds.isAccessDb())
            {
                file = new File(ds.getDataSetFilePath());
                accessFilePathField.setText(ds.getDataSetFilePath());
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
