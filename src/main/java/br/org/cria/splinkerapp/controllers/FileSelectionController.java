package br.org.cria.splinkerapp.controllers;

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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class FileSelectionController extends AbstractController {

    @FXML
    RadioButton localOption;
    @FXML
    RadioButton remoteOption;
    @FXML
    HBox localBox;
    @FXML
    HBox remoteBox;
    @FXML
    Label sourceTypeTitle;
    @FXML
    Label sourceTypeDescription;
    @FXML
    Label remoteHelpLabel;
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
    ToggleGroup sourceTypeGroup = new ToggleGroup();

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
                showErrorModal("Campo obrigatorio");
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

    @FXML
    void onSourceTypeChanged() {
        updateSourceTypeView();
        updateSaveButtonState();
    }

    private void validateBeforeSave(String datasourcePath) throws Exception {
        if (GoogleDriveFileService.isRemotePath(datasourcePath) && ds.getType() != DataSourceType.Excel) {
            throw new IllegalArgumentException("Links remotos sao suportados apenas para fontes Excel.");
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

    private void configureSourceTypeOptions() {
        localOption.setToggleGroup(sourceTypeGroup);
        remoteOption.setToggleGroup(sourceTypeGroup);

        boolean excelSource = ds.getType() == DataSourceType.Excel;
        remoteOption.setDisable(!excelSource);
        remoteUrl.setTooltip(new Tooltip("Cole aqui um link publico do Google Drive ou Google Sheets."));

        localFilePath.textProperty().addListener((__, ___, ____) -> updateSaveButtonState());
        remoteUrl.textProperty().addListener((__, ___, ____) -> updateSaveButtonState());

        if (!excelSource) {
            localOption.setSelected(true);
            localOption.setVisible(false);
            localOption.setManaged(false);
            remoteOption.setVisible(false);
            remoteOption.setManaged(false);
            sourceTypeTitle.setVisible(false);
            sourceTypeTitle.setManaged(false);
            sourceTypeDescription.setVisible(false);
            sourceTypeDescription.setManaged(false);
            remoteHelpLabel.setVisible(false);
            remoteHelpLabel.setManaged(false);
            remoteBox.setVisible(false);
            remoteBox.setManaged(false);
        }
    }

    private void loadExistingValue() {
        String savedPath = ds.getDataSetFilePath();
        if (savedPath == null || savedPath.trim().isEmpty()) {
            localOption.setSelected(true);
            updateSourceTypeView();
            updateSaveButtonState();
            return;
        }

        if (ds.getType() == DataSourceType.Excel && GoogleDriveFileService.isRemotePath(savedPath)) {
            remoteOption.setSelected(true);
            remoteUrl.setText(savedPath);
        } else {
            localOption.setSelected(true);
            localFilePath.setText(savedPath);
        }

        updateSourceTypeView();
        updateSaveButtonState();
    }

    private void updateSourceTypeView() {
        if (ds.getType() != DataSourceType.Excel) {
            localBox.setVisible(true);
            localBox.setManaged(true);
            remoteBox.setVisible(false);
            remoteBox.setManaged(false);
            return;
        }

        boolean remoteSelected = remoteOption.isSelected();

        localBox.setVisible(!remoteSelected);
        localBox.setManaged(!remoteSelected);

        remoteBox.setVisible(remoteSelected);
        remoteBox.setManaged(remoteSelected);
    }

    private void updateSaveButtonState() {
        String selectedPath = getSelectedPath();
        btnSave.setDisable(selectedPath == null || selectedPath.trim().isEmpty());
    }

    private String getSelectedPath() {
        return remoteOption.isSelected() ? remoteUrl.getText() : localFilePath.getText();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            super.initialize(location, resources);
            token = TokenRepository.getCurrentToken();
            ds = DataSetService.getDataSet(token);

            configureButtons();
            configureSourceTypeOptions();
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
