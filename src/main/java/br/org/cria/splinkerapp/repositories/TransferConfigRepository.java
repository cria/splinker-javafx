package br.org.cria.splinkerapp.repositories;

import br.org.cria.splinkerapp.models.RSyncConfig;

public class TransferConfigRepository extends BaseRepository {

    public static RSyncConfig getRSyncConfig() throws Exception {
        var sql = """
                SELECT rsync_port, rsync_server_destination
                FROM TransferConfiguration LIMIT 1;
                """;
        try (var conn = openLocalConnection();
             var statement = conn.prepareStatement(sql);
             var result = statement.executeQuery()) {
            if (result.next()) {
                var port = result.getInt("rsync_port");
                var destination = result.getString("rsync_server_destination");
                return new RSyncConfig(port, destination);
            }
            return new RSyncConfig(0, null);
        }
    }

    public static void saveRSyncConfig(int port, String destination) throws Exception {
        cleanTable("TransferConfiguration");
        var sql = """
                INSERT INTO
                TransferConfiguration (rsync_port, rsync_server_destination)
                VALUES(?,?);
                """;
        try (var conn = openLocalConnection();
             var statement = conn.prepareStatement(sql)) {
            statement.setInt(1, port);
            statement.setString(2, destination);
            statement.executeUpdate();
        }
    }
}
