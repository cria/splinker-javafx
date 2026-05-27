package br.org.cria.splinkerapp.models;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.utils.DatabaseLogUtil;
import br.org.cria.splinkerapp.utils.DbConnectionUtil;

public class DataSet {
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int lastRowCount;

    public int getLastRowCount() {
        return lastRowCount;
    }

    public void setLastRowCount(int lastRowCount) {
        this.lastRowCount = lastRowCount;
    }

    private DataSourceType type;

    public DataSourceType getType() {
        return type;
    }

    public void setType(DataSourceType type) {
        this.type = type;
    }

    private String connectionString;
    private String dbHost;

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    private String dbName;

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    private String dbUser;

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    private String dbPassword;

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    private String dbPort;

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    private String dataSourceFilePath;

    public String getDataSetFilePath() {
        return dataSourceFilePath;
    }

    public void setDataSourceFilePath(String dataSourceFilePath) {
        this.dataSourceFilePath = dataSourceFilePath;
    }

    private LocalDateTime updatedAt;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String datasetName;

    public String getDataSetName() {
        return datasetName;
    }

    public void setDataSetName(String datasetName) {
        this.datasetName = datasetName;
    }

    private String datasetAcronym;

    public String getDataSetAcronym() {
        return datasetAcronym;
    }

    public void setDataSetAcronym(String datasetAcronym) {
        this.datasetAcronym = datasetAcronym;
    }

    public String getConnectionString() {
        return connectionString;
    }

    private DataSet(String token, DataSourceType type, String filePath, String host, String databaseName,
                    String username, String password, String port, String connectionString, String datasetAcronym,
                    String datasetName, int lastRowCount, int id, LocalDateTime updatedAt) {
        this.id = id;
        this.token = token;
        this.type = type;
        this.dbHost = host;
        this.dbName = databaseName;
        this.dbUser = username;
        this.dbPassword = password;
        this.dbPort = port;
        this.connectionString = connectionString;
        this.dataSourceFilePath = filePath;
        this.datasetAcronym = datasetAcronym;
        this.datasetName = datasetName;
        this.lastRowCount = lastRowCount;
        this.updatedAt = updatedAt;
    }

    public boolean isFile() {
        return this.type.equals(DataSourceType.CSV) ||
                this.type.equals(DataSourceType.Excel) ||
                this.type.equals(DataSourceType.GoogleSheets) ||
                this.type.equals(DataSourceType.LibreOfficeCalc) ||
                this.type.equals(DataSourceType.Numbers) ||
                this.type.equals(DataSourceType.dBase);
    }

    public boolean isAccessDb() {
        return this.type == DataSourceType.Access;
    }

    public boolean isSQLDatabase() {
        return this.type.equals(DataSourceType.MySQL) ||
                this.type.equals(DataSourceType.MariaDB) ||
                this.type.equals(DataSourceType.Oracle) ||
                this.type.equals(DataSourceType.PostgreSQL) ||
                this.type.equals(DataSourceType.SQLServer);
    }

