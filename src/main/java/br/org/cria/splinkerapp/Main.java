package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

import br.org.cria.splinkerapp.config.DatabaseSetup;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try {
            var routeName = "login";
            var width = 380;
            var height = 240;
            DatabaseSetup.initDb();
            stage.setTitle("spLinker");
            stage.setResizable(false);
            Router.getInstance().navigateTo(stage,routeName, width, height);
    
            stage.show();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}