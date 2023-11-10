package br.org.cria.splinkerapp.repositories;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class TokenRepository extends BaseRepository
{
    public static boolean hasConfiguration() throws Exception
    {
        return getToken() != null;
    }
    
    private static void isNullToken(String token) throws Exception
    {
        if(token == null)
        {
            throw new Exception("Token não pode ser nulo.");
        }
    }

    public static String getToken() throws Exception
    { 
        var cmd = "SELECT token FROM BasicConfiguration;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        var token = result.getString("token");
        conn.close();
        return token;
    }

    public static void saveToken(String token) throws Exception{
        isNullToken(token);
        //cleanTable("BasicConfiguration");
        var cmd = "INSERT INTO BasicConfiguration (token) VALUES(?)";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var statement = conn.prepareStatement(cmd);
        statement.setString(1, token);
        statement.executeUpdate();
        statement.close();
        conn.close();
    };
    
    public static Map getConfigurationDataFromAPI(String token) throws Exception
    {
        isNullToken(token);
        String line;
        var config = CentralServiceRepository.getCentraServiceData();
        var url = "%s/login?version=1&token=%s".formatted(config.getCentralServiceUrl(), token);
        var urlConn = new URI(url).toURL();
        var response = new StringBuffer();
        var connection = (HttpURLConnection) urlConn.openConnection();
        connection.setRequestMethod("GET");
        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        while ((line = reader.readLine()) != null) 
        {
            response.append(line);
        }
        
        reader.close();
        connection.disconnect();
        HashMap json =  new Gson().fromJson(response.toString(), HashMap.class);
        return json;

    }
    
}
