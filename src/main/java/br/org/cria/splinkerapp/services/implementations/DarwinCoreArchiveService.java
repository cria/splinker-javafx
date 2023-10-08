package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import com.google.gson.Gson;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService 
{
    String zipFile = "%s/%s.zip";
    String textFile = "%s/occurences.txt".formatted(System.getProperty("user.dir"));
    ResultSet data;

    
    public DarwinCoreArchiveService generateTXTFile() throws Exception
    {   
        var columnNames = getColumnNames();
        var rows = getDataSourceRows();
        var rowCount = rows.length();
        if( rowCount > 0)
        {
            var path = Path.of(textFile);
            if(Files.lines(path).count() > rowCount+1)
            {
                if (Files.exists(path)) 
                {
                    Files.delete(path);
                }
                var writer = new BufferedWriter(new FileWriter(textFile));
                writer.write(columnNames);        
                writer.write(rows);
                writer.flush();
                writer.close();
            }
            else
            {
                //TODO: AVISAR O USUÁRIO QUE O ARQUIVO ATUAL TEM MAIS LINHAS QUE O ANTERIOR
            }
            
            
            
            
        }
        

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
        
        return builder.toString();
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
        var command = getQueryCommandFromAPI();
        // var uri = source.getConnectionString();
        // var conn = DriverManager.getConnection(uri);
        var conn = source.getDataSourceConnection();
        var statement = conn.createStatement();
        this.data = statement.executeQuery(command);
        
        return this;
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

    private String getQueryCommandFromAPI() throws Exception 
    {
        String line;
        var token = TokenRepository.getToken();
        var urlConn = new URI("http://localhost:8000/api/get_query?token="+token).toURL();
        var response = new StringBuffer();
        var connection = (HttpURLConnection) urlConn.openConnection();
        connection.setRequestMethod("GET");
        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        while ((line = reader.readLine()) != null) 
        {
            response.append(line);
        }
        
        reader.close();
        connection.disconnect();

        HashMap<String, List<Double>> json = new Gson().fromJson(response.toString(), HashMap.class);

        var strValues = json.get("cmd");
        var out = new ByteArrayOutputStream();
        strValues.forEach((e) -> out.write(e.byteValue()));
        var command = new String(out.toByteArray(), StandardCharsets.UTF_8);
        return command;
    }
}
