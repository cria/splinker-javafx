package br.org.cria.splinkerapp.controllers;

import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public abstract class AbstractController{
    
    protected abstract Pane getPane();

    protected Stage getStage(){
        
        try {
            var pane = getPane();
            var scene = pane.getScene();
            var stage = (Stage)scene.getWindow();
            stage.setResizable(false);
            return stage;    
        } catch (Exception e) {
            System.out.println("ERROR!\n");
            System.out.println(e);
            System.out.println("\n END ERROR!\n");
            e.printStackTrace();
        }
        return new Stage();
    }
}
