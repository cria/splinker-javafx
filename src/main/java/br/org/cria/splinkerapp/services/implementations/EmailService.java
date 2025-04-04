package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.repositories.BaseRepository;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService extends BaseRepository {

    private String destinatario;
    private String usuario;
    private String senha;
    private Properties emailProps;

    public EmailService() {
        loadEmailConfigurations();
        setupEmailProperties();
    }

    private void loadEmailConfigurations() {
        try {

            var results = DataSetService.getEmailConfiguration();
            destinatario = results.getContact_email_recipient();
            usuario = results.getContact_email_send();
            senha = results.getContact_email_token();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar configurações de e-mail.", e);
        }
    }

    private void setupEmailProperties() {
        emailProps = new Properties();
        emailProps.put("mail.smtp.auth", "true");
        emailProps.put("mail.smtp.starttls.enable", "true");
        emailProps.put("mail.smtp.host", "smtp.gmail.com");
        emailProps.put("mail.smtp.port", "587");
        emailProps.put("mail.smtp.ssl.checkserveridentity", "false");
        emailProps.put("mail.smtp.ssl.trust", "*");
    }

    public void sendEmail(String assunto, String mensagem, String emailUsuario, String token) throws Exception {
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
        message.setSubject("[ " + token + " ] - " + assunto);
        message.setText(mensagem + "\n\nRemetente: " + emailUsuario);
        Transport.send(message);
    }
}
