package br.org.cria.splinkerapp.repositories;

public class EmailRepository extends BaseRepository{

    public static void saveEmailData(String emailSend, String emailToken, String emailRecipient) throws Exception {
        cleanTable("EmailConfiguration");
        var cmd = """
                INSERT INTO EmailConfiguration (contact_email_send, contact_email_token, contact_email_recipient)
                VALUES (?,?,?)
                """;
        try (var conn = openLocalConnection();
             var statement = conn.prepareStatement(cmd)) {
            statement.setString(1, emailSend);
            statement.setString(2, emailToken);
            statement.setString(3, emailRecipient);
            statement.executeUpdate();
        }

    }
}
