package br.org.cria.splinkerapp.config;

import java.sql.DriverManager;
import java.sql.ResultSet;
import br.org.cria.splinkerapp.models.CentralService;

public class BaseConfiguration {
    public static String LOCAL_DB_CONNECTION = "jdbc:sqlite:splinker.db";
    public static void setToken(String token) throws Exception
    {
        var cmd = "INSERT INTO BasicConfiguration (token) VALUES(?)";
        var statement = DriverManager.getConnection(LOCAL_DB_CONNECTION).prepareStatement(cmd);
        statement.setString(1, token);
        statement.executeUpdate();
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
    public static CentralService getCentraServiceData() throws Exception
    {
        var cmd = "SELECT * FROM CentralServiceConfiguration";
        var result = runQuery(cmd);
        var uri = result.getString("central_service_uri");
        var url = result.getString("central_service_uri");
        return new CentralService(uri, url);
    };

    public static void saveCentralServiceData(CentralService cserv) throws Exception
    {
        var cmd = """
                    INSERT INTO CentralServiceConfiguration (central_service_uri, central_service_url) 
                    VALUES(?,?)
                    """;
        var statement = DriverManager.getConnection(LOCAL_DB_CONNECTION).prepareStatement(cmd);
        statement.setString(1, cserv.getCentralServiceUri());
        statement.setString(2, cserv.getCentralServiceUrl());
        statement.executeUpdate();
    }
    


    
}
