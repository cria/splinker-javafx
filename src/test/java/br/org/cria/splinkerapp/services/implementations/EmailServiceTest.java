package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.EmailConfiguration;
import org.junit.Test;
import javax.mail.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EmailServiceTest {
    private EmailConfiguration config(String recipient, String user, String password) {
        var config = new EmailConfiguration(); config.setContact_email_recipient(recipient); config.setContact_email_send(user); config.setContact_email_token(password); return config;
    }
    @Test public void composesEmailWithoutSendingToRealRecipients() throws Exception {
        try (var data = mockStatic(DataSetService.class); var transport = mockStatic(Transport.class)) {
            data.when(DataSetService::getEmailConfiguration).thenReturn(config("support@example.invalid", "sender@example.invalid", "test-password"));
            new EmailService().sendEmail("Assunto", "Mensagem", "user@example.invalid", "test-token");
            var message = org.mockito.ArgumentCaptor.forClass(Message.class); transport.verify(() -> Transport.send(message.capture()));
            assertEquals("[ test-token ] - Assunto", message.getValue().getSubject());
            assertEquals("Mensagem\n\nRemetente: user@example.invalid", message.getValue().getContent());
            assertEquals("support@example.invalid", message.getValue().getAllRecipients()[0].toString());
            assertEquals("sender@example.invalid", message.getValue().getFrom()[0].toString());
        }
    }
    @Test public void rejectsIncompleteConfigurationBeforeSending() throws Exception {
        try (var data = mockStatic(DataSetService.class); var transport = mockStatic(Transport.class)) {
            for (String empty : Arrays.asList(null, "")) {
                for (var config : List.of(config(empty, "user", "pass"), config("recipient", empty, "pass"), config("recipient", "user", empty))) {
                    data.when(DataSetService::getEmailConfiguration).thenReturn(config);
                    var service = new EmailService(); assertThrows(IllegalStateException.class, () -> service.sendEmail("subject", "body", "from", "token"));
                }
            }
            transport.verifyNoInteractions();
        }
    }
    @Test public void preservesConfigurationAndSendFailures() throws Exception {
        try (var data = mockStatic(DataSetService.class); var transport = mockStatic(Transport.class)) {
            data.when(DataSetService::getEmailConfiguration).thenThrow(new java.sql.SQLException("simulated"));
            assertTrue(assertThrows(RuntimeException.class, EmailService::new).getCause() instanceof java.sql.SQLException);
            data.when(DataSetService::getEmailConfiguration).thenReturn(config("support@example.invalid", "sender@example.invalid", "test-password"));
            transport.when(() -> Transport.send(any(Message.class))).thenThrow(new MessagingException("offline"));
            assertThrows(MessagingException.class, () -> new EmailService().sendEmail("subject", "body", "from", "token"));
        }
    }
}
