package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;
import io.sentry.Sentry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class InstallConfirmationController extends AbstractController {

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblMessage;

    @FXML
    private Button btnYes;

    @FXML
    private Button btnNo;

    private String installerPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        String os = System.getProperty("os.name").toLowerCase();

        String fileExtension;
        if (os.contains("windows")) {
            fileExtension = "msi";
        } else if (os.contains("linux")) {
            fileExtension = SpLinkerUpdateService.getInstallerExtension();
        } else {
            fileExtension = "dmg";
        }

        File homeDir = new File(System.getProperty("user.home"));
        File downloadDir = new File(homeDir, "Downloads");
        File installerFile = new File(downloadDir, "splinker_new_version." + fileExtension);

        installerPath = installerFile.getAbsolutePath();
    }

    public void setInstallerPath(String path) {
        this.installerPath = path;
    }

    @FXML
    void onBtnYesClicked() {
        try {
            if (btnYes != null) btnYes.setDisable(true);
            if (btnNo != null) btnNo.setDisable(true);

            if (lblMessage != null) {
                lblMessage.setText("Iniciando processo de atualização...");
            }

            LockFileManager.deleteLockfile();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("linux")) {
                if (lblMessage != null) {
                    lblMessage.setText("Iniciando o processo de atualização para Linux...");
                }
                SpLinkerUpdateService.runSoftwareUpdate();
                return;
            }

            File installerFile = new File(installerPath);
            if (!installerFile.exists()) {
                if (lblMessage != null) {
                    lblMessage.setText("Instalador não encontrado. Iniciando download...");
                }
                SpLinkerUpdateService.runSoftwareUpdate();
                return;
            }

            if (lblMessage != null) {
                lblMessage.setText("Iniciando instalação. O aplicativo será fechado...");
            }

            ProcessBuilder processBuilder = new ProcessBuilder();

            if (os.contains("windows")) {
                processBuilder.command("cmd.exe", "/c", "start", "", installerPath);
            } else {
                processBuilder.command("open", installerPath);
            }

            processBuilder.start();

            Thread.sleep(1500);

            Platform.runLater(() -> System.exit(0));

        } catch (Exception e) {
            Sentry.captureException(e);

            if (btnYes != null) btnYes.setDisable(false);
            if (btnNo != null) btnNo.setDisable(false);

            if (lblMessage != null) {
                lblMessage.setText("Erro ao iniciar a instalação: " + e.getMessage());
            }
        }
    }

    @FXML
    void onBtnNoClicked() {
        try {
            navigateToHome();
        } catch (Exception e) {
            Sentry.captureException(e);
            getStage().close();
        }
    }

    private void navigateToHome() {
        try {
            var hasConfig = DataSetService.hasConfiguration();
            var routeName = hasConfig ? "home" : "first-config-dialog";
            Router.navigateTo(getStage(), routeName);
        } catch (Exception e) {
            Sentry.captureException(e);
            getStage().close();
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}