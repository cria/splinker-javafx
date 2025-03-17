package br.org.cria.splinkerapp.repositories;

import java.sql.DriverManager;
public class EmailRepository extends BaseRepository{

    public static void saveEmailData(String emailSend, String emailToken, String emailRecipient) throws Exception {
        var cmd = """
                INSERT INTO EmailConfiguration (contact_email_send, contact_email_token, contact_email_recipient)
                VALUES (?,?,?)
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var statement = conn.prepareStatement(cmd);
        statement.setString(1, emailSend);
        statement.setString(2, emailToken);
        statement.setString(3, emailRecipient);
        statement.executeUpdate();
        statement.close();
        conn.close();

    }
}