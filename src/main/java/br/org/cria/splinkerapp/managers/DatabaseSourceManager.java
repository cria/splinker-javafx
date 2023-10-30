package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;

public class DatabaseSourceManager 
{

    public static Service<Void> processData(DataSourceType type, String host, String databaseName, String tableName, String username, String password, String port ) throws Exception
    {   
        var dataSource = DataSource.factory(type,null,host,databaseName,tableName,username,password,port);
        return extractAndSend(dataSource);
    }

     public static Service<Void> processData(String microsoftAccessFilePath, String userName, String password ) throws Exception
    {
        var dataSource = DataSource.factory(DataSourceType.Access, microsoftAccessFilePath, null, null, null, userName, password, null);
        
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