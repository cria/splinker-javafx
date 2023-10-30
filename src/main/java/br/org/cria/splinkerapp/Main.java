package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.stage.Stage;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.repositories.TokenRepository;

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
                initDb.setOnFailed(event -> {
                    var exception = initDb.getException();
                });

                initDb.setOnSucceeded((event )-> {
                        stage.setTitle("spLinker");
                        stage.setResizable(false); 
                        try {
                            if(TokenRepository.hasConfiguration())
                        {
                            Router.getInstance().navigateTo(stage, "home", 231 ,222);
                        }
                        else
                        {
                            Router.getInstance().navigateTo(stage, "first-config-dialog", 330,150);
                        }
                        } catch (Exception e) {
                             throw new RuntimeException(e);
                        }
                        stage.show();
            
                });
                initDb.start();
            }
          
        } 
        catch (Exception ex) 
        {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}