package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.SyncManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
            if (transferService != null) 
            {
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
            ApplicationLog.error(ex.getLocalizedMessage());
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
            if(ds.isAccessDb())
            {    
                openNewWindow("access-db-modal");
                return;
            }

            if(ds.isFile())
            {
                openNewWindow("file-selection");
                return;
            }

            openNewWindow("collection-database");
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onProxyConfigMenuOptionClick() {
        openNewWindow("proxy-config");
    }

    @FXML
    void onCentralServiceConfigMenuOptionClick() {
        openNewWindow("central-service");
    }

    @FXML
    void onDataSetAddMenuItemClick()
    {
        openNewWindow("token-login");
    }

    @FXML
    void onDeleteLocalConfigMenuItemClick() {
        try 
        {
            DatabaseSetup.deleteLocalDatabase();
            System.exit(0);
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onSyncMetaDataMenuItemClick() {
        try 
        {
            var token = DataSetService.getCurrentToken();
            var config = DataSetService.getConfigurationDataFromAPI(token);
            ConfigFacade.HandleBackendData(token, config);
        } catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
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
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @FXML
    void onDeleteDatasetButtonClick()
    {
        openNewWindow("delete-dataset");

    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            super.initialize(location, resources);
            getStage().focusedProperty().addListener(new ChangeListener<Boolean>()
                {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean onHidden, Boolean onShown)
                {
                    System.out.println("Foco na home!");
                }
                });

            // if(!DataSetService.hasConfiguration())
            // {
            // navigateTo(getStage(), "first-config-dialog", 330,150);
            // }
            token = DataSetService.getCurrentToken();
            var sources = DataSetService.getAllDataSets().stream();
            // this.pane.focusedProperty().addListener(
            //     (prop, oldNode, newNode) -> {
            //         });
            populateDatasetCombo(sources);
            var collName = DataSetService.getDataSet(token).getDataSetName();
            lblCollectionName.setText(collName);
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    private void populateDatasetCombo(Stream<DataSet> sources)
    {
        var options = sources.map(e -> e.getToken()).toList();
        cmbCollection.setItems(FXCollections.observableArrayList(options));
        cmbCollection.setValue(token);
    }

    @Override
    protected void setScreensize() 
    {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT + 150);
    }
}
