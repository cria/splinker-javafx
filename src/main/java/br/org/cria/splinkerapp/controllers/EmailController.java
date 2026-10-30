package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import java.net.URL;
import java.util.ResourceBundle;
import javax.mail.MessagingException;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.EmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EmailController extends AbstractController {

    @FXML
    private ComboBox<String> cmbAssunto;
    @FXML
    private TextArea urlField;
    @FXML
    private TextField emailField;
    @FXML
    private Button btnSave;
    @FXML
    private ComboBox<String> cmbToken;

    private EmailService emailService = new EmailService();

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbAssunto.getItems().addAll(I18n.text("email.subject.suggestions"), I18n.text("email.subject.transfer"), I18n.text("email.subject.other"));
        try {
            cmbToken.getItems().addAll(TokenRepository.getTokens());
            cmbToken.setValue(TokenRepository.getCurrentTokenSigla());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        btnSave.setOnAction(e -> enviarEmail());
    }

    private void enviarEmail() {
        String assunto = cmbAssunto.getValue();
        String mensagem = urlField.getText();
        String emailUsuario = emailField.getText();
        String token = cmbToken.getValue();

        if (assunto == null || assunto.isEmpty()) {
            showAlert(I18n.text("error"), I18n.text("email.validation.subject"));
            return;
        }
        if (mensagem == null || mensagem.isEmpty()) {
            showAlert(I18n.text("error"), I18n.text("email.validation.message"));
            return;
        }
        if (emailUsuario == null || emailUsuario.isEmpty()) {
            showAlert(I18n.text("error"), I18n.text("email.validation.address"));
            return;
        }
        if (token == null || token.isEmpty()) {
            showAlert(I18n.text("error"), I18n.text("email.validation.token"));
            return;
        }
        try {
            onButtonEnviarClicked();
            new Thread(() -> {
                try {
                    emailService.sendEmail(assunto, mensagem,emailUsuario, token);
                    Platform.runLater(() -> {
                        showAlert(I18n.text("success"), I18n.text("email.sent"));
                        cmbAssunto.setValue(null);
                        urlField.clear();
                        emailField.clear();
                        cmbToken.setValue(null);
                        loadPage("email");
                    });
                } catch (MessagingException me) {
                    me.printStackTrace();
                    Platform.runLater(() -> showAlert(I18n.text("error"), I18n.text("email.failed") + me.getMessage()));
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert(I18n.text("error"), I18n.text("error.occurred") + e.getMessage()));
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(I18n.text("error"), I18n.text("email.start.failed") + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void onButtonEnviarClicked() {
        loadPage("email-sending");
    }
}
