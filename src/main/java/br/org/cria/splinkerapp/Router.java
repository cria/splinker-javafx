package br.org.cria.splinkerapp;

import java.io.IOException;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

 public final class Router {
    private static Router instance;

    private Router(){};

    public static Router getInstance(){
        if(instance == null)
        {
            instance = new Router();
        } 
        return instance;
    }

    public void navigateTo(Stage stage, String routeName) {
        navigateTo(stage, routeName, 0, 0);
    }

    public void navigateTo(Stage stage, String routeName, int width, int height) {
        try 
        {
            width = width < 1 ? 320 : width;
            height = height < 1 ? 240 : height;
            var scene  = loadScene(routeName, width, height);
            stage.setScene(scene);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.show();
        } catch (Exception e) {
            System.out.println("ERROR\n");
            System.out.println(e.toString());
            System.out.println("\n END ERROR\n");
        }
        

    }

    private Scene loadScene(String routeName, int width, int height) throws IOException {
        width = width < 1 ? 320 : width;
        height = height < 1 ? 240 : height;
        String route = "%s.fxml".formatted(routeName);
        FXMLLoader fxmlLoader = new FXMLLoader(Router.class.getResource(route));
        return new Scene(fxmlLoader.load(), width, height);

        
    }

//  Map<String, Scene> routes = Map.of("login", loadScene("login.fxml"),
//                                     "proxy-config", loadScene("proxy-config.fxml"), 
//                                     "token-login", loadScene("token-login.fxml"));
}
