package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;
import io.sentry.Sentry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.eventbus.EventBus;

public class DarwinCoreArchiveService
{
    int totalRowCount = 0;
    String zipFile;
    String textFile;
    String columns;
    String rows;
    DataSet ds;
    EventBus writeDataEventBus = EventBusManager.getEvent(EventTypes.WRITE_ROW.name());

    public DataSet getDataSet()
    {
        return this.ds;
    }

    public int getTotalRowCount()
    {
        return totalRowCount;
    }

    public String getTxtFilePath()
    {
        return this.textFile;
    }

    public DarwinCoreArchiveService (DataSet ds) throws Exception
    {
        var userDir = System.getProperty("user.dir") + "/" + ds.getId();
        this.ds = ds;
        this.zipFile = "%s/dwca.zip".formatted(userDir);
        this.textFile = "%s/occurrence.txt".formatted(userDir);
        this.totalRowCount = ds.getLastRowCount();
        Files.createDirectories(Paths.get(userDir));
        
    }
        
    public DarwinCoreArchiveService generateTXTFile() throws Exception
    {   
        var message =  "Iniciando a geração do arquivo DWC";
        ApplicationLog.info(message);

        var path = Path.of(this.textFile);
        if(Files.exists(path))
        {   
            Files.delete(path);
        }
        var writer = new BufferedWriter(new FileWriter(this.textFile));    
        writer.write(columns);        
        writer.write(rows);
        writer.flush();
        writer.close();
        return this;
    }

    private String getDataSetRows(ResultSet data) throws Exception
    {
        var dataSourceRows = new StringBuilder();
        var rowCount = 0;
        var baseStr ="%s\t";
        while (data.next()) 
        {
            var resultSetMetaData = data.getMetaData();
            var count = resultSetMetaData.getColumnCount();
            
            for (int i = 1; i <= count; i++) 
            {
                var value = data.getString(i);
                var hasValue = value != null;
                var isNull = false;
                
                if(hasValue)
                {
                    isNull = value.toLowerCase() == "null";
                }
                
                var content = baseStr.formatted( isNull? "" : value);
                dataSourceRows.append(content);
            }

            dataSourceRows.append("\n");
            rowCount++;
            writeDataEventBus.post(rowCount);
        }
        totalRowCount = rowCount;
        return dataSourceRows.toString();
    }

    private String getColumnNames(ResultSet data) throws Exception
    {
        var builder = new StringBuilder();
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
        var message =  "Iniciando a geração do arquivo ZIP";
        ApplicationLog.info(message);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(this.zipFile))) 
        {
            File fileToZip = new File(this.textFile);
            zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
            Files.copy(fileToZip.toPath(), zipOut);
            return this;
        } 
        catch (Exception e) 
        {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
            throw e;
        }
    }

    public DarwinCoreArchiveService readDataFromSource() throws Exception
    {
        var message =  "Iniciando leitura de dados do banco SQL.";
        ApplicationLog.info(message);

        var loader = ClassLoader.load(ds.getType());
        var command = DataSetService.getSQLCommand(ds.getToken());
        var conn = ds.getDataSetConnection();
        var statement = conn.createStatement();
        
        var data = statement.executeQuery(command);
        columns = getColumnNames(data);
        rows = getDataSetRows(data);
        conn.close();
        return this;
    }
 
    public void transferData() throws Exception 
    {
        var message = "Iniciando a transferência do arquivo";
        ApplicationLog.info(message);

        var userDir = System.getProperty("user.dir");
        var rSyncConfig = TransferConfigRepository.getRSyncConfig();
        var port = rSyncConfig.getrSyncPort();
        var destination = "%s::%s".formatted(rSyncConfig.getrSyncDestination(), ds.getToken());
        var command = new String[] {"java", "-jar", "%s/libs/yajsync-app-0.9.0-SNAPSHOT-full.jar".formatted(userDir), 
                                    "client", "--port=%s".formatted(port), "-r", this.zipFile, destination };
        sendFileUsingCommandLine(command);
    }

    static void sendFileUsingCommandLine(String[] cmd) throws Exception
    {
        
        //Cria o processo
        var proc = Runtime.getRuntime().exec(cmd);
        //Cria o leitor de buffer do resultado do processamento
        var reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        //Cria o leitor de buffer de erro, caso haja algum erro
        var err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String line;
        String errLine;
        
        //Exibe a saída do processo
        while ((line = reader.readLine()) != null) 
        {
            System.out.println(line);
        }
        
        //Exibe a saída do erro
        while ((errLine = err.readLine()) != null) 
        {
            System.out.println(errLine);
        }
        //Captura código de saída do comando executado na linha de comando.
        var exitCode = proc.waitFor();

        System.out.println("exitcode " + exitCode);
        System.out.println("completed at " + Instant.now().atZone(ZoneId.systemDefault()));
    }  
}
