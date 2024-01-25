package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.services.implementations.DataSetService;

public class Main extends Application 
{
    @Override
    public void start(Stage stage) throws Exception
    {
        try 
        {
            LockFileManager.verifyLockFile();
            Task<Void> initDb = DatabaseSetup.initDb();
            if (initDb != null) 
            {
                stage.setOnCloseRequest(event ->{
                    LockFileManager.deleteLockfile();
                    LogManager.shutdown();
                });
                initDb.setOnFailed(event -> {
                    var exception = initDb.getException();
                    ApplicationLog.error(exception.getLocalizedMessage());
                });

                initDb.setOnSucceeded(event -> {
                        stage.setTitle("spLinker");
                        stage.setResizable(false); 
                        try {
                            if(DataSetService.hasConfiguration())
                        {
                            Router.getInstance().navigateTo(stage, "new-home");
                        }
                        else
                        {
                            Router.getInstance().navigateTo(stage, "first-config-dialog");
                        }
                        } catch (Exception e) {
                            ApplicationLog.error(e.getLocalizedMessage());
                             throw new RuntimeException(e);
                        }
            
                });
                initDb.run();
            }
          
        } 
        catch (Exception ex) 
        {
            LockFileManager.deleteLockfile();
            ApplicationLog.error(ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}