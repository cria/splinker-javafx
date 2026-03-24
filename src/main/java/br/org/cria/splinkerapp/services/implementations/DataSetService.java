package br.org.cria.splinkerapp.services.implementations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataSetService extends BaseRepository {


    private static final Log log = LogFactory.getLog(DataSetService.class);

    public static void updateRowcount(String token, int rowCount) throws Exception {
        var cmd = "UPDATE DataSetConfiguration SET last_rowcount = ? WHERE token = ?;";
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setInt(1, rowCount);
            stm.setString(2, token);
            stm.executeUpdate();
        }
    }

    public static void deleteDataSet(String token) throws Exception {
        var cmd = "DELETE FROM DataSetConfiguration WHERE token = ?;";
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, token);
            stm.executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getConfigurationDataFromAPI(String token) throws Exception {
        var config = CentralServiceRepository.getCentralServiceData();
        var url = "%s?version=%s&token=%s".formatted(config.getCentralServiceUrl(), VersionService.getVersion(), token);
        Object jsonResponse = HttpService.getJson(url);

        if (jsonResponse instanceof Map) {
            return (Map<String, Object>) jsonResponse;
        }
        else if (jsonResponse instanceof List && !((List<?>)jsonResponse).isEmpty()) {
            Object firstItem = ((List<?>)jsonResponse).get(0);
            if (firstItem instanceof Map) {
                return (Map<String, Object>) firstItem;
            }
        }

        throw new Exception("Formato de resposta inesperado da API: " + jsonResponse);
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

        try (var conn = openLocalConnection();
             var stmt1 = conn.prepareStatement(cmd1);
             var result1 = stmt1.executeQuery();
             var stmt2 = conn.prepareStatement(cmd2);
             var result2 = stmt2.executeQuery()) {
            var hasTokens = result1.next() && result1.getInt("TOKEN_COUNT") > 0;
            var hasConfiguredTokens = result2.next() && result2.getInt("HAS_CONFIGURED_TOKENS") > 0;
            return hasTokens && hasConfiguredTokens;
        }
    }

    public static boolean hasConfiguration(String token) throws Exception {

        var cmd2 = " SELECT COUNT(*) AS HAS_CONFIGURED_TOKENS " +
                "FROM DataSetConfiguration " +
                "WHERE TOKEN = ? AND (datasource_filepath IS NOT NULL " +
                "OR db_host IS NOT NULL); ";

        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd2)) {
            stm.setString(1, token);
            try (var result = stm.executeQuery()) {
                return result.next() && result.getInt("HAS_CONFIGURED_TOKENS") > 0;
            }
        }
    }

    public static List<DataSet> getAllDataSets() throws Exception {
        var sources = new ArrayList<DataSet>();
        var cmd = "SELECT * FROM  DataSetConfiguration;";
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd);
             var results = stm.executeQuery()) {
            while (results.next()) {
                var ds = buildFromResultSet(results);
                sources.add(ds);
            }
        }
        return sources;
    }

    public static DataSet getDataSetBy(String field, String value) throws Exception {
        var cmd = "SELECT * FROM  DataSetConfiguration WHERE %s = ? LIMIT 1;"
                .formatted(field);
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, value);
            try (var result = stm.executeQuery()) {
                return result.next() ? buildFromResultSet(result) : null;
            }
        }
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
        return DataSet.factory(token, type, filePath, host, dbName, user, pwd, port,
                acronym, name, lastRowCount, id, updatedAt);
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
        return String.join(" ", lines);
    }

    public static String getSQLCommandFromApi(String token) throws Exception {
        Map config = DataSetService.getConfigurationDataFromAPI(token);
        var cmd = (List<Double>) config.get("sql_command");
        return BaseRepository.byteArrayToString(cmd);
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
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, token);
            stm.setString(2, type.name());
            stm.setString(3, acronym);
            stm.setString(4, name);
            stm.setInt(5, 0);
            stm.setInt(6, id);
            stm.executeUpdate();
        }
    }

    public static void saveAccessDataSource(String token, String filePath, String password) throws Exception {
        var cmd = """
                    UPDATE DataSetConfiguration
                    SET datasource_filepath = ?,
                    db_password = ? WHERE token = ?;
                """;
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, filePath);
            stm.setString(2, password);
            stm.setString(3, token);
            stm.executeUpdate();
        }

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
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, dbName);
            stm.setString(2, userName);
            stm.setString(3, password);
            stm.setString(4, host);
            stm.setString(5, port);
            stm.setString(6, token);
            stm.executeUpdate();
        }

    }

    public static void saveSpreadsheetDataSource(String token, String filePath) throws Exception {
        var cmd = """
                    UPDATE DataSetConfiguration
                    SET datasource_filepath = ?
                    WHERE token = ?;
                """;
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, filePath);
            stm.setString(2, token);
            stm.executeUpdate();
        }
    }

    public static void updateDataSource(HashMap<String, String> args) throws Exception {

        var token = args.get("token");
        var sqlFieldList = args.keySet().stream().filter(e -> !"token".equals(e)).map(elem -> "%s = ?".formatted(elem))
                .toList();
        var fields = String.join(",", sqlFieldList);
        var cmd = "UPDATE  DataSetConfiguration SET %s WHERE token = ?;".formatted(fields);
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            var i = 1;
            for (String field : sqlFieldList) {
                var arg = field.split(" ")[0];
                var value = args.get(arg);
                stm.setString(i, value);
                i++;
            }
            stm.setString(i, token);
            stm.executeUpdate();
        }
    }

    public static void insertTransferHistory(HashMap<String, String> args) throws Exception {
        String cmd = "INSERT INTO TransferHistoryDataSet (token, rowcount, send_date) VALUES (?, ?, ?);";

        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, args.get("token"));
            stm.setString(2, args.get("last_rowcount"));
            stm.setString(3, args.get("updated_at"));
            stm.executeUpdate();
        }
    }

    public static List<TransferHistoryDataSet> getTransferHistory() throws Exception {
        String token = TokenRepository.getCurrentToken();
        var sources = new ArrayList<TransferHistoryDataSet>();
        var cmd = "SELECT * FROM  TransferHistoryDataSet th WHERE th.token = ? ORDER BY th.created_at DESC;";
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd)) {
            stm.setString(1, token);
            try (var results = stm.executeQuery()) {
                while (results.next()) {
                    TransferHistoryDataSet dataHistory= new TransferHistoryDataSet();
                    dataHistory.setDate(results.getString("send_date"));
                    dataHistory.setRowcount(results.getString("rowcount"));
                    sources.add(dataHistory);
                }
            }
        }
        return sources;
    }

    public static EmailConfiguration getEmailConfiguration() throws Exception {
        var cmd = "SELECT * FROM EmailConfiguration";
        EmailConfiguration emailConfiguration = new EmailConfiguration();
        try (var conn = openLocalConnection();
             var stm = conn.prepareStatement(cmd);
             var results = stm.executeQuery()) {
            if (results.next()) {
                emailConfiguration.setContact_email_recipient(results.getString("contact_email_recipient"));
                emailConfiguration.setContact_email_send(results.getString("contact_email_send"));
                emailConfiguration.setContact_email_token(results.getString("contact_email_token"));
            }
        }
        return emailConfiguration;
    }
}
