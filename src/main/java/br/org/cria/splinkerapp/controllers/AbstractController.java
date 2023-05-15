package br.org.cria.splinkerapp.controllers;

import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public abstract class AbstractController {
    
    protected abstract Pane getPane();
    protected Stage getStage(){
        
        try {
            var scene = getPane().getScene();
            return (Stage)scene.getWindow();    
        } catch (Exception e) {
            System.out.println("ERROR!\n");
            System.out.println(e.toString());
            System.out.println("\n END ERROR!\n");
        }
        return new Stage();
    }
    
}
