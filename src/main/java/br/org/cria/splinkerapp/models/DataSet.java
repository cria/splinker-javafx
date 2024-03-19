package br.org.cria.splinkerapp.models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;

import br.org.cria.splinkerapp.managers.LocalDbManager;

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
    private DataSet(String token, DataSourceType type, String filePath, String host, String databaseName,
                     String username, String password, String port, String connectionString, String datasetAcronym, 
                     String datasetName, int lastRowCount, int id, LocalDateTime updatedAt)
    {
        this.id = id;
        this.token = token;
        this.type = type;
        this.dbHost = host;
        this.dbName = databaseName;
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
    
    public boolean isFile() 
    { 
        return this.type.equals(DataSourceType.CSV) || 
        this.type.equals(DataSourceType.Excel) || 
        this.type.equals(DataSourceType.LibreOfficeCalc) || 
        this.type.equals(DataSourceType.Numbers) || 
        this.type.equals(DataSourceType.dBase) ||
        this.type.equals(DataSourceType.LibreOfficeCalc); 
    }
    
    public boolean isAccessDb() { return this.type == DataSourceType.Access; }
    public boolean isSQLDatabase() 
    { 
        return this.type.equals(DataSourceType.MySQL) || 
        this.type.equals(DataSourceType.Oracle) || 
        this.type.equals(DataSourceType.PostgreSQL) || 
        this.type.equals(DataSourceType.SQLServer);
    }
    public static DataSet factory(String token, DataSourceType type, String filePath, String host, 
                                        String databaseName, String username, String password, 
                                        String port, String datasetAcronym, String datasetName, 
                                        int lastRowCount, int id, LocalDateTime updated_at) 
    {
        DataSet ds;
        String connectionString = null;
              
        if(type != null)
        {
            switch (type) 
            {
                case MySQL:
                connectionString = "jdbc:%s://%s/%s?user=%s&password=%s"//&allowPublicKeyRetrieval=true&useSSL=false"
                                .formatted(type.name().toLowerCase(),host, databaseName, username, password);
                    break;
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
                default:
                    connectionString = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());
                break;
            } 
        }
        ds = new DataSet(token, type, filePath, host, databaseName,
                        username, password, port, connectionString, datasetAcronym, 
                        datasetName, lastRowCount, id, updated_at);
        return ds;
    }
    public Connection getDataSetConnection() throws Exception
    {
        if(this.connection == null)
        {
            var hasUserName = this.dbUser != null && this.dbUser.trim().length() > 0;
            var passwordIsNull = this.dbPassword != null;
            var hasPassword = this.isAccessDb() ? passwordIsNull && this.dbPassword.trim().length() > 0 
                             : true;
            
            var hasUserNameAndPassword = hasUserName && hasPassword;
            this.connection = hasUserNameAndPassword ? 
                    DriverManager.getConnection(connectionString, this.dbUser, this.dbPassword) : 
                    DriverManager.getConnection(connectionString);
        }
        
        return this.connection;
    }
}
