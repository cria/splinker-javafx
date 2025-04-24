package br.org.cria.splinkerapp;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.config.LockFileManager;
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
    private TrayIcon trayIcon;
    private SystemTray tray;
    private Stage primaryStage;
    private Image appIcon;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        try {
            SentryConfig.setUp();
            LockFileManager.verifyLockFile();
            Task<Void> initDb = DatabaseSetup.initDb();
            if (initDb != null) {
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    minimizarParaTray();
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
                        try {
                            stage.setResizable(false);
                            stage.setTitle("v" + VersionService.getVersion());

                            String os = System.getProperty("os.name").toLowerCase();
                            if (os.contains("win")) {
                                appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png")));
                                stage.getIcons().add(appIcon);
                            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                                appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cria-logo.png")));
                                stage.getIcons().add(appIcon);
                            }

                            inicializarSystemTray();

                            if (SpLinkerUpdateService.hasNewVersion()) {
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

    private void inicializarSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray não é suportado");
            return;
        }

        try {
            tray = SystemTray.getSystemTray();
            java.awt.Image awtImage = getAwtImageFromFxImage(appIcon);
            if (awtImage == null) {

                awtImage = ImageIO.read(getClass().getResource("/images/cria-logo.png"));
            }

            PopupMenu popup = new PopupMenu();

            MenuItem mostrarItem = new MenuItem("Mostrar aplicação");
            mostrarItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.setIconified(false);
                primaryStage.toFront();
            }));
            popup.add(mostrarItem);

            CheckboxMenuItem startupItem = new CheckboxMenuItem("Iniciar com o Windows");
            startupItem.setState(isAutoStartEnabled());
            startupItem.addItemListener(e -> {
                configurarInicializacaoAutomatica(startupItem.getState(), "SpLinker");
            });
            popup.add(startupItem);
            popup.addSeparator();

            MenuItem sairItem = new MenuItem("Sair");
            sairItem.addActionListener(e -> {
                fecharAplicacao();
            });
            popup.add(sairItem);

            trayIcon = new TrayIcon(awtImage, "SpLinker v" + VersionService.getVersion(), popup);
            trayIcon.setImageAutoSize(true);

            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.setIconified(false);
                primaryStage.toFront();
            }));

            tray.add(trayIcon);

        } catch (AWTException | IOException e) {
            Sentry.captureException(e);
            System.err.println("Erro ao inicializar o system tray: " + e.getMessage());
        }
    }

    private void minimizarParaTray() {
        Platform.runLater(() -> primaryStage.hide());

        if (trayIcon != null && SystemTray.isSupported()) {
            trayIcon.displayMessage(
                    "SpLinker em execução",
                    "A aplicação continua em execução na bandeja do sistema.",
                    TrayIcon.MessageType.INFO
            );
        }
    }

    private void fecharAplicacao() {
        try {
            if (tray != null && trayIcon != null) {
                tray.remove(trayIcon);
            }

            LockFileManager.deleteLockfile();
            LogManager.shutdown();

            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            Sentry.captureException(e);
            throw new RuntimeException(e);
        }
    }

    private java.awt.Image getAwtImageFromFxImage(Image fxImage) {
        try {
            return ImageIO.read(getClass().getResource("/images/cria-logo.png"));
        } catch (Exception e) {
            Sentry.captureException(e);
            return null;
        }
    }

    // Método para configurar inicialização automática com o Windows
    private void configurarInicializacaoAutomatica(boolean ativar, String nomeAplicacao) {
        try {
            String caminhoJar = new File(Main.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();

            String comando;
            if (ativar) {
                comando = "reg add HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v \""
                        + nomeAplicacao + "\" /t REG_SZ /d \"javaw -jar \\\"" + caminhoJar
                        + "\\\"\" /f";
            } else {
                comando = "reg delete HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v \""
                        + nomeAplicacao + "\" /f";
            }

            Process process = Runtime.getRuntime().exec(comando);
            int resultado = process.waitFor();

            if (resultado == 0) {
                trayIcon.displayMessage(
                        "Configuração salva",
                        ativar ? "SpLinker será iniciado automaticamente com o Windows" :
                                "SpLinker não será mais iniciado automaticamente",
                        TrayIcon.MessageType.INFO
                );
            } else {
                trayIcon.displayMessage(
                        "Erro",
                        "Falha ao configurar inicialização automática",
                        TrayIcon.MessageType.ERROR
                );
            }

        } catch (Exception e) {
            Sentry.captureException(e);
            System.err.println("Erro ao configurar registro: " + e.getMessage());
        }
    }

    private boolean isAutoStartEnabled() {
        try {
            Process process = Runtime.getRuntime().exec(
                    "reg query HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v SpLinker");

            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}