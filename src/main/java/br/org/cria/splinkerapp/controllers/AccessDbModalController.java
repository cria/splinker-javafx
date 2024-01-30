package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import io.sentry.Sentry;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class AccessDbModalController extends AbstractController {
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
   
    @FXML
    void onBtnSaveClicked()
    {
        try
        {
            var path = file.getAbsolutePath();
            var password = accessPasswordField.getText();
            token = System.getProperty("splinker_token");
            
            DataSetService.saveAccessDataSource(token, path, password);
            navigateTo(getStage(),"home");
        } 
        catch(Exception ex)
        {
            ApplicationLog.error(ex.getLocalizedMessage());
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
            accessFilePathField.setDisable(true);
            token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            if(ds.getDataSetFilePath() != null && ds.isAccessDb())
            {
                file = new File(ds.getDataSetFilePath());
                accessFilePathField.setText(ds.getDataSetFilePath());
                accessPasswordField.setText(ds.getDbPassword());
            }
            else
            {
                btnSave.setDisable(true);
            }
            super.initialize(location, resources);
        } 
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getMessage());
        }
    }
    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
