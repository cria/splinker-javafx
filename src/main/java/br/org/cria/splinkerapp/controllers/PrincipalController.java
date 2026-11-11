package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.MetadataSyncService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;


public class PrincipalController extends AbstractController {

    @FXML
    Button syncServerBtn;
    @FXML
    ComboBox<String> cmbCollection;
    @FXML
    Label lblCollectionName;
    @FXML
    Label lblLastUpdate;
    @FXML
    Label lblRecordsSent;
    @FXML
    Label lblMetadataStatus;
    EventBus addDatasetBus;

    DataSet ds;
    private boolean populatingCollections;
    private long refreshSequence;

    @FXML
    void onSyncServerBtnClicked() throws Exception {
        Collection<String> tokens = TokenRepository.getTokens();
        if (tokens.size() == 1) {
            navigateTo("file-transfer");
        } else {
            navigateTo("batch-select-file-transfer");
        }

    }

    @FXML
    void onDeleteLocalConfigMenuItemClick() {
        try {
            DatabaseSetup.deleteLocalDatabase();
            System.exit(0);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void syncMetaData() {
        if (ds == null) return;
        var requestedToken = ds.getToken();
        long sequence = ++refreshSequence;
        lblMetadataStatus.setText(I18n.text("dashboard.refreshing"));
        MetadataSyncService.getInstance().refresh(requestedToken, false)
                .whenComplete((unused, error) -> Platform.runLater(() -> {
                    // A previous collection's response must never replace the current one.
                    if (sequence != refreshSequence || !requestedToken.equals(token)
                            || pane.getScene() == null) return;
                    if (error != null) {
                        lblMetadataStatus.setText(I18n.text("dashboard.refresh.failed"));
                        return;
                    }
                    try {
                        ds = DataSetService.getDataSet(requestedToken);
                        if (ds != null) {
                            updateDisplayedData();
                            populateDatasetCombo();
                        }
                        lblMetadataStatus.setText("");
                    } catch (Exception e) {
                        lblMetadataStatus.setText(I18n.text("dashboard.refresh.failed"));
                    }
                }));
    }

    @FXML
    void onDeleteDatasetButtonClick() {
        navigateTo(getStage(), "delete-dataset");
    }

    @FXML
    void onCmbCollectionChange(ActionEvent event) {
        if (populatingCollections) return;
        try {
            var acronym = cmbCollection.getValue();
            if (acronym != null) {
                ds = DataSetService.getDataSetBy("dataset_acronym", acronym);
                token = ds.getToken();
                TokenRepository.setCurrentToken(token);
                updateDisplayedData();
                syncMetaData();
            }
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Subscribe
    void reloadDatasetListAfterChange(String eventToken) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> reloadDatasetListAfterChange(eventToken));
            return;
        }
        try {
            var currentDataset = DataSetService.getDataSet(eventToken);
            var datasetWasDeleted = currentDataset == null || currentDataset.getId() <= 0;
            if (datasetWasDeleted) {
                var remaining = DataSetService.getAllDataSets().stream()
                        .filter(java.util.Objects::nonNull).toList();
                ds = remaining.stream().filter(item -> item.getToken().equals(token))
                        .findFirst().orElse(remaining.isEmpty() ? null : remaining.getLast());
            } else {
                ds = currentDataset;
            }
            ++refreshSequence;
            if (ds == null) {
                token = "";
                TokenRepository.setCurrentToken(token);
                cmbCollection.getItems().clear();
                lblCollectionName.setText("-");
                lblLastUpdate.setText("-");
                lblRecordsSent.setText("-");
                lblMetadataStatus.setText("");
                syncServerBtn.setDisable(true);
                return;
            }
            syncServerBtn.setDisable(false);
            token = ds.getToken();
            TokenRepository.setCurrentToken(token);
            populateDatasetCombo();
            updateDisplayedData();
            syncMetaData();
        } catch (Exception e) {
            handleErrors(e);
        }

    }

    void populateDatasetCombo() throws Exception {
        var sources = DataSetService.getAllDataSets().stream();
        addOptionsToDataset(sources);
        sources.close();
    }

    void addOptionsToDataset(Stream<DataSet> sources) {
        populatingCollections = true;
        try {

            var options = sources.map(DataSet::getDataSetAcronym).toList();
            cmbCollection.setItems(FXCollections.observableArrayList(options));
            cmbCollection.setValue(ds.getDataSetAcronym());
        } catch (Exception e) {
            handleErrors(e);
        } finally {
            populatingCollections = false;
        }
    }

    void updateDisplayedData() {
        var lastUpdate = "-";
        if (ds.getUpdatedAt() != null) {
            lastUpdate = ds.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }

        var recordsSent = ds.getLastRowCount() > 0 ? String.valueOf(ds.getLastRowCount()) : "-";
        lblCollectionName.setText(ds.getDataSetName());
        lblLastUpdate.setText(lastUpdate);
        lblRecordsSent.setText(recordsSent);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            lblMetadataStatus.visibleProperty().bind(lblMetadataStatus.textProperty().isNotEmpty());
            lblMetadataStatus.managedProperty().bind(lblMetadataStatus.visibleProperty());
            addDatasetBus = EventBusManager.getEvent(EventTypes.ADD_DATASET.name());
            bus = EventBusManager.getEvent(EventTypes.DELETE_DATASET.name());
            addDatasetBus.register(this);
            bus.register(this);
            token = TokenRepository.getCurrentToken();
            ds = DataSetService.getDataSet(token);
            if (ds != null) {
                updateDisplayedData();
                populateDatasetCombo();
            }
            super.initialize(location, resources);
            lblCollectionName.setWrapText(true);
            lblCollectionName.setTextOverrun(OverrunStyle.CLIP);
            lblCollectionName.setEllipsisString("");
            syncMetaData();
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

}