    public static DataSet factory(String token, DataSourceType type, String filePath, String host,
                                  String databaseName, String username, String password,
                                  String port, String datasetAcronym, String datasetName,
                                  int lastRowCount, int id, LocalDateTime updated_at) {
        DataSet ds;
        String connectionString = null;

        if (type == DataSourceType.PostgreSQL) {
            ApplicationLog.info("[POSTGRES] Construindo DataSet. token=%s, id=%s, type=%s, host=%s, port=%s, dbName=%s, user=%s, hasPassword=%s"
                    .formatted(token, id, type, host, port, databaseName, username, password != null && !password.isBlank()));
        }

        if (type != null) {
            switch (type) {
                case MySQL:
                    connectionString = "jdbc:%s://%s/%s?user=%s&password=%s"//&allowPublicKeyRetrieval=true&useSSL=false"
                            .formatted(type.name().toLowerCase(), host, databaseName, username, password);
                    break;
                case MariaDB:
                    connectionString = "jdbc:mariadb://%s:%s/%s?user=%s&password=%s"
                            .formatted(host, port, databaseName, username, password);
                    break;
                case PostgreSQL:
                    connectionString = "jdbc:%s://%s:%s/%s?user=%s&password=%s"
                            .formatted(type.name().toLowerCase(), host, port, databaseName, username, password);
                    ApplicationLog.info("[POSTGRES] String JDBC base gerada para PostgreSQL: %s"
                            .formatted(DatabaseLogUtil.showJdbcUrlWithCredentials(connectionString)));
                    break;
                case Oracle:
                    connectionString = "jdbc:%s:thin:%s/%s@%s:%s:%s"
                            .formatted(type.name().toLowerCase(), username, password, host, port, databaseName);
                    break;
                case SQLServer:
                    connectionString = "jdbc:%s://%s:%s;encrypt=true;trustServerCertificate=true;databaseName=%s;user=%s;password=%s"
                            .formatted(type.name().toLowerCase(), host, port, databaseName, username, password);
                    break;
                default:
                    connectionString = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());
                    break;
            }
        }
        ds = new DataSet(token, type, filePath, host, databaseName,
                username, password, port, connectionString, datasetAcronym,
                datasetName, lastRowCount, id, updated_at);
        if (type == DataSourceType.PostgreSQL) {
            ApplicationLog.info("[POSTGRES] DataSet PostgreSQL construido: %s".formatted(DatabaseLogUtil.describeDataSet(ds)));
        }
        return ds;
    }

    public Connection getDataSetConnection() throws Exception {
        if (this.type == DataSourceType.PostgreSQL) {
            ApplicationLog.info("[POSTGRES] Abrindo conexao do DataSet PostgreSQL com fallback SSL. %s"
                    .formatted(DatabaseLogUtil.describeDataSet(this)));
            return getPostgreSqlConnectionWithSslFallback();
        }

        var hasUserName = this.dbUser != null && this.dbUser.trim().length() > 0;
        var passwordIsNull = this.dbPassword != null;
        var hasPassword = this.isAccessDb() ? passwordIsNull && this.dbPassword.trim().length() > 0
                : true;

        var hasUserNameAndPassword = hasUserName && hasPassword;
        return hasUserNameAndPassword ?
                DbConnectionUtil.getConnection(connectionString, this.dbUser, this.dbPassword) :
                DbConnectionUtil.getConnection(connectionString);
    }

    private Connection getPostgreSqlConnectionWithSslFallback() throws SQLException {
        SQLException firstException;
        DatabaseLogUtil.logTcpProbe("conexao-real", this.dbHost, this.dbPort, 5000);

        try {
            ApplicationLog.info("[POSTGRES] Tentativa 1/2 de conexao PostgreSQL usando sslmode=require.");
            return getConnection(withPostgreSqlSslMode("require"));
        } catch (SQLException e) {
            firstException = e;
            ApplicationLog.info("[POSTGRES] Falha na tentativa PostgreSQL com sslmode=require. %s"
                    .formatted(DatabaseLogUtil.describeSqlException(e)));
        }

        try {
            ApplicationLog.info("[POSTGRES] Tentativa 2/2 de conexao PostgreSQL usando sslmode=disable.");
            return getConnection(withPostgreSqlSslMode("disable"));
        } catch (SQLException e) {
            ApplicationLog.info("[POSTGRES] Falha na tentativa PostgreSQL com sslmode=disable. %s"
                    .formatted(DatabaseLogUtil.describeSqlException(e)));
            e.addSuppressed(firstException);
            throw e;
        }
    }

    private Connection getConnection(String url) throws SQLException {
        var hasUserName = this.dbUser != null && !this.dbUser.trim().isEmpty();
        ApplicationLog.info("[POSTGRES] Tentando abrir conexao JDBC. url=%s, hasUserName=%s, user=%s, hasPassword=%s"
                .formatted(DatabaseLogUtil.showJdbcUrlWithCredentials(url), hasUserName, this.dbUser,
                        this.dbPassword != null && !this.dbPassword.isBlank()));
        Connection connection = hasUserName ?
                DbConnectionUtil.getConnection(url, this.dbUser, this.dbPassword) :
                DbConnectionUtil.getConnection(url);
        ApplicationLog.info("[POSTGRES] Conexao JDBC aberta com sucesso. url=%s, autoCommit=%s, catalog=%s, schema=%s"
                .formatted(DatabaseLogUtil.showJdbcUrlWithCredentials(url), connection.getAutoCommit(),
                        connection.getCatalog(), connection.getSchema()));
        return connection;
    }

    private String withPostgreSqlSslMode(String sslMode) {
        String url = removeJdbcQueryParameter(connectionString, "sslmode");
        url = removeJdbcQueryParameter(url, "socketFactory");
        return url + (url.contains("?") ? "&" : "?") + "sslmode=" + sslMode
                + "&socketFactory=br.org.cria.splinkerapp.utils.DirectSocketFactory";
    }

    private String removeJdbcQueryParameter(String url, String parameterName) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return url;
        }

        String baseUrl = url.substring(0, queryStart);
        String query = url.substring(queryStart + 1);
        String filteredQuery = Arrays.stream(query.split("&"))
                .filter(parameter -> !parameter.equals(parameterName)
                        && !parameter.startsWith(parameterName + "="))
                .collect(Collectors.joining("&"));

        return filteredQuery.isEmpty() ? baseUrl : baseUrl + "?" + filteredQuery;
    }
}
