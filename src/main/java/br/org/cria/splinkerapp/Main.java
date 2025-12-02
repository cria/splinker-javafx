package br.org.cria.splinkerapp;

import java.util.Objects;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.config.SentryConfig;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;
import br.org.cria.splinkerapp.services.implementations.VersionService;
import io.sentry.Sentry;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            SentryConfig.setUp();
            Task<Void> initDb = DatabaseSetup.initDb();
            stage.setOnCloseRequest(event -> {
                try {
                    LogManager.shutdown();
                } catch (Exception e) {
                    Sentry.captureException(e);
                    throw new RuntimeException(e);
                }
            });
            initDb.setOnFailed(event -> {
                try {
                    var exception = initDb.getException();
                    Sentry.captureException(exception);
                    throw new RuntimeException(exception);
                } catch (Exception e) {
                    Sentry.captureException(e);
                }
            });

            initDb.setOnSucceeded(event -> Platform.runLater(() -> {
                try {
                    stage.setResizable(false);

                    stage.setTitle("v" + VersionService.getVersion());

                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                    } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                    }

                    if (!os.contains("mac") && SpLinkerUpdateService.hasNewVersion()) {
                        Router.navigateTo(stage, "splinker-update");
                    } else {
                        var hasConfig = DataSetService.hasConfiguration();
                        var routeName = hasConfig ? "home" : "first-config-dialog";
                        Router.navigateTo(stage, routeName);
                    }
                } catch (Exception e) {
                    Sentry.captureException(e);

                    try {
                        var hasConfig = DataSetService.hasConfiguration();
                        var routeName = hasConfig ? "home" : "first-config-dialog";
                        Router.navigateTo(stage, routeName);
                    } catch (Exception ex) {
                        Sentry.captureException(ex);
                        throw new RuntimeException(ex);
                    }
                }
            }));
            initDb.run();
            initDb.get();
        } catch (Exception ex) {
            Sentry.captureException(ex);
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}