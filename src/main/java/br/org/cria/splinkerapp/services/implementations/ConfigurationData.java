package br.org.cria.splinkerapp.services.implementations;

import java.sql.SQLException;
import java.sql.Statement;
import static java.sql.DriverManager.getConnection;

public abstract class ConfigurationData  {

    private static Statement getStatement() throws SQLException {
        return getConnection("jdbc:sqlite:spLinker.db").createStatement();
    }
    static int getRSyncPort() {
        try {
            var sql = """
                       SELECT rsync_port, rsync_server_destination
                       FROM TransferConfiguration LIMIT 1;
                       """;
            var statement = getStatement();
            var result = statement.executeQuery(sql);
            return result.getInt("rsync_port");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static String getTransferDataDestination() {

        try {
            var sql = """
                       SELECT rsync_server_destination
                       FROM TransferConfiguration LIMIT 1;
                       """;
            var statement = getStatement();
            var result = statement.executeQuery(sql);
            return result.getString("rsync_server_destination");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
