package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.DatabaseSourceManager;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    void onSyncServerBtnClicked() throws Exception 
    {
        try {
            var ds = DataSourceRepository.getDataSource();
            if (ds != null) 
            {
                if(ds.isFile())
                {
                    transferService = FileSourceManager.processData(ds.getDataSourceFilePath());
                }
                if(ds.isAccessDb())
                {
                    transferService = DatabaseSourceManager.processData(ds.getDataSourceFilePath(), 
                                                                ds.getDbUser(),ds.getDbPassword());
                }
                if(ds.isSQLDatabase())
                {
                    transferService = DatabaseSourceManager.processData(ds.getType(), ds.getDbHost(), 
                    ds.getDbName(), ds.getDbTableName(), ds.getDbUser(), ds.getDbPassword(), ds.getDbPort());
                }

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
                    });
                    transferService.start();
                    showTransferModal("Transferindo");
                }
            }

        } catch (IllegalStateException ex) {
            return;
        }
        catch (Exception ex) {
            showErrorModal(ex.toString());
        }
    }

    void onCancelTransferButtonClicked() {
        if (transferService != null) {
            transferService.cancel();
        }
    }

    @FXML
    void onDbConfigMenuItemClick() 
    {
        var width = 364;
        var height = 370;
        openNewWindow("collection-database", width, height);
    }
    @FXML
    void onFilePathConfigMenuOptionClick() 
    {
        try
        {
            var ds = DataSourceRepository.getDataSource();
            var isAccessFile = ds.getType() == DataSourceType.Access;
            var width = 360;
            var height = 200;
            var routeName = "file-selection";
            if(isAccessFile)
            {
                routeName = "access-db-modal";
                width = 424;
                height = 200;
            }
            openNewWindow(routeName, width, height);
        }
        catch(Exception ex)
        {

        }
    }

    @FXML
    void onProxyConfigMenuOptionClick() 
    {
        var width = 330;
        var height = 300;
        openNewWindow("proxy-config", width, height);
    }
    @FXML
    void onCentralServiceConfigMenuOptionClick() 
    {
        openNewWindow("central-service", 360, 170);
    }
    @FXML
    void onDeleteLocalConfigMenuItemClick() 
    {
        try 
        {
            DatabaseSetup.deleteLocalDatabase();
            System.exit(0);
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    
    
    @FXML
    void onSyncMetaDataMenuItemClick()
    {
        try 
        {
            var token = TokenRepository.getToken();
            var config = TokenRepository.getConfigurationDataFromAPI(token);
            ConfigFacade.handleConfiguration(config);
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getLocalizedMessage());
        }  
    }
    @Override
    protected Pane getPane() {
        return this.pane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        try 
        {
            if(TokenRepository.getToken() == null)
            {
                navigateTo(getStage(), "first-config-dialog", 330,150);
            }    
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getLocalizedMessage());
        }
        
    }

}
