package br.org.cria.splinkerapp;

import java.util.Objects;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.config.SentryConfig;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;
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
                    // No método setOnSucceeded do initDb na classe Main
                    // No método setOnSucceeded do initDb na classe Main
                    Platform.runLater(() -> {
                        try {
                            stage.setResizable(false);

                            // Verificar o sistema operacional e definir o ícone correto
                            String os = System.getProperty("os.name").toLowerCase();
                            if (os.contains("win")) {
                                stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                                stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png"))));
                            }

                            // IMPORTANTE: Modificação principal - usar um bloco if-else em vez de continuar a execução
                            if (SpLinkerUpdateService.hasNewVersion()) {
                                // Se houver uma nova versão, mostre a tela de atualização e PARE aqui
                                Router.navigateTo(stage, "splinker-update");
                            } else {
                                // Se NÃO houver nova versão, só então mostrar a tela principal
                                var hasConfig = DataSetService.hasConfiguration();
                                var routeName = hasConfig ? "home" : "first-config-dialog";
                                Router.navigateTo(stage, routeName);
                            }
                        } catch (Exception e) {
                            Sentry.captureException(e);

                            // Em caso de erro na verificação de versão, continuar para a tela principal
                            try {
                                var hasConfig = DataSetService.hasConfiguration();
                                var routeName = hasConfig ? "home" : "first-config-dialog";
                                Router.navigateTo(stage, routeName);
                            } catch (Exception ex) {
                                Sentry.captureException(ex);
                                throw new RuntimeException(ex);
                            }
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