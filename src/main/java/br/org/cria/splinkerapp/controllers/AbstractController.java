package br.org.cria.splinkerapp.controllers;

import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public abstract class AbstractController{//  implements Initializable{
    
    protected abstract Pane getPane();

    // @Override
    // public void initialize(URL location, ResourceBundle resources) {
    //     // TODO Auto-generated method stub
        
    // }

    protected Stage getStage(){
        
        try {
            var scene = getPane().getScene();
            var stage = (Stage)scene.getWindow();
            stage.setResizable(false);
            return stage;    
        } catch (Exception e) {
            System.out.println("ERROR!\n");
            System.out.println(e.toString());
            System.out.println("\n END ERROR!\n");
        }
        return new Stage();
    }
}
