package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import com.google.common.eventbus.EventBus;
import io.sentry.Sentry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService {
    int rowCount = 0;
    int totalRowCount = 0;
    String zipFile;
    String textFile;
    String columns;
    String rows;
    DataSet ds;
    EventBus writeDataEventBus = EventBusManager.getEvent(EventTypes.WRITE_ROW.name());

    public DataSet getDataSet() {
        return this.ds;
    }

    public int getCurrentRowCount() {
        return this.rowCount;
    }

    public int getTotalRowCount() {
        return totalRowCount;
    }

    public String getTxtFilePath() {
        return this.textFile;
    }

    public DarwinCoreArchiveService(DataSet ds) throws Exception {
        var userDir = System.getProperty("user.dir") + "/" + ds.getId();
        this.ds = ds;
        this.zipFile = "%s/dwca.zip".formatted(userDir);
        this.textFile = "%s/occurrence.txt".formatted(userDir);
        Files.createDirectories(Paths.get(userDir));

    }

    public DarwinCoreArchiveService generateTXTFile() throws Exception {
        var message = "Iniciando a geração do arquivo DWC";
        ApplicationLog.info(message);

        var path = Path.of(this.textFile);
        if (Files.exists(path)) {
            Files.delete(path);
        }
        var writer = new BufferedWriter(new FileWriter(this.textFile));
        writer.write(columns);
        writer.write(rows);
        writer.flush();
        writer.close();
        return this;
    }

    private String getDataSetRows(ResultSet data) throws Exception {
        var dataSourceRows = new StringBuilder();
        var rowCount = 0;
        var baseStr = "%s\t";
        while (data.next()) {
            var resultSetMetaData = data.getMetaData();
            var count = resultSetMetaData.getColumnCount();

            for (int i = 1; i <= count; i++) {
                var value = data.getString(i);
                var isNotNull = value != null && value.toLowerCase() != "null";
                var isNotCRLF = isNotNull && value != "\r" && value != "\t";
                var hasValue = isNotNull && isNotCRLF;
                var content = baseStr.formatted(hasValue ? value : "");
                dataSourceRows.append(content);
            }

            dataSourceRows.append("\n");
            rowCount++;
            writeDataEventBus.post(rowCount);
        }
        totalRowCount = rowCount;
        return dataSourceRows.toString();
    }

    private String getColumnNames(ResultSet data) throws Exception {
        var builder = new StringBuilder();
        var metaData = data.getMetaData();
        int count = metaData.getColumnCount();

        for (int i = 1; i <= count; i++) {
            builder.append("%s\t".formatted(metaData.getColumnName(i)));
        }
        builder.append("\n");
        var columns = builder.toString();
        return columns;
    }

    public DarwinCoreArchiveService generateZIPFile() throws Exception {
        var message = "Iniciando a geração do arquivo ZIP";
        ApplicationLog.info(message);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(this.zipFile))) {
            File fileToZip = new File(this.textFile);
            zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
            Files.copy(fileToZip.toPath(), zipOut);
            return this;
        } catch (Exception e) {
            Sentry.captureException(e);
            throw e;
        }
    }

    public DarwinCoreArchiveService readDataFromSource() throws Exception {
        var message = "Iniciando leitura de dados do banco SQL.";
        ApplicationLog.info(message);

        var loader = ClassLoader.load(ds.getType());
        var command = DataSetService.getSQLCommandFromApi(ds.getToken());
        var conn = ds.getDataSetConnection();
        var statement = conn.createStatement();

        var data = statement.executeQuery(command);
        columns = getColumnNames(data);
        rows = getDataSetRows(data);
        conn.close();
        return this;
    }

    public DarwinCoreArchiveService transferData() throws Exception {
        var message = "Iniciando a transferência do arquivo";
        ApplicationLog.info(message);

        var rSyncConfig = TransferConfigRepository.getRSyncConfig();
        var port = rSyncConfig.getrSyncPort();
        var destination = "%s::%s".formatted(rSyncConfig.getrSyncDestination(), ds.getToken());

        var args = new String[]{"client", "--port=%s".formatted(port), "-r", this.zipFile, destination};
        String[] arrayArgs = Arrays.copyOfRange(args, 1, args.length);
        new YajsyncClient().start(arrayArgs);
        return this;
    }

    public void cleanData() throws Exception {
        dropDataTables();
        deleteSentFiles();
    }

    private void dropDataTables() throws Exception {
        var cmd = """
                SELECT name
                FROM sqlite_master
                WHERE type='table' AND name NOT IN 
                ('DataSetConfiguration', 'CentralServiceConfiguration', 
                'TransferConfiguration','ProxyConfiguration');
                """;
        var connString = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());
        var conn = DriverManager.getConnection(connString);
        var result = conn.createStatement().executeQuery(cmd);
        conn.setAutoCommit(false);
        var statement = conn.createStatement();
        while (result.next()) {
            var tableName = result.getString("name");
            var dropCommand = "DROP TABLE %s;".formatted(tableName);
            statement.addBatch(dropCommand);
        }
        statement.executeBatch();
        conn.commit();
        statement.clearBatch();
        statement.close();
        result.close();
        conn.close();
    }

    private void deleteSentFiles() throws Exception {
        var cmd = "SELECT id FROM DataSetConfiguration;";
        var connString = LocalDbManager.getLocalDbConnectionString();
        var conn = DriverManager.getConnection(connString);
        var result = conn.createStatement().executeQuery(cmd);

        while (result.next()) {
            var datasetId = result.getString("id");
            var userDir = "%s/%s".formatted(System.getProperty("user.dir"), datasetId);
            Files.delete(Path.of("%s/occurrence.txt".formatted(userDir)));
            Files.delete(Path.of("%s/dwca.zip".formatted(userDir)));
           // Files.delete(Path.of("%s.sql".formatted(userDir)));
            Files.delete(Path.of("%s/".formatted(userDir)));
        }
    }

    static void sendFileUsingCommandLine(String[] cmd) throws Exception {
        //Cria o processo
        var proc = Runtime.getRuntime().exec(cmd);
        //Cria o leitor de buffer do resultado do processamento
        var reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        //Cria o leitor de buffer de erro, caso haja algum erro
        var err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String line;
        String errLine;

        //Exibe a saída do processo
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        //Exibe a saída do erro
        while ((errLine = err.readLine()) != null) {
            System.out.println(errLine);
        }
        //Captura código de saída do comando executado na linha de comando.
        var exitCode = proc.waitFor();

        System.out.println("exitcode " + exitCode);
        System.out.println("completed at " + Instant.now().atZone(ZoneId.systemDefault()));
    }
}
