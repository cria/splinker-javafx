package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.stage.Stage;
import br.org.cria.splinkerapp.config.BaseConfiguration;
import br.org.cria.splinkerapp.config.DatabaseSetup;

public class Main extends Application 
{
    @Override
    public void start(Stage stage) 
    {
        try 
        {
            DatabaseSetup.initDb();
            var routeName = "first-config-dialog";
            var width = 330;
            var height = 150;
            
            if(BaseConfiguration.hasConfiguration())
            {
                routeName ="home";
                width = 350;
                height = 200;
            
            }
            
            stage.setTitle("spLinker");
            stage.setResizable(false);
            Router.getInstance().navigateTo(stage, routeName, width, height);
    
            stage.show();
        } 
        catch (Exception ex) 
        {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) 
    {
        launch();
    }
}