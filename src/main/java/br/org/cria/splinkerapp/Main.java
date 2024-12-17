package br.org.cria.splinkerapp;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.config.SentryConfig;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;
import io.sentry.Sentry;
import javafx.application.Platform;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            SentryConfig.setUp();
            LockFileManager.verifyLockFile();
            Task<Void> initDb = DatabaseSetup.initDb();
            if (initDb != null) {
                stage.setOnCloseRequest(event -> {
                    try {
                        LockFileManager.deleteLockfile();
                        LogManager.shutdown();
                    } catch (Exception e) {
                        Sentry.captureException(e);
                        throw new RuntimeException(e);
                    }
                });
                initDb.setOnFailed(event -> {
                    try {
                        LockFileManager.deleteLockfile();
                        var exception = initDb.getException();
                        Sentry.captureException(exception);
                        throw new RuntimeException(exception);
                    } catch (Exception e) {
                        Sentry.captureException(e);
                    }
                });

                initDb.setOnSucceeded(event -> {
                    Platform.runLater(() -> {
                        stage.setTitle("spLinker");
                        stage.setResizable(false);

                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                        } else {
                            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                        }

                        try {
                            if (SpLinkerUpdateService.hasNewVersion()) {
                                Router.navigateTo(stage, "splinker-update");
                            }

                            var hasConfig = DataSetService.hasConfiguration();
                            var routeName = hasConfig ? "home" : "first-config-dialog";
                            Router.navigateTo(stage, routeName);
                        } catch (Exception e) {
                            Sentry.captureException(e);
                            throw new RuntimeException(e);
                        }
                    });
                });
                initDb.run();
                initDb.get();
            }
        } catch (Exception ex) {
            Sentry.captureException(ex);
            LockFileManager.deleteLockfile();
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}