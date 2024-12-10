package br.org.cria.splinkerapp.repositories;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

import br.org.cria.splinkerapp.managers.LocalDbManager;

public class BaseRepository {
    protected static String LOCAL_DB_CONNECTION = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());

    protected static ResultSet runQuery(String sql, Connection conn) throws Exception {
        var statement = conn.createStatement();
        var result = statement.executeQuery(sql);
        return result;
    }

    protected static void cleanTable(String tableName) throws Exception {
        var cmd = "DELETE FROM %s;".formatted(tableName);
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var statement = conn.prepareStatement(cmd);
        statement.executeUpdate();
        statement.close();
        conn.close();

    }

    public static String byteArrayToString(List<Double> byteArr) {
        var out = new ByteArrayOutputStream();
        byteArr.forEach((e) -> out.write(e.byteValue()));
        var str = new String(out.toByteArray(), StandardCharsets.UTF_8);
        return str;
    }
}
