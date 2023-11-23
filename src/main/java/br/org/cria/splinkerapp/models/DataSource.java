package br.org.cria.splinkerapp.models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

public class DataSource {
    private DataSourceType type;
    public DataSourceType getType() {
        return type;
    }
    public void setType(DataSourceType type) {
        this.type = type;
    }

    private Connection connection;
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

    private String dbTableName;
    public String getDbTableName() {
        return dbTableName;
    }
    public void setDbTableName(String dbTableName) {
        this.dbTableName = dbTableName;
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
    public String getDataSourceFilePath() {
        return dataSourceFilePath;
    }
    public void setDataSourceFilePath(String dataSourceFilePath) {
        this.dataSourceFilePath = dataSourceFilePath;
    }

    private LocalDate updatedAt;
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    private LocalDate createdAt = LocalDate.now();
    public LocalDate getCreatedAt() {
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
    public String getDatasetName() {
        return datasetName;
    }
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
    
    private String datasetAcronym;
    public String getDatasetAcronym() {
        return datasetAcronym;
    }
    public void setDatasetAcronym(String datasetAcronym) {
        this.datasetAcronym = datasetAcronym;
    }
    private DataSource(String token, DataSourceType type, String filePath, String host, String databaseName, String tableName,
                     String username, String password, String port, String connectionString, String datasetAcronym, String datasetName)
    {
        this.token = token;
        this.type = type;
        this.dbHost = host;
        this.dbName = databaseName;
        this.type = type;
        this.dbHost = host;
        this.dbName = databaseName;
        this.dbTableName = tableName;
        this.dbUser = username;
        this.dbPassword = password;
        this.dbPort = port;
        this.connectionString = connectionString;
        this.dataSourceFilePath = filePath;
        this.datasetAcronym = datasetAcronym;
        this.datasetName = datasetName;
    }
    
    public boolean isFile() { return this.dataSourceFilePath != null && this.type != DataSourceType.Access; }
    public boolean isAccessDb() { return this.dataSourceFilePath != null && this.type == DataSourceType.Access; }
    public boolean isSQLDatabase() { return this.dataSourceFilePath == null; }
    public static DataSource factory(String token, DataSourceType type, String filePath, String host, 
                                        String databaseName, String tableName,
                                        String username, String password, String port, String datasetAcronym, String datasetName) 
    {
        DataSource ds;
        String connectionString = null;
              
        switch (type) 
        {
            case MySQL:
            case PostgreSQL:
                connectionString = "jdbc:%s://%s/%s?user=%s&password=%s"
                            .formatted(type.name().toLowerCase(),host, databaseName, username, password);
                break;
            case Oracle:
                connectionString = "jdbc:%s:thin:%s/%s@%s:%s:%s"
                            .formatted(type.name().toLowerCase(), username, password, host, port, databaseName);
                break;
            case SQLServer:
                connectionString = "jdbc:%s://%s:%s;encrypt=true;trustServerCertificate=true;databaseName=%s;user=%s;password=%s"
                        .formatted(type.name().toLowerCase(), host, port, databaseName, username, password);
                break;
            case Access:
                connectionString = "jdbc:ucanaccess://%s;memory=false".formatted(filePath);
                break;
            default:
                connectionString = "jdbc:sqlite:splinker.db";
            break;
        }
        ds = new DataSource(token, type, filePath, host, databaseName, tableName, 
                            username, password, port, connectionString, datasetAcronym, datasetName);
        return ds;
    }
    public Connection getDataSourceConnection() throws Exception
    {
        if(this.connection == null)
        {
            var hasUserName = this.dbUser != null && this.dbUser.trim().length() > 0;
            var hasPassword = this.dbPassword != null && this.dbPassword.trim().length() > 0;
            var hasUserNameAndPassword = hasUserName && hasPassword;
            this.connection = hasUserNameAndPassword ? 
                    DriverManager.getConnection(connectionString, this.dbUser, this.dbPassword) : 
                    DriverManager.getConnection(connectionString);
        }
        
        return this.connection;
    }
}
