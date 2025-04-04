package br.org.cria.splinkerapp.services.implementations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.models.EmailConfiguration;
import br.org.cria.splinkerapp.models.TransferHistoryDataSet;
import br.org.cria.splinkerapp.repositories.BaseRepository;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DataSetService extends BaseRepository {


    public static void updateRowcount(String token, int rowCount) throws Exception {
        var cmd = "UPDATE DataSetConfiguration SET last_rowcount = ? WHERE token = ?;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setInt(1, rowCount);
        stm.setString(2, token);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }

    public static void deleteDataSet(String token) throws Exception {
        var cmd = "DELETE FROM DataSetConfiguration WHERE token = ?;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, token);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }

    public static Map<String, Object> getConfigurationDataFromAPI(String token) throws Exception {

        var config = CentralServiceRepository.getCentralServiceData();
        var url = "%s?version=%s&token=%s".formatted(config.getCentralServiceUrl(), config.getSystemVersion(), token);
        var json = HttpService.getJson(url);
        return json;
    }

    public static boolean hasConfiguration() throws Exception {

        var cmd1 = "SELECT COUNT(TOKEN) as TOKEN_COUNT FROM DataSetConfiguration;";
        var cmd2 = """
                SELECT COUNT(*) AS HAS_CONFIGURED_TOKENS
                FROM DataSetConfiguration
                WHERE TOKEN IS NOT NULL 
                AND (datasource_filepath IS NOT NULL 
                OR db_host IS NOT NULL);
                """;

        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd1, conn);
        var hasTokens = result.getInt("TOKEN_COUNT") > 0;
        result = runQuery(cmd2, conn);
        var hasConfiguredTokens = result.getInt("HAS_CONFIGURED_TOKENS") > 0;
        var hasConfig = hasTokens && hasConfiguredTokens;

        result.close();
        conn.close();
        return hasConfig;
    }

    public static boolean hasConfiguration(String token) throws Exception {

        var cmd2 = " SELECT COUNT(*) AS HAS_CONFIGURED_TOKENS " +
                "FROM DataSetConfiguration " +
                "WHERE TOKEN = '" + token + "' AND (datasource_filepath IS NOT NULL " +
                "OR db_host IS NOT NULL); ";

        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd2, conn);
        var hasConfiguredTokens = result.getInt("HAS_CONFIGURED_TOKENS") > 0;

        result.close();
        conn.close();
        return hasConfiguredTokens;
    }

    public static List<DataSet> getAllDataSets() throws Exception {
        var sources = new ArrayList<DataSet>();
        var cmd = "SELECT * FROM  DataSetConfiguration;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var results = runQuery(cmd, conn);
        while (results.next()) {
            var ds = buildFromResultSet(results);
            sources.add(ds);
        }
        results.close();
        conn.close();
        return sources;
    }

    public static DataSet getDataSetBy(String field, String value) throws Exception {
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

    private static DataSet buildFromResultSet(ResultSet result) throws Exception {


        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


        var token = result.getString("token");
        if (token == null) {
            return null;
        }
        var id = result.getInt("id");
        var host = result.getString("db_host");
        var port = result.getString("db_port");
        var dbName = result.getString("db_name");
        var user = result.getString("db_username");
        var pwd = result.getString("db_password");
        var filePath = result.getString("datasource_filepath");
        var acronym = result.getString("dataset_acronym");
        var name = result.getString("dataset_name");
        var lastRowCount = result.getInt("last_rowcount");
        var type = result.getString("datasource_type") == null ? null
                : DataSourceType.valueOf(result.getString("datasource_type"));
        var strUpdatedAt = result.getString("updated_at");
        var updatedAt = strUpdatedAt == null ? null : LocalDateTime.parse(strUpdatedAt, dateFormatter);
        var ds = DataSet.factory(token, type, filePath, host, dbName, user, pwd, port,
                acronym, name, lastRowCount, id, updatedAt);

        return ds;
    }

    public static DataSet getDataSet(String token) throws Exception {

        return getDataSetBy("token", token);
    }

    public static void saveSQLCommand(String token, List<Double> cmd) throws Exception {
        var ds = getDataSet(token);
        var id = ds.getId();
        var fileName = "%s/%s.sql".formatted(System.getProperty("user.dir"), id);
        var sqlCmd = byteArrayToString(cmd);
        Path path = Paths.get(fileName);
        byte[] strToBytes = sqlCmd.getBytes();
        if (Files.exists(path)) {
            Files.delete(path);
        }
        Files.write(path, strToBytes);
    }

    public static String getSQLCommand(String token) throws Exception {
        var ds = getDataSet(token);
        var id = ds.getId();
        var fileName = "%s/%s.sql".formatted(System.getProperty("user.dir"), id);
        var lines = Files.readAllLines(Path.of(fileName));
        String read = String.join(" ", lines);
        return read;
    }


    public static String getSQLCommandFromApi(String token) throws Exception {
        Map config = DataSetService.getConfigurationDataFromAPI(token);
        var cmd = (List<Double>) config.get("sql_command");
        String resultado = BaseRepository.byteArrayToString(cmd);
        log.info("Resultado da query SQL: {}", resultado);
        return resultado;
    }

    /**
     * Salva os dados da coleção.
     *
     * @param token   - token da coleção
     * @param type    - Tipo de fonte de dados
     * @param acronym - acrônimo da coleção
     * @param name    - nome da coleção
     */
    public static void saveDataSet(String token, DataSourceType type, String acronym, String name, int id)
            throws Exception {

        var cmd = """
                    INSERT INTO DataSetConfiguration
                    (token, datasource_type,
                    dataset_acronym, dataset_name, last_rowcount, id)
                    VALUES(?,?,?,?,?,?);
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, token);
        stm.setString(2, type.name());
        stm.setString(3, acronym);
        stm.setString(4, name);
        stm.setInt(5, 0);
        stm.setInt(6, id);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }

    public static void saveAccessDataSource(String token, String filePath, String password) throws Exception {
        var cmd = """
                    UPDATE DataSetConfiguration
                    SET datasource_filepath = ?,
                    db_password = ? WHERE token = ?;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, filePath);
        stm.setString(2, password);
        stm.setString(3, token);

        stm.executeUpdate();
        stm.close();
        conn.close();

    }

    public static void saveSQLDataSource(String token, String host, String port,
                                         String dbName, String userName, String password) throws Exception {
        var cmd = """
                    UPDATE DataSetConfiguration
                    SET db_name = ?,
                    db_username = ?, db_password = ?,
                    db_host = ?, db_port = ?
                    WHERE token = ?;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, dbName);
        stm.setString(2, userName);
        stm.setString(3, password);
        stm.setString(4, host);
        stm.setString(5, port);
        stm.setString(6, token);
        stm.executeUpdate();
        stm.close();
        conn.close();

    }

    public static void saveSpreadsheetDataSource(String token, String filePath) throws Exception {
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

    public static void updateDataSource(HashMap<String, String> args) throws Exception {

        var token = args.get("token");
        var sqlFieldList = args.keySet().stream().filter(e -> e != "token").map(elem -> "%s = ?".formatted(elem))
                .toList();
        var fields = String.join(",", sqlFieldList);
        var cmd = "UPDATE  DataSetConfiguration SET %s WHERE token = ?;".formatted(fields);
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        var i = 1;
        for (String field : sqlFieldList) {
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

    public static void insertTransferHistory(HashMap<String, String> args) throws Exception {
        String cmd = "INSERT INTO TransferHistoryDataSet (token, rowcount, send_date) VALUES ('"
                + args.get("token") + "', '"
                + args.get("last_rowcount") + "', '"
                + args.get("updated_at") + "');";

        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }

    public static List<TransferHistoryDataSet> getTransferHistory() throws Exception {
        String token = TokenRepository.getCurrentToken();
        var sources = new ArrayList<TransferHistoryDataSet>();
        var cmd = "SELECT * FROM  TransferHistoryDataSet th WHERE th.token = '" + token + "' ORDER BY th.created_at DESC;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var results = runQuery(cmd, conn);
        while (results.next()) {
            TransferHistoryDataSet dataHistory= new TransferHistoryDataSet();
            dataHistory.setDate(results.getString("send_date"));
            dataHistory.setRowcount(results.getString("rowcount"));
            sources.add(dataHistory);
        }
        results.close();
        conn.close();
        return sources;
    }

    public static EmailConfiguration getEmailConfiguration() throws Exception {
        var cmd = "SELECT * FROM EmailConfiguration";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var results = runQuery(cmd, conn);
        EmailConfiguration emailConfiguration = new EmailConfiguration();
        if (results.next()) {
            emailConfiguration.setContact_email_recipient(results.getString("contact_email_recipient"));
            emailConfiguration.setContact_email_send(results.getString("contact_email_send"));
            emailConfiguration.setContact_email_token(results.getString("contact_email_token"));
        }
        results.close();
        conn.close();
        return emailConfiguration;
    }
}
