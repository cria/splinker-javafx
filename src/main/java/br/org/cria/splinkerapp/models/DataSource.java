package br.org.cria.splinkerapp.models;

public class DataSource {
    private String connectionString = "jdbc:sqlite:splinker.db";
    private DataSourceType type;
    

    public DataSource(){};
    public DataSource(DataSourceType type) { this.type = type; };
    
    public DataSource(DataSourceType type, String connectionString) 
    { this.type = type; this.connectionString = connectionString; };
    
    
    public DataSourceType getType() 
    {
        return type;
    }

    public String getConnectionString() {
        return connectionString;
    }

}
