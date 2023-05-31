package br.org.cria.splinkerapp;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

 public final class Router {
    private static Router instance;

    private Router(){};

    public static Router getInstance()
    {
        if(instance == null)
        {
            instance = new Router();
        } 
        return instance;
    }

    public void navigateTo(Stage stage, String routeName) 
    {
        navigateTo(stage, routeName, 0, 0);
    }

    public void navigateTo(Stage stage, String routeName, int width, int height) 
    {
        try 
        {
            width = width < 1 ? 320 : width;
            height = height < 1 ? 240 : height;
            var scene  = loadScene(routeName, width, height);
            stage.setScene(scene);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.show();
        } 
        catch (Exception e) 
        {
            System.out.println("ERROR\n");
            System.out.println(e.toString());
            System.out.println("\n END ERROR\n");
            e.printStackTrace();
        }
    }

    private Scene loadScene(String routeName, int width, int height) throws IOException 
    {
        var route = "%s.fxml".formatted(routeName);
        var resource = getClass().getResource(route);
        var fxmlLoader = new FXMLLoader(resource);
        var parent = (Parent)fxmlLoader.load();
        width = width < 1 ? 320 : width;
        height = height < 1 ? 240 : height;

        return new Scene(parent, width, height);
    }

}
