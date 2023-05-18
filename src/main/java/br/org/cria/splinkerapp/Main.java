package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

import br.org.cria.splinkerapp.config.DatabaseSetup;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try {
            DatabaseSetup.initDb();
            stage.setTitle("SpLinker");
            Router.getInstance().navigateTo(stage, "login", 380, 240);
    
            stage.show();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void main(String[] args) {
        launch();
    }
}