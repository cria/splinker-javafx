package br.org.cria.splinkerapp.repositories;

import java.sql.DriverManager;

import br.org.cria.splinkerapp.models.RSyncConfig;

public class TransferConfigRepository extends BaseRepository {

    public static RSyncConfig getRSyncConfig() throws Exception {
        var sql = """
                SELECT rsync_port, rsync_server_destination
                FROM TransferConfiguration LIMIT 1;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var statement = conn.createStatement();
        var result = statement.executeQuery(sql);
        var port = result.getInt("rsync_port");
        var destination = result.getString("rsync_server_destination");
        var conf = new RSyncConfig(port, destination);
        statement.close();
        conn.close();
        return conf;
    }

    public static void saveRSyncConfig(int port, String destination) throws Exception {
        cleanTable("TransferConfiguration");
        var sql = """
                INSERT INTO 
                TransferConfiguration (rsync_port, rsync_server_destination)
                VALUES(?,?);
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var statement = conn.prepareStatement(sql);
        statement.setInt(1, port);
        statement.setString(2, destination);
        statement.executeUpdate();
        statement.close();
        conn.close();
    }
}
