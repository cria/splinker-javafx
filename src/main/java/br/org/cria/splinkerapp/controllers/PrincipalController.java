package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import io.sentry.Sentry;
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
    EventBus addDatasetBus;
    
    DataSet ds;

    @FXML
    void onSyncServerBtnClicked() throws Exception 
    {
            navigateTo("file-transfer");
    }
  
    @FXML
    void onDeleteLocalConfigMenuItemClick() 
    {
        try 
        {
            DatabaseSetup.deleteLocalDatabase();
            System.exit(0);
        } catch (Exception e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    void syncMetaData() 
    {
        try 
        {
            var map = new HashMap<String, String>();
            var token = DataSetService.getCurrentToken();
            var config = DataSetService.getConfigurationDataFromAPI(token);
            var collName = config.get("dataset_name").toString();
            var datasetAcronym = config.get("dataset_acronym").toString();
            var id = (int)Double.parseDouble(config.get("dataset_id").toString());
            DataSetService.setCurrentToken(token);
            var dsType = DataSourceType.valueOf(config.get("data_source_type").toString());
            map.put("token", token);
            map.put("id", String.valueOf(id));
            map.put("dataset_name", collName);
            map.put("dataset_acronym", datasetAcronym);
            map.put("datasource_type", dsType.name());
            DataSetService.updateDataSource(map);
                
            ConfigFacade.HandleBackendData(token, config);
        } catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }
    
    @FXML
    void onDeleteDatasetButtonClick()
    {
        navigateTo(getStage(),"delete-dataset");
    }
    
    @FXML
    void onCmbCollectionChange(ActionEvent event) {

        try 
        {
            var newToken = cmbCollection.getValue();
            if(newToken!=null)
            {
                token = newToken;
                ds = DataSetService.getDataSet(token);
                var lastUdate = ds.getUpdatedAt() ==null? "-": ds.getUpdatedAt().toString();
                var recordsSent = ds.getLastRowCount() > 0? String.valueOf(ds.getLastRowCount()): "-";
                DataSetService.setCurrentToken(token);
                syncMetaData();
                lblCollectionName.setText(ds.getDataSetName());
                lblLastUpdate.setText(lastUdate);
                lblRecordsSent.setText(recordsSent);
            }
        } catch (Exception e) {
            Sentry.captureException(e);
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
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    
    void populateDatasetCombo() throws Exception
    {
            var sources = DataSetService.getAllDataSets().stream();
            addOptionsToDataset(sources);
            sources.close();
    }

    void addOptionsToDataset(Stream<DataSet> sources)
    {
        try {
        var options = sources.map(e -> e.getToken()).toList();
        cmbCollection.setItems(FXCollections.observableArrayList(options));
        cmbCollection.setValue(token);    
        } catch (Exception e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
        }
    }
   

   @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        try {
            addDatasetBus = EventBusManager.getEvent(EventTypes.ADD_DATASET.name());
            bus = EventBusManager.getEvent(EventTypes.DELETE_DATASET.name());
            addDatasetBus.register(this);
            bus.register(this);
            token = DataSetService.getCurrentToken();
            ds = DataSetService.getDataSet(token);
            var datasetWasUpdatedAtLeastOnce = ds.getUpdatedAt() != null;
            if(datasetWasUpdatedAtLeastOnce)
            {
                lblLastUpdate.setText(ds.getUpdatedAt().toString());
                lblRecordsSent.setText(String.valueOf(ds.getLastRowCount()));    
            }
            syncMetaData();
            populateDatasetCombo();
            var collName = DataSetService.getDataSet(token).getDataSetName();
            lblCollectionName.setText(collName);
            super.initialize(location, resources);
            lblCollectionName.setWrapText(true);
            lblCollectionName.setTextOverrun(OverrunStyle.CLIP);
            lblCollectionName.setEllipsisString("");
        } catch (Exception e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected void setScreensize() 
    {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

}