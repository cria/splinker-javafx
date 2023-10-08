package br.org.cria.splinkerapp.repositories;

import java.sql.SQLException;
import java.sql.Statement;

import br.org.cria.splinkerapp.models.RSyncConfig;

import static java.sql.DriverManager.getConnection;

//TODO: Mudar essa configuração para ser remotamente chamada da API
public class TransferConfigRepository  {

    public static RSyncConfig getRSyncConfig() throws Exception
    {
            var sql = """
                       SELECT rsync_port, rsync_server_destination
                       FROM TransferConfiguration LIMIT 1;
                       """;
            var statement = getStatement();
            var result = statement.executeQuery(sql);
            var port = result.getInt("rsync_port");
            var destination = result.getString("rsync_port");
            var conf = new RSyncConfig(port, destination);
            return conf;
    }
    private static Statement getStatement() throws SQLException 
    {
        return getConnection("jdbc:sqlite:spLinker.db").createStatement();
    }
}
