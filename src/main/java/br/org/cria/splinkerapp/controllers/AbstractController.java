package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import com.google.common.eventbus.EventBus;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.Router;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class AbstractController implements Initializable {
    protected EventBus bus;
    protected String token;
    protected Stage modalStage = new Stage();
    Alert dialog = new Alert(AlertType.INFORMATION);
    protected abstract Pane getPane();

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        this.setScreensize();
    }

    protected abstract void setScreensize();

    void navigateTo(String routeName)
    {
        try 
        {
            Router.getInstance().navigateTo(getStage(), routeName);
        } catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        
    }
    
    void navigateTo(Stage stage, String routeName)
    {
        try 
        {
            Router.getInstance().navigateTo(stage, routeName);
        } catch (Exception e) 
        {
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
        dialog.onCloseRequestProperty().addListener((listener) -> { 
        try 
        {
            navigateTo("home");     
        } catch (Exception e) {
            ApplicationLog.error(e.getLocalizedMessage());
            showErrorModal(e.getLocalizedMessage());
        }
        });
    }

    protected Stage getStage()
    {
        try 
        {
            var pane = getPane();
            var scene = pane.getScene();
            var stage = (Stage)scene.getWindow();
            stage.setResizable(false);
            return stage;    
        }
        catch (IllegalStateException ex) {
            System.out.println("\nIllegalStateException\n");
            ex.printStackTrace();
            return null;
        } 
        catch (Exception e) 
        {
            final String windowMsg = "Cannot invoke \"javafx.scene.Scene.getWindow()\" because \"scene\" is null";
            final String panelMsg = "Cannot invoke \"javafx.scene.layout.Pane.getScene()\" because \"pane\" is null";
            var errorMsg = e.getMessage();
            var isExpectedError = errorMsg.contains(windowMsg) || errorMsg.contains(panelMsg);
            
            if(!isExpectedError)
            {
                ApplicationLog.error(e.getLocalizedMessage());
            }
            
        }
        return new Stage();
    }

    protected void showTransferModal(String modalText)
    {
        modalStage.initOwner(getStage());
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initStyle(StageStyle.TRANSPARENT);
        Label label = new Label(modalText);
        label.setStyle("-fx-font-size: 24;");
        StackPane modalLayout = new StackPane(label);
        modalLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);"); 
        Scene modalScene = new Scene(modalLayout, 300, 200, Color.TRANSPARENT);
        modalStage.setScene(modalScene);
        modalStage.show();

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
        catch (IllegalStateException ex) {
                return null;
        } 
        catch (Exception e) 
        {
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
        catch (IllegalStateException ex) {
            return;
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
        }
    }
}
