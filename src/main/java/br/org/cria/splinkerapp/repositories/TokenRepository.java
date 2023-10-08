package br.org.cria.splinkerapp.repositories;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.DriverManager;
import java.util.HashMap;
import com.google.gson.Gson;

public class TokenRepository extends BaseRepository
{
    public static String LOCAL_DB_CONNECTION = "jdbc:sqlite:splinker.db";
    
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
        var cmd = "SELECT token FROM BasicConfiguration";
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
    };
    
    public static boolean tokenIsValid(String token) throws Exception
    {
        isNullToken(token);
        String line;
        var url = "http://localhost:8000/api/validate_token?token=%s".formatted(token);
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
        HashMap<String, Boolean> json = new Gson().fromJson(response.toString(), HashMap.class);

        var isValid = json.get("valid");
        return isValid;

    }
    
}
