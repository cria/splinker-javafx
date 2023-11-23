package br.org.cria.splinkerapp.repositories;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import br.org.cria.splinkerapp.models.BasicConfiguration;

public class BasicConfigurationRepository extends BaseRepository
{
     public static boolean hasConfiguration() throws Exception
    {
        var cmd = "SELECT COUNT(token) AS TOKEN_COUNT FROM BasicConfiguration;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        var hasConfig =  result.getInt("TOKEN_COUNT") > 0;
        result.close();
        conn.close();
        return hasConfig;
    }
  
    public static BasicConfiguration getBasicConfigurationBy(String fieldName, String value) throws Exception
    { 
        var cmd = "SELECT * FROM BasicConfiguration WHERE %s = ? LIMIT 1;".formatted(fieldName);
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, value);
        var result = stm.executeQuery();
        BasicConfiguration bc = null;
        
        while(result.next())
        {
            var token = result.getString("token");
            var collectionName = result.getString("collection_name");
            var lastRowCount = result.getInt("last_rowcount");
            bc = new BasicConfiguration(token,collectionName,lastRowCount);
        }

        stm.close();
        conn.close();
        return bc;
    }
    public static void saveBasicConfiguration(String token, String collectionName) throws Exception
    {
        var isValidConfiguration = validateBasicConfiguration(token, collectionName);
        
        if(isValidConfiguration)
        {
            var cmd = "INSERT INTO BasicConfiguration (token, collection_name) VALUES(?,?);";
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var statement = conn.prepareStatement(cmd);
            statement.setString(1, token);
            statement.setString(2,collectionName);
            statement.executeUpdate();
            statement.close();
            conn.close();
        }
    };
    public static Map<String, Object> getConfigurationDataFromAPI(String token) throws Exception
    {
     
        String line;
        var config = CentralServiceRepository.getCentralServiceData();
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
        HashMap<String, Object> json = new Gson().fromJson(response.toString(), HashMap.class);
        return json;
    }
    public static String getCurrentToken() throws Exception
    {
        var token = System.getProperty("splinker_token");
        if(token == null)
        {
            var cmd = "SELECT token FROM BasicConfiguration LIMIT 1;";
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var result = runQuery(cmd, conn);
            token = result.getString("token");
            result.close();
            conn.close();
        }
        return token;

    }
    public static void setCurrentToken(String token)
    {
        System.setProperty("splinker_token", token);
    }
    public static void updateRowcount(String token, int rowCount) throws Exception
    {
        var cmd = "UPDATE BasicConfiguration SET last_rowcount = ? WHERE token = ?;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setInt(1, rowCount);
        stm.setString(2, token);
        stm.executeUpdate();
        stm.close();
        conn.close();

    }
    public static void deleteConfiguration(String token) throws Exception
    {
        var cmd = "DELETE FROM BasicConfiguration WHERE token = ?;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, token);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }
    private static boolean validateBasicConfiguration(String token, String collectionName)
    {
        var hasToken = token != null && token != "";
        var hasCollectionName = collectionName != null && collectionName != "";
        return hasCollectionName && hasToken;
    }
}
