package br.org.cria.splinkerapp.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class BaseRepository 
{
    public static String LOCAL_DB_CONNECTION = "jdbc:sqlite:splinker.db";
    
    protected static ResultSet runQuery(String sql, Connection conn) throws Exception
    {
        var statement = conn.createStatement();
        var result = statement.executeQuery(sql);
        return result;
    }
    
    protected static void cleanTable(String tableName) throws Exception
    {
        var cmd = "DELETE FROM %s;".formatted(tableName);
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var statement = conn.prepareStatement(cmd);
        statement.executeUpdate();
        statement.close();
        conn.close();

    }
    
}
