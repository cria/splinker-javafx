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
        var dataSource = new DataSource(type, host, databaseName, tableName, username, password, port);
        return extractAndSend(dataSource);
    }

     public static Service<Void> processData(String microsoftAccessFilePath, String userName, String password ) throws Exception
    {
        
        var hasUserNameAndPassword = userName != null && password !=null;
        var connString = "jdbc:ucanaccess://%s;memory=false".formatted(microsoftAccessFilePath);
        var conn =  hasUserNameAndPassword? 
                    DriverManager.getConnection(connString,userName, password) : 
                    DriverManager.getConnection(connString);
        var dataSource = new DataSource(DataSourceType.Access, conn);
        
        return extractAndSend(dataSource);   
    }

    private static Service<Void> extractAndSend(DataSource source) throws Exception
    {
          return new DarwinCoreArchiveService()
            .readDataFromSource(source)
            .generateTXTFile()
            .generateZIPFile()
            .transferData();
    }
}