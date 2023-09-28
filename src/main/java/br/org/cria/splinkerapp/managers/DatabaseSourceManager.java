package br.org.cria.splinkerapp.managers;

import java.sql.DriverManager;
import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class DatabaseSourceManager 
{

    public static Service<Void> processData(DataSourceType type, String host, String databaseName, String tableName, String username, String password, String port ) throws Exception
    {   
        String connString = null; 
        var dataSource = new DataSource(type, port);
        switch (type) {
            case MySQL:
            case PostgreSQL:
                connString = "jdbc:%s://%s/%s".formatted(type.name().toLowerCase(),host, databaseName);
                break;
            case Oracle:
                break;
            case SQLServer:
                break;
            default:
        }
        
       var dwcManager = new DarwinCoreArchiveService();
        dwcManager.readDataFromSource(new DataSource(type, connString))
        .generateTXTFile()
        .generateZIPFile();
        return dwcManager.transferData();
    }

     public static Service<Void> processData(String microsoftAccessFilePath, String userName, String password ) throws Exception
    {
        var connString = "";
        var dataSource = new DataSource(DataSourceType.Access, connString);
        return new Service<Void>() 
        {
            @Override
            protected Task<Void> createTask()
            {
                return new Task<Void>() 
                {
                    @Override
                    protected Void call() throws Exception
                    {   var hasUserNameAndPassword = userName != null && password !=null;

                        var connString = "jdbc:ucanaccess://%s".formatted(microsoftAccessFilePath);
                        var conn =  hasUserNameAndPassword? 
                                    DriverManager.getConnection(connString,userName, password) : 
                                    DriverManager.getConnection(connString);
                        return null;
                        
                    }
                };
            }
        };
    }
}