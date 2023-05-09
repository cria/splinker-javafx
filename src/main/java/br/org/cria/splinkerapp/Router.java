package br.org.cria.splinkerapp;

import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

 final class Router {
    private static Router instance;

    private Router(){};

    static Router getInstance(){
        if(instance == null)
        {
            instance = new Router();
        }
        return instance;
    }

    Scene loadScene(String routeName) {
        Scene scene = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(routeName));
            scene = new Scene(fxmlLoader.load(), 320, 240);
        } catch (Exception e) {
            System.exit(1);
        }

        return scene;
    }

    Map<String, Scene> routes = Map.of("login", loadScene("login.fxml"),
            "proxy-config", loadScene("proxy-config.fxml"));
}
