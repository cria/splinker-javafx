package br.org.cria.splinkerapp.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.InstallerService;
import br.org.cria.splinkerapp.services.implementations.SpLinkerUpdateService;
import io.sentry.Sentry;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

public class SpLinkerUpdateController extends AbstractController {

    @FXML
    private Button btnYes;

    @FXML
    private Button btnNo;

    @FXML
    private Label lblMessage;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ProgressIndicator progressIndicator;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // Obtenha a URL de download do serviço
    private String downloadUrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // Obter a URL de download do serviço
        downloadUrl = SpLinkerUpdateService.getLatestDownloadUrl();

        // Configurar a visibilidade inicial
        if (progressBar != null) progressBar.setVisible(false);
        if (progressIndicator != null) progressIndicator.setVisible(false);

        if (btnYes != null) btnYes.setVisible(true);
        if (btnNo != null) btnNo.setVisible(true);

        if (lblMessage != null) {
            lblMessage.setText("É necessário atualizar o spLinker. Deseja fazê-lo agora?");
        }
    }

    @FXML
    void onBtnYesClicked() {
        try {
            // Esconder botões e mostrar progresso
            if (btnYes != null) btnYes.setVisible(false);
            if (btnNo != null) btnNo.setVisible(false);

            if (progressBar != null) {
                progressBar.setVisible(true);
                progressBar.setProgress(0);
            }

            if (progressIndicator != null) {
                progressIndicator.setVisible(true);
                progressIndicator.setProgress(0);
            }

            if (lblMessage != null) {
                lblMessage.setText("Preparando download da atualização...");
                lblMessage.getStyleClass().add("update-message");
            }

            // Criar tarefa de download com progresso
            Task<File> downloadTask = new Task<File>() {
                @Override
                protected File call() throws Exception {
                    // Criar diretório de downloads se não existir
                    File homeDir = new File(System.getProperty("user.home"));
                    File downloadDir = new File(homeDir, "Downloads");
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs();
                    }

                    // Definir arquivo de destino
                    File outputFile = new File(downloadDir, "splinker_new_version.msi");

                    // Configurar conexão
                    URL url = new URL(downloadUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "SpLinker-App");

                    // Obter tamanho do arquivo
                    int fileSize = connection.getContentLength();

                    // Iniciar download
                    try (InputStream in = connection.getInputStream();
                         FileOutputStream out = new FileOutputStream(outputFile)) {

                        byte[] buffer = new byte[8192]; // 8KB buffer
                        int bytesRead;
                        long totalBytesRead = 0;

                        // Atualizar mensagem
                        Platform.runLater(() -> {
                            if (lblMessage != null) {
                                lblMessage.setText("Baixando atualização do spLinker...");
                                lblMessage.getStyleClass().add("update-message");
                            }
                        });

                        // Ler e escrever o arquivo, atualizando o progresso
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            // Atualizar progresso na UI
                            if (fileSize > 0) { // Evitar divisão por zero
                                final double progress = (double) totalBytesRead / fileSize;
                                Platform.runLater(() -> {
                                    if (progressBar != null) progressBar.setProgress(progress);
                                    if (progressIndicator != null) progressIndicator.setProgress(progress);
                                });
                            }
                        }
                    }

                    return outputFile;
                }
            };

            // Configurar callbacks para a conclusão do download
            downloadTask.setOnSucceeded(event -> {
                try {
                    File downloadedFile = downloadTask.getValue();

                    // Atualizar UI
                    Platform.runLater(() -> {
                        if (lblMessage != null) {
                            lblMessage.setText("Download concluído!");
                        }
                        if (progressBar != null) progressBar.setProgress(1.0);
                        if (progressIndicator != null) progressIndicator.setProgress(1.0);
                    });

                    // IMPORTANTE: Armazenar o caminho no serviço compartilhado
                    InstallerService.setInstallerPath(downloadedFile.getAbsolutePath());

                    // Aguardar um pouco para o usuário ver que terminou
                    Thread.sleep(1000);

                    // Navegar para a tela de confirmação de instalação
                    Platform.runLater(() -> {
                        try {
                            // Navegar para a tela de confirmação
                            Router.navigateTo(getStage(), "install-confirmation");
                        } catch (Exception e) {
                            Sentry.captureException(e);
                            showError("Erro ao exibir tela de confirmação: " + e.getMessage());
                        }
                    });

                } catch (Exception e) {
                    Sentry.captureException(e);
                    showError("Erro ao processar download: " + e.getMessage());
                }
            });

            // Configurar callbacks para erros durante o download
            downloadTask.setOnFailed(event -> {
                Throwable ex = downloadTask.getException();
                Sentry.captureException(ex);
                showError("Falha ao baixar a atualização: " + ex.getMessage());
            });

            // Executar a tarefa de download
            executor.execute(downloadTask);

        } catch (Exception e) {
            Sentry.captureException(e);
            showError("Erro ao iniciar o download: " + e.getMessage());
        }
    }

    @FXML
    void onBtnNoClicked() {
        try {
            // Fechar o executor
            executor.shutdownNow();

            // Voltar para a tela principal
            SpLinkerUpdateService.verifyOSVersion();
            navigateToHome();
        } catch (Exception e) {
            Sentry.captureException(e);
            getStage().close();
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            if (lblMessage != null) lblMessage.setText(message);
            if (progressBar != null) progressBar.setVisible(false);
            if (progressIndicator != null) progressIndicator.setVisible(false);
            if (btnYes != null) {
                btnYes.setText("Voltar");
                btnYes.setVisible(true);
                btnYes.setOnMouseClicked(__ -> navigateToHome());
            }
        });
    }

    private void navigateToHome() {
        try {
            // Navegar para a tela principal
            Router.navigateTo(getStage(), "home");
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