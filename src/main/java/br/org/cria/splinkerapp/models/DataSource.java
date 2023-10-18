package br.org.cria.splinkerapp.models;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

public class DataSource {
    private DataSourceType type;
    private Connection connection;
    private String connectionString = "jdbc:sqlite:splinker.db";
    private String dataSourceFilePath;
    public String getDataSourceFilePath() {
        return dataSourceFilePath;
    }
    public void setDataSourceFilePath(String dataSourceFilePath) {
        this.dataSourceFilePath = dataSourceFilePath;
    }

    private String dbHost;
    public String getDbHost() {
        return dbHost;
    }
    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    private String dbPort;
    public String getDbPort() {
        return dbPort;
    }
    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
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

    private LocalDate createdAt;
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    private LocalDate updatedAt;
  
    private Path filePath;
    public Path getFilePath() {
        return filePath;
    }
    public DataSource(DataSourceType type) { this.type = type; };
    public DataSource(DataSourceType type, Path filepath) { this.type = type; this.filePath = filepath; };
    public DataSource(DataSourceType type, Connection conn) 
    { this.type = type; this.connection = conn; };
    public DataSource(DataSourceType type, String connectionString) 
    { this.type = type; this.connectionString = connectionString; };
    
    public DataSource(DataSourceType type, String host, String databaseName, String tableName,
                     String username, String password, String port )
    {
        this.type = type;
        switch (type) 
        {
            case MySQL:
            case PostgreSQL:
                this.connectionString = "jdbc:%s://%s/%s?user=%s&password=%s"
                            .formatted(type.name().toLowerCase(),host, databaseName, username, password);
                break;
            case Oracle:
                this.connectionString = "jdbc:%s:thin:%s/%s@%s:%s:%s"
                            .formatted(type.name().toLowerCase(), username, password, host, port, databaseName);
                break;
            case SQLServer:
                this.connectionString = "jdbc:%s://%s:%s;encrypt=true;trustServerCertificate=true;databaseName=%s;user=%s;password=%s"
                        .formatted(type.name().toLowerCase(), host, port, databaseName, username, password);
                break;
            default:
            break;
        }
    }
    public DataSourceType getType() 
    {
        return type;
    }

    public String getConnectionString() {
        return connectionString;
    }
    public boolean isFile() { return this.filePath == null;}
    
    public Connection getDataSourceConnection() throws Exception
    {
        if(this.connection == null)
        {
            this.connection = DriverManager.getConnection(connectionString);
        }
        
        return this.connection;
    }
}
