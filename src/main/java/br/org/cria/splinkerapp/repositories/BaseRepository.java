package br.org.cria.splinkerapp.repositories;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.utils.DbConnectionUtil;

public class BaseRepository {
    protected static String LOCAL_DB_CONNECTION = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());

    protected static Connection openLocalConnection() throws Exception {
        return DbConnectionUtil.getConnection(LOCAL_DB_CONNECTION);
    }

    protected static void cleanTable(String tableName) throws Exception {
        var cmd = "DELETE FROM %s;".formatted(tableName);
        try (Connection conn = openLocalConnection();
             var statement = conn.prepareStatement(cmd)) {
            statement.executeUpdate();
        }
    }

    public static String byteArrayToString(List<Double> byteArr) {
        var out = new ByteArrayOutputStream();
        byteArr.forEach((e) -> out.write(e.byteValue()));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
