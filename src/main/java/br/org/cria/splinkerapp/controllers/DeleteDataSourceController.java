package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class DeleteDataSourceController extends AbstractController {
    @FXML
    Pane pane;
    
    @FXML
    Button btnYes;
    
    @FXML
    Button btnNo;

    @FXML
    Label lblMsg;

    @FXML
    void onButtonYesClicked()
    {
        try {
            DataSetService.deleteDataSet(token);    
            if(DataSetService.getAllDataSets().isEmpty())
            {
                System.exit(0);
            }
            getStage().close();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    
    @FXML
    void onButtonNoClicked()
    {
        getStage().close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        try {
            token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            var msg = lblMsg.getText() + "\n %s (%s)".formatted(ds.getDataSetAcronym(), ds.getDataSetName()) + "?";
            lblMsg.setText(msg);
        } catch (Exception e) {
            
            logger.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected Pane getPane() 
    {
        return this.pane;
    }
    
}
