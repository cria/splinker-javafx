package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.services.implementations.EmailService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.net.URL;
import java.util.ResourceBundle;
import javax.mail.MessagingException;

public class EmailController extends AbstractController {

    @FXML
    private ComboBox<String> cmbAssunto;
    @FXML
    private TextArea urlField;
    @FXML
    private TextField emailField;
    @FXML
    private Button btnSave;

    private EmailService emailService = new EmailService();

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbAssunto.getItems().addAll("Sugestões", "Erro na transmissão", "Outros");
        btnSave.setOnAction(e -> enviarEmail());
    }

    private void enviarEmail() {
        String assunto = cmbAssunto.getValue();
        String mensagem = urlField.getText();
        String emailUsuario = emailField.getText();

        if (assunto == null || assunto.isEmpty()) {
            showAlert("Erro", "Por favor, selecione um assunto.");
            return;
        }
        if (mensagem == null || mensagem.isEmpty()) {
            showAlert("Erro", "Por favor, preencha o campo de mensagem.");
            return;
        }
        if (emailUsuario == null || emailUsuario.isEmpty()) {
            showAlert("Erro", "Por favor, informe o seu email.");
            return;
        }

        try {
            emailService.sendEmail(assunto, mensagem, emailUsuario);
            showAlert("Sucesso", "Email enviado com sucesso!");
        } catch (MessagingException me) {
            me.printStackTrace();
            showAlert("Erro", "Falha ao enviar o email: " + me.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro", "Ocorreu um erro: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
