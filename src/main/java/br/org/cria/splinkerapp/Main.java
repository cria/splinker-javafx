package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;

public class Main extends Application 
{
    @Override
    public void start(Stage stage) throws Exception
    {
        try 
        {
            var initDb = DatabaseSetup.initDb();
            if (initDb != null) 
            {
                stage.setOnCloseRequest(event ->{
                    LogManager.shutdown();
                });
                initDb.setOnFailed(event -> {
                    var exception = initDb.getException();
                });

                initDb.setOnSucceeded((event )-> {
                        stage.setTitle("spLinker");
                        stage.setResizable(false); 
                        try {
                            if(DataSetService.hasConfiguration())
                        {
                            Router.getInstance().navigateTo(stage, "home");
                        }
                        else
                        {
                            Router.getInstance().navigateTo(stage, "first-config-dialog");
                        }
                        } catch (Exception e) {
                            ApplicationLog.error(e.getLocalizedMessage());
                             throw new RuntimeException(e);
                        }
                        stage.show();
            
                });
                initDb.start();
            }
          
        } 
        catch (Exception ex) 
        {
            ApplicationLog.error(ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}