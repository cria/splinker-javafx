package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.SyncManager;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
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
                });
                transferService.start();
                showTransferModal("Transferindo");
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
    protected Pane getPane() {
        return this.pane;
    }
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            // if(!DataSetService.hasConfiguration())
            // {
            // navigateTo(getStage(), "first-config-dialog", 330,150);
            // }

            // TODO: Tela de adicionar nova coleção (só um item no menu + alterar tela de
            // login)
            token = DataSetService.getCurrentToken();
            var sources = DataSetService.getAllDataSets().stream();
            var options = sources.map(e -> e.getToken()).toList();
            cmbCollection.setItems(FXCollections.observableArrayList(options));
            cmbCollection.setValue(token);
            var collName = DataSetService.getDataSet(token).getDataSetName();
            lblCollectionName.setText(collName);
        } catch (Exception e) {
            showErrorModal(e.getLocalizedMessage());
        }

    }

}
