package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.repositories.TokenRepository;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailService {

    private String destinatario;
    private String usuario;
    private String senha;
    private Properties emailProps;

    public EmailService() {
        loadEmailConfigurations();
        setupEmailProperties();
    }

    private void loadEmailConfigurations() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("src/main/java/br/org/cria/splinkerapp/properties/email.properties")) {
            props.load(input);
            this.destinatario = props.getProperty("email.destinatario");
            this.usuario = props.getProperty("email.usuario");
            this.senha = props.getProperty("email.senha");
        } catch (IOException ex) {
            throw new RuntimeException("Não foi possível carregar as configurações de e-mail: " + ex.getMessage(), ex);
        }
    }

    private void setupEmailProperties() {
        emailProps = new Properties();
        emailProps.put("mail.smtp.auth", "true");
        emailProps.put("mail.smtp.starttls.enable", "true");
        emailProps.put("mail.smtp.host", "smtp.gmail.com");
        emailProps.put("mail.smtp.port", "587");
    }

    public void sendEmail(String assunto, String mensagem, String emailUsuario) throws Exception {
        if (destinatario == null || destinatario.isEmpty() ||
                usuario == null || usuario.isEmpty() ||
                senha == null || senha.isEmpty()) {
            throw new IllegalStateException("Dados de configuração de email não foram definidos corretamente.");
        }

        Session session = Session.getInstance(emailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, senha);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(usuario));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        message.setSubject(assunto);
        String token = TokenRepository.getCurrentToken();
        message.setText(mensagem + " de: " + emailUsuario + " no token: " + token);

        Transport.send(message);
    }
}
