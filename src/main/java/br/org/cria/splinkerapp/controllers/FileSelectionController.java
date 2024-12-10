package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class FileSelectionController extends AbstractController {

    @FXML
    TextField filePath;
    @FXML
    Button btnSelectFile;
    @FXML
    Button btnSave;
    File file;
    FileChooser fileChooser = new FileChooser();
    DataSet ds;

    @FXML
    void onButtonSelectFileClicked() {
        file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            filePath.setText(file.getAbsolutePath());
            btnSave.setDisable(false);
        }
    }

    @FXML
    void onButtonSaveClicked() {
        try {
            var datasourcePath = filePath.getText();
            var hasPath = datasourcePath != null && !datasourcePath.trim().isEmpty();
            if (!hasPath) {
                showErrorModal("Campo obrigatório");
            }
            DataSetService.saveSpreadsheetDataSource(token, datasourcePath);
            navigateTo(getStage(), "home");
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {

            var img = new ImageView("/images/select-file.png");
            img.setFitHeight(30);
            img.setFitWidth(30);
            btnSelectFile.setGraphic(img);
            btnSelectFile.setPadding(Insets.EMPTY);

            super.initialize(location, resources);
            token = TokenRepository.getCurrentToken();
            filePath.setEditable(false);
            ds = DataSetService.getDataSet(token);
            if (ds.getDataSetFilePath() == null) {
                btnSave.setDisable(true);
                return;
            }
            filePath.setText(ds.getDataSetFilePath());

        } catch (Exception e) {
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
