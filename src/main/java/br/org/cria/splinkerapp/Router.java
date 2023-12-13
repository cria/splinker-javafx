package br.org.cria.splinkerapp;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

 public final class Router {
    private static Router instance;

    private Router(){}

     public static Router getInstance()
    {
        if(instance == null)
        {
            instance = new Router();
        } 
        return instance;
    }

    public void navigateTo(Stage stage, String routeName) throws Exception
    {
            var scene  = loadScene(routeName);
            stage.setScene(scene);
            stage.show();
        
    }

    private Scene loadScene(String routeName) throws IOException 
    {
        var route = "%s.fxml".formatted(routeName);
        var resource = getClass().getResource(route);
        var fxmlLoader = new FXMLLoader(resource);
        var parent = (Parent)fxmlLoader.load();
        var scene = new Scene(parent);
        
        return scene;
    }
}
