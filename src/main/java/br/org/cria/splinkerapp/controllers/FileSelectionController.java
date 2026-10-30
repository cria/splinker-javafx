package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class FileSelectionController extends AbstractController {

    @FXML
    Label sourceTypeTitle;
    @FXML
    Label sourceTypeDescription;
    @FXML
    Label remoteHelpLabel;
    @FXML
    HBox localBox;
    @FXML
    HBox remoteBox;
    @FXML
    TextField localFilePath;
    @FXML
    TextField remoteUrl;
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
            localFilePath.setText(file.getAbsolutePath());
            updateSaveButtonState();
        }
    }

    @FXML
    void onButtonSaveClicked() {
        try {
            String datasourcePath = getSelectedPath();
            if (datasourcePath == null || datasourcePath.trim().isEmpty()) {
                showErrorModal(I18n.text("validation.field.required"));
                return;
            }

            String normalizedPath = datasourcePath.trim();
            validateBeforeSave(normalizedPath);
            DataSetService.saveSpreadsheetDataSource(token, normalizedPath);
            navigateTo(getStage(), "home");
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    private void validateBeforeSave(String datasourcePath) throws Exception {
        if (ds.getType() == DataSourceType.GoogleSheets && !GoogleDriveFileService.isRemotePath(datasourcePath)) {
            throw new IllegalArgumentException(I18n.text("file.google.required"));
        }

        if (GoogleDriveFileService.isRemotePath(datasourcePath) && ds.getType() != DataSourceType.GoogleSheets) {
            throw new IllegalArgumentException(I18n.text("file.google.only"));
        }

        GoogleDriveFileService.validateAccess(datasourcePath);
    }

    private void configureButtons() {
        var img = new ImageView("/images/select-file.png");
        img.setFitHeight(30);
        img.setFitWidth(30);
        btnSelectFile.setGraphic(img);
        btnSelectFile.setPadding(Insets.EMPTY);
    }

    private void configureViewByDataSetType() {
        boolean googleSheetsSource = ds.getType() == DataSourceType.GoogleSheets;

        localBox.setVisible(!googleSheetsSource);
        localBox.setManaged(!googleSheetsSource);

        remoteBox.setVisible(googleSheetsSource);
        remoteBox.setManaged(googleSheetsSource);

        remoteHelpLabel.setVisible(googleSheetsSource);
        remoteHelpLabel.setManaged(googleSheetsSource);

        if (googleSheetsSource) {
            sourceTypeTitle.setText(I18n.text("file.link.title"));
            sourceTypeDescription.setText(I18n.text("file.link.description"));
            remoteUrl.setTooltip(new Tooltip(I18n.text("file.link.tooltip")));
            return;
        }

        sourceTypeTitle.setText(I18n.text("file.path"));
        sourceTypeDescription.setText(I18n.text("file.description"));
    }

    private void loadExistingValue() {
        String savedPath = ds.getDataSetFilePath();
        if (savedPath == null || savedPath.trim().isEmpty()) {
            updateSaveButtonState();
            return;
        }

        if (ds.getType() == DataSourceType.GoogleSheets) {
            remoteUrl.setText(savedPath);
        } else {
            localFilePath.setText(savedPath);
        }

        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        String selectedPath = getSelectedPath();
        btnSave.setDisable(selectedPath == null || selectedPath.trim().isEmpty());
    }

    private String getSelectedPath() {
        return ds.getType() == DataSourceType.GoogleSheets ? remoteUrl.getText() : localFilePath.getText();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            super.initialize(location, resources);
            token = TokenRepository.getCurrentToken();
            ds = DataSetService.getDataSet(token);

            configureButtons();
            configureViewByDataSetType();

            localFilePath.textProperty().addListener((__, ___, ____) -> updateSaveButtonState());
            remoteUrl.textProperty().addListener((__, ___, ____) -> updateSaveButtonState());

            loadExistingValue();
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT + 40);
    }
}
