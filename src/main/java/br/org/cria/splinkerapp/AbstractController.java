package br.org.cria.splinkerapp;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public abstract class AbstractController {
    
    @FXML
    Pane pane;

    protected AbstractController(){
        if(this.pane == null){
            this.pane = new Pane();
        }
    }
    protected Stage getStage(){
        
        try {
            return (Stage) this.pane.getScene().getWindow();    
        } catch (Exception e) {
            System.out.println("ERROR!\n");
            System.out.println(e.toString());
            System.out.println("\n END ERROR!\n");
        }
        return new Stage();
    }
    
}
