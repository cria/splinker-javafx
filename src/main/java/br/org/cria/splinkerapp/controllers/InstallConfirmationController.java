package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
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

    // Caminho para o arquivo MSI baixado
    private String installerPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // Por padrão, assumimos que o arquivo está no local padrão
        File homeDir = new File(System.getProperty("user.home"));
        File downloadDir = new File(homeDir, "Downloads");
        File installerFile = new File(downloadDir, "splinker_new_version.msi");

        installerPath = installerFile.getAbsolutePath();
    }

    /**
     * Define o caminho para o instalador.
     * Este método deve ser chamado pelo controller anterior ao navegar para esta tela.
     *
     * @param path Caminho completo para o arquivo MSI
     */
    public void setInstallerPath(String path) {
        this.installerPath = path;
    }

    @FXML
    void onBtnYesClicked() {
        try {
            // Desabilitar os botões para evitar cliques múltiplos
            if (btnYes != null) btnYes.setDisable(true);
            if (btnNo != null) btnNo.setDisable(true);

            // Atualizar a mensagem
            if (lblMessage != null) {
                lblMessage.setText("Iniciando instalação. O aplicativo será fechado...");
            }

            // Verificar se o arquivo existe
            File installerFile = new File(installerPath);
            if (!installerFile.exists()) {
                if (lblMessage != null) {
                    lblMessage.setText("Erro: Arquivo de instalação não encontrado.");
                }
                if (btnYes != null) btnYes.setDisable(false);
                if (btnNo != null) btnNo.setDisable(false);
                return;
            }

            // Iniciar o instalador
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", "start", "", installerPath);
            processBuilder.start();

            // Aguardar um pouco e depois fechar o aplicativo
            Thread.sleep(1500);

            // Fechar o aplicativo
            Platform.runLater(() -> System.exit(0));

        } catch (Exception e) {
            Sentry.captureException(e);

            // Em caso de erro, re-habilitar os botões
            if (btnYes != null) btnYes.setDisable(false);
            if (btnNo != null) btnNo.setDisable(false);

            // Mostrar mensagem de erro
            if (lblMessage != null) {
                lblMessage.setText("Erro ao iniciar a instalação: " + e.getMessage());
            }
        }
    }

    @FXML
    void onBtnNoClicked() {
        try {
            // Navegar de volta para a tela principal
            navigateToHome();
        } catch (Exception e) {
            Sentry.captureException(e);
            getStage().close();
        }
    }

    private void navigateToHome() {
        try {
            // Determinar qual tela mostrar baseado na configuração
            var hasConfig = DataSetService.hasConfiguration();
            var routeName = hasConfig ? "home" : "first-config-dialog";

            // Navegar para a tela apropriada
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