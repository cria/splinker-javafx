package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Hello!");
        stage.setScene(Router.routes.get("login"));
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                Stage _stage = new Stage();
                _stage.setScene(Router.routes.get("proxy-config"));
                _stage.show();
            }
            
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}