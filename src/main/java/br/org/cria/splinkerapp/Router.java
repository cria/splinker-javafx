package br.org.cria.splinkerapp;

import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public abstract class Router {

    static Scene loadScene(String routeName) {
        Scene scene = new Scene(null);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(routeName));
            scene = new Scene(fxmlLoader.load(), 320, 240);
        } catch (Exception e) {
            System.exit(1);
        }

        return scene;
    }

    static Map<String, Scene> routes = Map.of("login", loadScene("login.fxml"),
            "proxy-config", loadScene("proxy-config.fxml"));
}
