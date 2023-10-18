package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

    Label lblMessage;

    @FXML
    void onSyncServerBtnClicked() throws Exception 
    {
        try {
            var ds = DataSourceRepository.getDataSource();
            if (ds != null) 
            {
                transferService = new DarwinCoreArchiveService()
                        .readDataFromSource(ds)
                        .generateTXTFile()
                        .generateZIPFile()
                        .transferData();

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
            System.out.println("\n\n\n" + ex.toString() + "\n\n\n\n");
            ex.printStackTrace();
            showErrorModal(ex.toString());

        }
    }

    void onCancelTransferButtonClicked() {
        if (transferService != null) {
            transferService.cancel();
        }
    }

    @FXML
    void onSyncMetadataBtnClicked() {
        try 
        {
            var token = TokenRepository.getToken();
            var config = TokenRepository.getConfigurationDataFromAPI(token);
            ConfigFacade.handleConfiguration(config);
        } 
        catch (Exception e) 
        {
            showErrorModal(e.getMessage());
        }
    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }

}
