package br.org.cria.splinkerapp.services.implementations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.BaseRepository;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.utils.StringStandards;

public class DataSetService extends BaseRepository {

    public static String getCurrentToken() throws Exception
    {
        var token = System.getProperty("splinker_token");
        if(token == null)
        {
            var cmd = "SELECT token FROM DataSetConfiguration LIMIT 1;";
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var result = runQuery(cmd, conn);
            token = result.getString("token");
            result.close();
            conn.close();
            setCurrentToken(token);
        }
        return token;

    }
   
    public static void setCurrentToken(String token)
    {
        System.setProperty("splinker_token", token);
    }
    
    public static void updateRowcount(String token, int rowCount) throws Exception
    {
        var cmd = "UPDATE DataSetConfiguration SET last_rowcount = ? WHERE token = ?;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setInt(1, rowCount);
        stm.setString(2, token);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }
   
    public static void deleteDataSet(String token) throws Exception
    {
        var cmd = "DELETE FROM DataSetConfiguration WHERE token = ?;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, token);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }

    public static Map<String, Object> getConfigurationDataFromAPI(String token) throws Exception
    {
     
        String line;
        var config = CentralServiceRepository.getCentralServiceData();
        var url = "%s?version=%s&token=%s".formatted(config.getCentralServiceUrl(), config.getSystemVersion(), token);
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

    public static boolean hasConfiguration() throws Exception
    {
        var cmd = "SELECT COUNT(token) AS TOKEN_COUNT FROM  DataSetConfiguration;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        var hasConfig =  result.getInt("TOKEN_COUNT") > 0;
        result.close();
        conn.close();
        return hasConfig;
    }
    
    public static boolean checkIfRecordsHaveDecreased(String token, int newRowCount) throws Exception
    {
        var cmd = """
                SELECT last_rowcount FROM  DataSetConfiguration
                WHERE token = ?
                LIMIT 1;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, token);
        var result = stm.executeQuery();
        int lastRowCount = 0;
        while (result.next()) 
        {
            lastRowCount = result.getInt("last_rowcount");    
        }
        var hasLessRowsThanBefore = lastRowCount > newRowCount;
        
        return hasLessRowsThanBefore;
        
    }

    public static List<DataSet> getAllDataSets() throws Exception
    {
        var sources = new ArrayList<DataSet>();
        var cmd = """
                    SELECT * FROM  DataSetConfiguration;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var results = runQuery(cmd, conn);
        while (results.next()) 
        {
            var ds = buildFromResultSet(results);
            sources.add(ds);    
        }
        results.close();
        conn.close();
        return sources;
    }

    public static DataSet getDataSetBy(String field, String value) throws Exception
    {
        var cmd = "SELECT * FROM  DataSetConfiguration WHERE %s = ? LIMIT 1;"
        .formatted(field);
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, value);
        var result = stm.executeQuery();
        DataSet ds = buildFromResultSet(result);
        result.close();
        conn.close();
        return ds;
    }

    private static DataSet buildFromResultSet(ResultSet result) throws Exception
    {
                var token = result.getString("token");
                var host = result.getString("db_host");
                var port = result.getString("db_port");
                var dbName = result.getString("db_name");
                var table = result.getString("db_tablename");
                var user = result.getString("db_username");
                var pwd = result.getString("db_password");
                var filePath = result.getString("datasource_filepath");
                var acronym =  result.getString("dataset_acronym");
                var name =  result.getString("dataset_name");
                var lastRowCount =  result.getInt("last_rowcount");
                var type =result.getString("datasource_type") == null? null:
                 DataSourceType.valueOf(result.getString("datasource_type"));
                
                var ds = DataSet.factory(token, type, filePath, host,dbName, table,user, pwd, port, acronym, name, lastRowCount);
                return ds;
    }

    public static DataSet getDataSet(String token) throws Exception
    {

        return getDataSetBy("token",token);
    }

    public static void saveSQLCommand(String token, List<Double> cmd) throws Exception
    {
        var ds = getDataSet(token);
        var normalizedName = StringStandards.normalizeString(ds.getDataSetAcronym());
        var fileName = "%s/%s.sql".formatted(System.getProperty("user.dir"), normalizedName);
        var sqlCmd = byteArrayToString(cmd);
        Path path = Paths.get(fileName);
        byte[] strToBytes = sqlCmd.getBytes();
        if(Files.exists(path))
        {
            Files.delete(path);
        }
        Files.write(path, strToBytes);
    }

    public static String getSQLCommand(String token) throws Exception 
    {
        var ds = getDataSet(token);
        var normalizedName = StringStandards.normalizeString(ds.getDataSetAcronym());
        var fileName = "%s/%s.sql".formatted(System.getProperty("user.dir"), normalizedName);
        var lines = Files.readAllLines(Path.of(fileName));
        String read = String.join(" ", lines);
        return read;
    }

    /**
     * Salva os dados da coleção.
     * @param token - token da coleção
     * @param type - Tipo de fonte de dados
     * @param acronym - acrônimo da coleção
     * @param name - nome da coleção
     */
    public static void saveDataSet(String token, DataSourceType type, String acronym, String name) throws Exception
    {
 
        var cmd = """
                    INSERT INTO DataSetConfiguration
                    (token, datasource_type,
                    dataset_acronym, dataset_name, last_rowcount)
                    VALUES(?,?,?,?,?);
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, token);
        stm.setString(2,  type.name());
        stm.setString(3, acronym);
        stm.setString(4, name);
        stm.setInt(5, 0);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }

    public static void saveAccessDataSource(String token, String filePath, String userName, String password) throws Exception
    {
        var cmd = """
                    UPDATE DataSetConfiguration
                    SET datasource_filepath = ?
                    db_username = ?, db_password = ?
                    WHERE token = ?;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1,filePath);
        stm.setString(2, userName);
        stm.setString(3, password);
        stm.setString(4, token);
        
        stm.executeUpdate();
        stm.close();
        conn.close();

    }

    public static void saveSQLDataSource(String token, String host, String port, 
                            String dbName, String tableName, String userName, String password) throws Exception
    {
         var cmd = """
                    UPDATE DataSetConfiguration
                    SET db_name = ?,
                    db_username = ?, db_password = ?
                    db_host = ?, db_port = ?,
                    db_tablename = ?
                    WHERE token = ?;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, dbName);
        stm.setString(2, userName);
        stm.setString(3, password);
        stm.setString(4, host);
        stm.setString(5, port);
        stm.setString(6, tableName);
        stm.setString(7, token);
        stm.executeUpdate();
        stm.close();
        conn.close();

    }

    public static void saveSpreadsheetDataSource(String token, String filePath) throws Exception
    {
        var cmd = """
                    UPDATE DataSetConfiguration
                    SET datasource_filepath = ?
                    WHERE token = ?;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, filePath);
        stm.setString(2, token);
        
        stm.executeUpdate();
        stm.close();
        conn.close();
    }
  
    public static void updateDataSource(HashMap<String, String> args) throws Exception
    {

        var token = args.get("token");
        var sqlFieldList = args.keySet().stream().filter(e -> e!= "token").map(elem -> "%s = ?".formatted(elem)).toList();
        var fields = String.join(",", sqlFieldList);
        var cmd = "UPDATE  DataSetConfiguration SET %s WHERE token = ?;".formatted(fields);
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        var i = 1;
        for (String field : sqlFieldList) 
        {
            var arg = field.split(" ")[0];
            var value = args.get(arg);
            stm.setString(i, value);
            i++;
        }
        stm.setString(i, token);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }
}
