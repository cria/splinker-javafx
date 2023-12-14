package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.managers.SyncManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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

    EventBus addDatasetBus;
    @Override
    protected Pane getPane() { return this.pane; }
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
    
    @FXML
    void onDeleteDatasetButtonClick()
    {
        openNewWindow("delete-dataset");
    }
    
    @FXML
    void onCmbCollectionChange(ActionEvent event) {

        try 
        {
            var token = cmbCollection.getValue();
            if(token!=null)
            {
                DataSetService.setCurrentToken(token);
                var dataSet = DataSetService.getDataSet(token);
                lblCollectionName.setText(dataSet.getDataSetName());
            }
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        
        try {
            addDatasetBus = EventBusManager.getEvent(EventTypes.ADD_DATASET.name());
            bus = EventBusManager.getEvent(EventTypes.DELETE_DATASET.name());
            
            addDatasetBus.register(this);
            bus.register(this);
            // if(!DataSetService.hasConfiguration())
            // {
            // navigateTo(getStage(), "first-config-dialog", 330,150);
            // }
            token = DataSetService.getCurrentToken();
            // this.pane.focusedProperty().addListener(
            //     (prop, oldNode, newNode) -> {
            //         });
            populateDatasetCombo();
            var collName = DataSetService.getDataSet(token).getDataSetName();
            lblCollectionName.setText(collName);
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    @Subscribe
    void reloadDatasetListAfterChange(String eventToken)
    {
        try 
        {
            var ds = DataSetService.getDataSet(eventToken);
            var datasetWasDeleted = ds.getId() <= 0;
            if(datasetWasDeleted)
            {
                cmbCollection.getItems().remove(eventToken);
                var size = cmbCollection.getItems().size();
                var newToken = cmbCollection.getItems().get(size-1);
                DataSetService.setCurrentToken(newToken);
                cmbCollection.setValue(newToken);
            }
            else
            {
                populateDatasetCombo();
                cmbCollection.setValue(eventToken);
            }
            
            
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    private void populateDatasetCombo() throws Exception
    {
            var sources = DataSetService.getAllDataSets().stream();
            addOptionsToDataset(sources);
    }

    private void addOptionsToDataset(Stream<DataSet> sources)
    {
        try {
        var options = sources.map(e -> e.getToken()).toList();
        cmbCollection.setItems(FXCollections.observableArrayList(options));
        cmbCollection.setValue(token);    
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
        }
        
    }
   
    @Override
    protected void setScreensize() 
    {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT + 150);
    }
}
