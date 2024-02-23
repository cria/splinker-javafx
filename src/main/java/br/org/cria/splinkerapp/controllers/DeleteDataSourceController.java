package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
public class DeleteDataSourceController extends AbstractController {
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
                LockFileManager.deleteLockfile();
                System.exit(0);
            }
            bus.post(token);;
            getStage().close();
        } catch (Exception e) {
            handleErrors(e);
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
            super.initialize(location, resources);
            bus = EventBusManager.getEvent(EventTypes.DELETE_DATASET.name());
            token = DataSetService.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            var msg = lblMsg.getText() + "\n %s (%s)".formatted(ds.getDataSetAcronym(), ds.getDataSetName()) + "?";
            lblMsg.setText(msg);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
