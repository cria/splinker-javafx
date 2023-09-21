package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.services.interfaces.IDarwinCoreArchiveService;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import com.google.gson.Gson;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService implements IDarwinCoreArchiveService {
    String zipFile = "%s/spLinker_dwca.zip".formatted(System.getProperty("user.dir"));
    String textFile = "%s/spLinker_dwca.txt".formatted(System.getProperty("user.dir"));
    ResultSet data;

    @Override
    public DarwinCoreArchiveService generateTXTFile() throws IOException, SQLException 
    {   
        var path = Path.of(textFile);
        if (Files.exists(path)) 
        {
            Files.delete(path);
        }
        
        var columnNames = getColumnNames();
        var rows = getDataSourceRows();
        var writer = new BufferedWriter(new FileWriter(textFile));
        writer.write(columnNames);        
        writer.write(rows);
        writer.flush();
        writer.close();

        return this;
    }

    private String getDataSourceRows() throws SQLException
    {
        var dataSourceRows = new StringBuilder();
        while (data.next()) 
        {
            var resultSetMetaData = data.getMetaData();
            var count = resultSetMetaData.getColumnCount();
            for (int i = 0; i <= count; i++) 
            {
                var content = "%s;".formatted(data.getString(i));
                dataSourceRows.append(content);
            }
    
             dataSourceRows.append("\n");
        }
        return dataSourceRows.toString();
    }

    private String getColumnNames() throws SQLException
    {
        var builder = new StringBuilder();
        var metaData = data.getMetaData();
        int count = metaData.getColumnCount();
        
        for(int i = 0; i<=count; i++) 
        {
           builder.append("%s;".formatted(metaData.getColumnName(i)));
        }
        builder.append("\n");
        
        return builder.toString();
    }

    @Override
    public DarwinCoreArchiveService generateZIPFile() 
    {

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) 
        {
            File fileToZip = new File(textFile);
            zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
            Files.copy(fileToZip.toPath(), zipOut);
            return this;
        } 
        catch (Exception e) 
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DarwinCoreArchiveService readDataFromSource(DataSource source)throws SQLException, IOException, URISyntaxException
    {
        var command = getQueryCommandFromAPI();
        var uri = source.getConnectionString();
        var conn = DriverManager.getConnection(uri);
        var statement = conn.createStatement();
        this.data = statement.executeQuery(command);
        
        return this;
    }

    @Override
    public Service<Void> transferData() 
    {
        // TODO: Ler map.dat e db.dat das coleções.
        // Esses arquivos são combinados para montar o select dos dados
        // nas fontes de dados das coleções (DB, planilhas, Brahms, etc)
        // O que não for planilha, é banco sempre
        var port = ConfigurationData.getRSyncPort();
        // var source =
        // "%s/splinker_dwca.zip".formatted(System.getProperty("user.dir"));
        var destination = ConfigurationData.getTransferDataDestination();
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

    private String getQueryCommandFromAPI() throws IOException, URISyntaxException 
    {
        String line;
        var command = "";
        var urlConn = new URI("http://localhost:8000/api/get_query").toURL();
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
        command = new String(out.toByteArray(), StandardCharsets.UTF_8);
        return command;
    }
}
