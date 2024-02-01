package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import io.sentry.Sentry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class NewConfigurationController extends AbstractController {
        
    @FXML
    Pane content;
    
    @FXML
    Label serverLabel;
    
    @FXML
    Label proxyLabel;
    
    @FXML
    Label tokenLabel;
    
    @FXML
    Label dataLabel;

    @FXML
    ImageView imgBack;

    String basePath = "/br/org/cria/splinkerapp/%s.fxml";

    @Override
    public void initialize(URL location, ResourceBundle bundle)
    {
        try 
        {
            loadPage("central-service");
            serverLabel.setTextFill(Color.RED);
            
            token = DataSetService.getCurrentToken();
            
        } 
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    protected void loadPage(String pageName)
    {
        try 
        {
            
            var template = basePath.formatted(pageName);
            loader = new FXMLLoader(getClass().getResource(template));
            Node childNode = loader.load();
            var children = content.getChildren();
            children.clear();
            children.add(childNode);
        } catch (Exception e)
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }


    void paintItRed(String lblName)
    {
        var lbls = pane.getScene().getRoot().lookupAll(".label");
        lbls.forEach((lbl) ->{ 
            var id =  lbl.getId();
            var paintItBlack = !lblName.equals(id);
            ((Label)lbl).setTextFill(paintItBlack? Color.BLACK : Color.RED);
        });
    }
    @FXML
    void onBackClicked()
    {
         navigateTo(getStage(), "home");
    }

    @FXML
    void showCentralServiceConfiguration()
    {
        loadPage("central-service");
        paintItRed("serverLabel");
    }

    @FXML
    void showProxyConfiguration()
    {
        loadPage("proxy-config");
        paintItRed("proxyLabel");
    }

    @FXML
    void showTokenConfiguration()
    {
        loadPage("token-login");
        paintItRed("tokenLabel");
    }

    @FXML
    void showDataSourceConfiguration()
    {
          try 
          {
            var ds = DataSetService.getDataSet(token);  
            var pageName = "collection-database";

            if(ds.isAccessDb())
            {    
                pageName = "access-db-modal";
            }

            if(ds.isFile())
            {
                pageName = "file-selection";
            }

            loadPage(pageName);
            paintItRed("dataLabel");
          } 
          catch (Exception e) 
          {
            ApplicationLog.error(e.getLocalizedMessage());
            Sentry.captureException(e);
            showErrorModal(e.getLocalizedMessage());
          }
    }

    
}
