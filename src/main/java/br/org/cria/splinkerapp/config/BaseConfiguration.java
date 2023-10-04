package br.org.cria.splinkerapp.config;

import java.sql.DriverManager;
import java.sql.ResultSet;

public class BaseConfiguration {
    public static String LOCAL_DB_CONNECTION = "jdbc:sqlite:splinker.db";
    public static void setToken(String token) throws Exception
    {
        var cmd = "INSERT INTO BasicConfiguration (token) VALUES(?)";
        var statement = DriverManager.getConnection(LOCAL_DB_CONNECTION).prepareStatement(cmd);
        statement.setString(0, token);
        statement.executeQuery();
    }
    public static String getToken() throws Exception
    {
        var cmd = "SELECT token FROM BasicConfiguration";
        var result = runQuery(cmd);
        var token = result.getString("token");
        return token;
    }
    public static boolean hasConfiguration() throws Exception
    {
        return getToken() != null;
    }

    private static ResultSet runQuery(String sql) throws Exception
    {
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        return conn.createStatement().executeQuery(sql);
    }

    
}
