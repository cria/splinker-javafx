package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import com.google.common.eventbus.EventBus;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.Router;
import io.sentry.Sentry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class AbstractController implements Initializable {
    
    @FXML
    Pane pane;
    
    protected EventBus bus;
    protected String token;
    protected FXMLLoader loader;
    protected Stage modalStage = new Stage();
    Alert dialog = new Alert(AlertType.INFORMATION);
    

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {

        //throw new NotImplementedException("Not implemented");
    }

    protected abstract void setScreensize();

    void navigateTo(String routeName)
    {
        try 
        {
            Router.navigateTo(getStage(), routeName);
        } catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    
    void navigateTo(Stage stage, String routeName)
    {
        try 
        {
            Router.navigateTo(stage, routeName);
        } catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
    }

    protected void showAlert(AlertType type, String title, String message)
    {
        if(type != null)
        {
            dialog = new Alert(type);
        }
        
        dialog.setTitle(title);
        dialog.setContentText(message);
        dialog.show();
    }

    protected Stage getStage()
    {
        try 
        {
            var scene = pane.getScene();
            var stage = (Stage)scene.getWindow();
            stage.setResizable(false);
            return stage;    
        }
        catch (Exception ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
            return null;
        }
    }

    protected void showErrorModal(String errorMessage) 
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    protected void navigateOrOpenNewWindowOnExistingDataSource(String routeName, boolean newWindow) throws Exception
    {
        if(newWindow)
        {
            openNewWindow(routeName);
        }
        else
        {
            navigateTo(routeName);
        }
    }

    protected Stage createNewWindow(String routeName)
    {
        var stage = new Stage();
        try 
        {
            var route = "/br/org/cria/splinkerapp/%s.fxml".formatted(routeName);
            var resource = getClass().getResource(route);
            var fxmlLoader = new FXMLLoader(resource);
            var parent = (Parent) fxmlLoader.load();
            var scene  = new Scene(parent);
            stage.setScene(scene);
        }
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
        }
        return stage;
    }

    protected void openNewWindow(String routeName)
    {

        try 
        {
            var stage = createNewWindow(routeName);
            stage.show();    
        }
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
        }
    }

}
