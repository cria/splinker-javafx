package br.org.cria.splinkerapp.services.implementations;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import static java.sql.DriverManager.getConnection;

public class ConfigurationData  {
    static int getRSyncPort() {
        var dbUrl = "jdbc:sqlite:splinker.db";
        try (var conn = getConnection(dbUrl)){
            var sql = "";
            var statement = conn.createStatement();
            var result = statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //var commands = String.join("", lines);
        //var statement = conn.createStatement();
        return 10_000;
    }

    static String getDarwinCoreFileSourcePath() {
        //var now = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime().toString();
        //return System.getProperty("user.dir");
        return "/Users/brunobemfica/Downloads/dwca-tropicosspecimens-v1.124.zip.old.zip";
    }

    static String getTransferDataDestination() {
        return "bruno@34.68.143.184::meu_modulo";
    }

}
