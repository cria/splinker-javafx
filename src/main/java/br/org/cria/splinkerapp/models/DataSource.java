package br.org.cria.splinkerapp.models;

import java.sql.Connection;
import java.sql.DriverManager;

public class DataSource {
    private String connectionString = "jdbc:sqlite:splinker.db";
    private DataSourceType type;
    private Connection connection;

    
    public DataSource(DataSourceType type) { this.type = type; };
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
    
    public Connection getDataSourceConnection() throws Exception
    {
        if(this.connection == null)
        {
            this.connection = DriverManager.getConnection(connectionString);
        }
        
        return this.connection;
    }
}
