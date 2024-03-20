package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.config.SentryConfig;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import io.sentry.Sentry;
import javafx.application.Platform;

public class Main extends Application 
{
    @Override
    public void start(Stage stage) throws Exception
    {
        try 
        {
            SentryConfig.setUp();
            LockFileManager.verifyLockFile();
            Task<Void> initDb = DatabaseSetup.initDb();
            if (initDb != null) 
            {
                stage.setOnCloseRequest(event ->{
                    LockFileManager.deleteLockfile();
                    LogManager.shutdown();
                });
                initDb.setOnFailed(event -> {
                    LockFileManager.deleteLockfile();
                    var exception = initDb.getException();
                    Sentry.captureException(exception);
                    ApplicationLog.error(exception.getLocalizedMessage());
                });

                initDb.setOnSucceeded(event -> {
                    Platform.runLater(() ->
                    {
                        stage.setTitle("spLinker");
                        stage.setResizable(false); 
                        try 
                        {
                            var hasConfig = DataSetService.hasConfiguration();
                            var routeName = hasConfig ? "home" :  "first-config-dialog";
                            Router.navigateTo(stage, routeName);
                        } 
                        catch (Exception e) 
                        {
                            Sentry.captureException(e);
                            ApplicationLog.error(e.getLocalizedMessage());
                            throw new RuntimeException(e);
                        }
                    });
                        
                });
                initDb.run();
                initDb.get();
            }
        } 
        catch (Exception ex) 
        {
            Sentry.captureException(ex);
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