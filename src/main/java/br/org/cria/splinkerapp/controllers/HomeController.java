package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.SyncManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Pane;

public class HomeController extends AbstractController {

    @FXML
    Pane pane;

    @FXML
    MenuBar menuBar;

    @FXML
    Button syncServerBtn;

    @FXML
    Button syncMetaDataBtn;

    @FXML
    Button btnDeleteDataset;

    @FXML
    ComboBox<String> cmbCollection;

    @FXML
    Label lblCollectionName;

    @FXML
    void onSyncServerBtnClicked() throws Exception {
        try {
            
            var token = DataSetService.getCurrentToken();
            transferService = SyncManager.SyncCollectionData(token);
            if (transferService != null) {
                transferService.setOnFailed(event -> {
                    var exception = transferService.getException();
                    modalStage.hide();
                    modalStage.close();
                    showErrorModal(exception.getMessage());
                });
                transferService.setOnSucceeded(event -> {
                    modalStage.hide();
                    modalStage.close();
                    showAlert(AlertType.INFORMATION, "Transferência concluída", "Arquivo transferido com sucesso");

                });
                showTransferModal("Transferindo");
                transferService.start();
                
            }

        } catch (IllegalStateException ex) {
            return;
        } catch (Exception ex) {
            showErrorModal(ex.toString());
        }
    }

    @FXML
    void onCancelTransferButtonClicked() {
        if (transferService != null) {
            transferService.cancel();
        }
    }

    @FXML
    void onDataSetConfigMenuItemClick() {
        try 
        {
            var ds = DataSetService.getDataSet(token);
            var width = 364;
            var height = 370;
            var routeName = "collection-database";
            if(ds.isAccessDb())
            {
                width = 424;
                height = 200;
                routeName = "access-db-modal";
            }
            if(ds.isFile())
            {
                width = 360;
                height = 200;
                routeName = "file-selection";
            }
            openNewWindow(routeName, width, height);
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onProxyConfigMenuOptionClick() {
        var width = 330;
        var height = 300;
        openNewWindow("proxy-config", width, height);
    }

    @FXML
    void onCentralServiceConfigMenuOptionClick() {
        openNewWindow("central-service", 360, 170);
    }

    @FXML
    void onDataSetAddMenuItemClick()
    {
        openNewWindow("token-login", 280, 150);
    }

    @FXML
    void onDeleteLocalConfigMenuItemClick() {
        try {
            DatabaseSetup.deleteLocalDatabase();
            System.exit(0);
        } catch (Exception e) {
            showErrorModal(e.getLocalizedMessage());
        }

    }

    @FXML
    void onSyncMetaDataMenuItemClick() {
        try {
            var token = DataSetService.getCurrentToken();
            var config = DataSetService.getConfigurationDataFromAPI(token);
            ConfigFacade.HandleBackendData(token, config);
        } catch (Exception e) {
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected Pane getPane() { return this.pane; }
    @FXML
    void onCmbCollectionChange(ActionEvent event) {

        try 
        {
            var token = cmbCollection.getValue();
            DataSetService.setCurrentToken(token);
            var dataSet = DataSetService.getDataSet(token);
            lblCollectionName.setText(dataSet.getDataSetName());

        } catch (Exception e) {
            showErrorModal(e.getLocalizedMessage());
        }

    }

    @FXML
    void onDeleteDatasetButtonClick()
    {
        openNewWindow("delete-dataset",300,200);

    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            // if(!DataSetService.hasConfiguration())
            // {
            // navigateTo(getStage(), "first-config-dialog", 330,150);
            // }
            token = DataSetService.getCurrentToken();
            var sources = DataSetService.getAllDataSets().stream();
            this.pane.focusedProperty().addListener(
                (prop, oldNode, newNode) -> {
                    //populateDatasetCombo(sources); 
                    System.out.println("Teste!!!!");});
            populateDatasetCombo(sources);
            var collName = DataSetService.getDataSet(token).getDataSetName();
            lblCollectionName.setText(collName);
        } catch (Exception e) {
            showErrorModal(e.getLocalizedMessage());
        }

    }
    private void populateDatasetCombo(Stream<DataSet> sources)
    {
            var options = sources.map(e -> e.getToken()).toList();
            cmbCollection.setItems(FXCollections.observableArrayList(options));
            cmbCollection.setValue(token);
    }
}
