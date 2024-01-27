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
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class AccessDbModalController extends AbstractController {
    @FXML
    Pane pane;
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
            token = System.getProperty("splinker_token");
            var path = file.getAbsolutePath();
            
            var password = accessPasswordField.getText();
            var ds = DataSetService.getDataSet(token);
            DataSetService.saveAccessDataSource(token, path, password);
            
            if(ds.getDataSetFilePath() == null && ds.isAccessDb())
            {
                navigateTo(getStage(),"home");
            }
            else
            {
                var stage = getStage();
                stage.close();
            }
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
        super.initialize(location, resources);
        accessFilePathField.setDisable(true);
        try 
        {
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
