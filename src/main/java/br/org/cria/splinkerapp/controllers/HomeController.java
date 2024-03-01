package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import br.org.cria.splinkerapp.enums.WindowSizes;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class HomeController extends AbstractController {

    
    @FXML
    MenuBar menuBar;
    @FXML
    Pane content;
    @FXML
    ImageView principalImg = new ImageView();
    @FXML
    ImageView settingsImg = new ImageView();
    @FXML
    ImageView helpImg = new ImageView();

    String basePath = "/br/org/cria/splinkerapp/%s.fxml";

    
    @FXML
    void onConfigurationItemClick()
    {
        loadPage("configuration");
    }

    @FXML
    void onPrincipalItemClick()
    {
        loadPage("principal");    
    }

    @FXML
    void onHelpItemClick()
    {
        //  loadPage("help");
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
            handleErrors(e);
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        
        try 
        {
            loadPage("principal");
            super.initialize(location, resources);
            Platform.runLater(() ->
            {
                // TODO: Tooltips
                // Tooltip.install(principalImg, new Tooltip("Principal")); // Doesn't work
                // // Tooltip.install(principalImg.getParent(), new Tooltip("Tooltip")); // Works

                // //Tooltip.install(settingsImg, new Tooltip("Configurações")); // Doesn't work
                // Tooltip.install(settingsImg.getParent(), new Tooltip("Tooltip")); // Works
                // Tooltip.install(helpImg, new Tooltip("Ajuda")); // Doesn't work
                // // Tooltip.install(helpImg.getParent(), new Tooltip("Tooltip")); // Works

            });
        } catch (Exception e) {
            handleErrors(e);
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
