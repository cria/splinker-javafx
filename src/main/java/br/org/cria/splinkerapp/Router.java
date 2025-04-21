package br.org.cria.splinkerapp;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Router {

    // Armazenar o último controller carregado
    private static Object currentController;

    /**
     * Navega para a rota especificada
     * @param stage Stage a ser atualizado
     * @param routeName Nome da rota
     * @throws Exception Se ocorrer erro na navegação
     */
    public static void navigateTo(Stage stage, String routeName) throws Exception {
        var scene = loadScene(routeName);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Retorna o controller da tela atual
     * @return O controller da tela atual ou null se nenhum foi carregado
     */
    public static Object getCurrentController() {
        return currentController;
    }

    private static Scene loadScene(String routeName) throws IOException {
        var route = "%s.fxml".formatted(routeName);
        var resource = Router.class.getResource(route);
        var fxmlLoader = new FXMLLoader(resource);
        var parent = (Parent) fxmlLoader.load();

        // Armazenar o controller
        currentController = fxmlLoader.getController();

        var scene = new Scene(parent);
        scene.getStylesheets().add(Router.class.getResource("styles.css").toExternalForm());

        return scene;
    }
}