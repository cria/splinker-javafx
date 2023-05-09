package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("SpLinker");
        stage.setScene(Router.getInstance().routes.get("login"));
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
             
            }
            
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}