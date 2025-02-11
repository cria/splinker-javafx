package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailController extends AbstractController {

    @FXML
    private ComboBox<String> cmbAssunto;
    @FXML
    private TextArea urlField;
    @FXML
    private TextField emailField;
    @FXML
    private Button btnSave;

    private String destinatario = System.getenv("EMAIL_DESTINATARIO");
    private String usuario = System.getenv("EMAIL_USUARIO");
    private String senha = System.getenv("EMAIL_SENHA");

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        loadEmailConfigurations();

        cmbAssunto.getItems().add("Sugestões");
        cmbAssunto.getItems().add("Erro na transmissão");
        cmbAssunto.getItems().add("Outros");
        btnSave.setOnAction(e -> enviarEmail());
    }

    private void loadEmailConfigurations() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("src/main/java/br/org/cria/splinkerapp/properties/email.properties")) {
            props.load(input);
            destinatario = props.getProperty("email.destinatario");
            usuario = props.getProperty("email.usuario");
            senha = props.getProperty("email.senha");
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erro", "Não foi possível carregar as configurações de e-mail: " + ex.getMessage());
        }
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

        // Verifica se as variáveis de ambiente foram definidas
        if (destinatario == null || destinatario.isEmpty() ||
                usuario == null || usuario.isEmpty() ||
                senha == null || senha.isEmpty()) {
            showAlert("Erro", "Dados de configuração de email não foram definidos corretamente.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, senha);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(usuario));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(assunto);
            message.setText(mensagem + " de: " + emailUsuario + " no token: " + TokenRepository.getCurrentToken()
                    );
            Transport.send(message);
            showAlert("Sucesso", "Email enviado com sucesso!");
        } catch (MessagingException e) {
            e.printStackTrace();
            showAlert("Erro", "Falha ao enviar o email: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
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
