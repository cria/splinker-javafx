package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class AccessDbController extends AbstractController {
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
            var pathIsNull = (path == null) || (path == "");
            if(pathIsNull)
            {
                showErrorModal("Caminho do arquivo não pode ser vazio");
                return;
            }
            var password = accessPasswordField.getText();
            token = System.getProperty("splinker_token");
            
            DataSetService.saveAccessDataSource(token, path, password);
            navigateTo(getStage(),"home");
        } 
        catch(Exception ex)
        {
            handleErrors(ex);
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
                        
            var img = new ImageView("images/select-file.png");
            img.setFitHeight(30);
            img.setFitWidth(30);
            btnSelectFile.setGraphic(img);
            btnSelectFile.setPadding(Insets.EMPTY);
            accessFilePathField.setDisable(true);
            token = TokenRepository.getCurrentToken();
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
            handleErrors(e);
        }
    }
    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
