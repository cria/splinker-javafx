package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;

import com.github.perlundq.yajsync.ui.YajsyncClient;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService
{
    String zipFile = "%s/%s.zip";
    String textFile = "%s/occurences.txt".formatted(System.getProperty("user.dir"));
    ResultSet data;

    
    public DarwinCoreArchiveService generateTXTFile() throws Exception
    {   
        var path = Path.of(textFile);
        var columnNames = getColumnNames();
        var rows = getDataSourceRows();
        var rowCount = rows.length();
        if(Files.exists(path))
        {
            var dataSourceLineCount = rowCount + 1;
            var fileLineCount = Files.lines(path).count();
            if(rowCount > 0 && fileLineCount > dataSourceLineCount)
            {
                    throw new Exception("Não é possível apagar registros!");
            }
            else
            {
                Files.delete(path);
            }
        }
                var writer = new BufferedWriter(new FileWriter(textFile));
                writer.write(columnNames);        
                writer.write(rows);
                writer.flush();
                writer.close();
            
        return this;
    }

    private String getDataSourceRows() throws Exception
    {
        var dataSourceRows = new StringBuilder();
        while (data.next()) 
        {
            var resultSetMetaData = data.getMetaData();
            var count = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= count; i++) 
            {
                var content = "%s\t".formatted(data.getString(i));
                dataSourceRows.append(content);
            }
    
             dataSourceRows.append("\n");
        }
        return dataSourceRows.toString();
    }

    private String getColumnNames() throws Exception
    {
        var builder = new StringBuilder("\n");
        var metaData = data.getMetaData();
        int count = metaData.getColumnCount();
        
        for(int i = 1; i<=count; i++) 
        {
           builder.append("%s\t".formatted(metaData.getColumnName(i)));
        }
        builder.append("\n");
        var columns = builder.toString();
        return columns;
    }

    public DarwinCoreArchiveService generateZIPFile() throws Exception 
    {
        String token = TokenRepository.getToken();
        zipFile = zipFile.formatted(System.getProperty("user.dir"), token);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) 
        {
            File fileToZip = new File(textFile);
            zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
            Files.copy(fileToZip.toPath(), zipOut);
            return this;
        } 
        catch (Exception e) 
        {
            throw e;
        }
    }

    public DarwinCoreArchiveService readDataFromSource(DataSource source) throws Exception
    {
        var command = DataSourceRepository.getSQLCommand();
        var conn = source.getDataSourceConnection();
        var statement = conn.createStatement();
        this.data = statement.executeQuery(command);
        
        return this;
    }
    public DarwinCoreArchiveService readDataFromSource() throws Exception
    {
        DataSource source = DataSourceRepository.getDataSource();
        return readDataFromSource(source);
    }

    public Service<Void> transferData() throws Exception 
    {
        var rSyncConfig = TransferConfigRepository.getRSyncConfig();
        var port = rSyncConfig.getrSyncPort();
        
        var destination = rSyncConfig.getrSyncDestination();
        var command = new String[] { "--port=%s".formatted(port), "-r", this.zipFile, destination };
        var client = new YajsyncClient();
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask() 
            {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception 
                    {
                        var session = client.start(command);
                        return null;
                    }
                };
            }
        };

    }
}
