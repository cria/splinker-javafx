package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.parsers.FileParser;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;
import br.org.cria.splinkerapp.utils.DbConnectionUtil;
import br.org.cria.splinkerapp.utils.SystemConfigurationUtil;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import com.google.common.eventbus.EventBus;
import com.google.gson.GsonBuilder;
import io.sentry.Sentry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService {
    int rowCount = 0;
    int totalRowCount = 0;
    String zipFile;
    String textFile;
    String userDir;
    DataSet ds;
    EventBus writeDataEventBus = EventBusManager.getEvent(EventTypes.WRITE_ROW.name());

    private record CsvWriteResult(List<String> columns, int rowCount) {
    }

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
        userDir = System.getProperty("user.dir") + "/" + ds.getId();
        this.ds = ds;
        this.zipFile = "%s/dwca.zip".formatted(userDir);
        this.textFile = "%s/occurrence.txt".formatted(userDir);
        Files.createDirectories(Paths.get(userDir));
        cleanWorkingDirectory();
    }

    private void cleanWorkingDirectory() throws Exception {
        try (var files = Files.list(Path.of(userDir))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private void getDataSetRows(ResultSet data) throws Exception {
        rowCount = 0;
        boolean firstRow = true;
        try (var writer = new BufferedWriter(new FileWriter(this.textFile, true))) {
            while (data.next()) {
                var baseStr = "%s\t";
                if (!firstRow) {
                    writer.write("\n");
                } else {
                    firstRow = false;
                }

                var resultSetMetaData = data.getMetaData();
                var count = resultSetMetaData.getColumnCount();

                for (int i = 1; i <= count; i++) {
                    var value = data.getString(i);
                    value = FileParser.getCellValue(value);
                    var isNotNull = value != null && !value.equalsIgnoreCase("null");
                    var isNotCRLF = isNotNull && !value.equals("\r") && !value.equals("\t");
                    var hasValue = isNotNull && isNotCRLF;
                    if (i == count) {
                        baseStr = "%s";
                    }
                    var content = baseStr.formatted(hasValue ? value : "");
                    writer.write(content);
                }

                rowCount++;
                writeDataEventBus.post(rowCount);
            }
            totalRowCount = rowCount;
        }
    }

    private void getColumnNames(ResultSet data) throws Exception {
        var builder = new StringBuilder();
        var metaData = data.getMetaData();
        int count = metaData.getColumnCount();
        var baseStr = "%s\t";

        for (int i = 1; i <= count; i++) {
            if (i == count) {
                baseStr = "%s";
            }
            builder.append(baseStr.formatted(metaData.getColumnLabel(i)));
        }
        builder.append("\n");
        var columns = builder.toString();

        try (var writer = new BufferedWriter(new FileWriter(this.textFile, true))) {
            writer.write(columns);
        }
    }

    public DarwinCoreArchiveService generateZIPFile() throws Exception {
        var message = "Iniciando a geracao do arquivo ZIP";
        ApplicationLog.info(message);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(this.zipFile));
             var files = Files.list(Path.of(userDir))) {
            var zipFileName = Path.of(this.zipFile).getFileName().toString();
            var filesToZip = files
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals(zipFileName))
                    .toList();

            for (Path fileToZip : filesToZip) {
                zipOut.putNextEntry(new ZipEntry(fileToZip.getFileName().toString()));
                Files.copy(fileToZip, zipOut);
                zipOut.closeEntry();
            }

            return this;
        } catch (Exception e) {
            Sentry.captureException(e);
            throw e;
        }
    }

    public DarwinCoreArchiveService readDataFromSource() throws Exception {
        var message = "Iniciando leitura de dados do banco SQL.";
        ApplicationLog.info(message);

        var command = DataSetService.getSQLCommandFromApi(ds.getToken());
        if (command == null || command.isBlank()) {
            return readDwcDpDataFromSource();
        }

        try (var conn = ds.getDataSetConnection();
             var statement = conn.createStatement();
             var data = statement.executeQuery(command)) {
            getColumnNames(data);
            getDataSetRows(data);
        }
        return this;
    }

    private DarwinCoreArchiveService readDwcDpDataFromSource() throws Exception {
        var resources = DataSetService.getDwcDpFromApi(ds.getToken());
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("A API nao retornou sql_command nem dwc_dp valido para a colecao.");
        }

        this.zipFile = "%s/dwc-dp.zip".formatted(userDir);
        this.textFile = "%s/occurrence.csv".formatted(userDir);
        rowCount = 0;
        totalRowCount = 0;

        var datapackageResources = new ArrayList<Map<String, Object>>();
        boolean hasOccurrenceResource = false;

        try (var conn = ds.getDataSetConnection();
             var statement = conn.createStatement()) {
            for (var resource : resources) {
                var name = resource.get("name").toString();
                var sql = resource.get("sql").toString();
                var csvFile = Path.of(userDir, "%s.csv".formatted(name));

                try (var data = statement.executeQuery(sql);
                     var writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
                    var csvResult = writeCsv(data, writer);
                    if (name.equalsIgnoreCase("occurrence")) {
                        totalRowCount = csvResult.rowCount();
                        this.textFile = csvFile.toString();
                        hasOccurrenceResource = true;
                    }
                    datapackageResources.add(buildDatapackageResource(resource, csvResult.columns()));
                }
            }
        }

        if (!hasOccurrenceResource) {
            totalRowCount = rowCount;
        }
        writeDatapackage(datapackageResources);
        return this;
    }

    private CsvWriteResult writeCsv(ResultSet data, Writer writer) throws Exception {
        var metaData = data.getMetaData();
        int columnCount = metaData.getColumnCount();
        var columns = new ArrayList<String>();
        int resourceRowCount = 0;

        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        writer.write(toCsvLine(columns));

        while (data.next()) {
            var row = new ArrayList<String>();
            for (int i = 1; i <= columnCount; i++) {
                var value = FileParser.getCellValue(data.getString(i));
                row.add(value == null || value.equalsIgnoreCase("null") ? "" : value);
            }
            writer.write("\n");
            writer.write(toCsvLine(row));
            rowCount++;
            resourceRowCount++;
            writeDataEventBus.post(rowCount);
        }

        return new CsvWriteResult(columns, resourceRowCount);
    }

    private String toCsvLine(List<String> values) {
        return values.stream()
                .map(this::escapeCsv)
                .collect(Collectors.joining(","));
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        var escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"%s\"".formatted(escaped);
        }

        return escaped;
    }

    private Map<String, Object> buildDatapackageResource(Map<String, Object> dwcDpResource, List<String> columns) {
        var name = dwcDpResource.get("name").toString();
        var schema = new LinkedHashMap<String, Object>();
        var fields = columns.stream()
                .map(this::buildDatapackageField)
                .toList();
        schema.put("fields", fields);

        if (dwcDpResource.containsKey("primaryKey")) {
            schema.put("primaryKey", dwcDpResource.get("primaryKey"));
        }

        if (dwcDpResource.containsKey("foreignKeys")) {
            schema.put("foreignKeys", dwcDpResource.get("foreignKeys"));
        }

        var resource = new LinkedHashMap<String, Object>();
        resource.put("name", name);
        resource.put("path", "%s.csv".formatted(name));
        resource.put("profile", "tabular-data-resource");
        resource.put("format", "csv");
        resource.put("mediatype", "text/csv");
        resource.put("schema", schema);

        return resource;
    }

    private Map<String, Object> buildDatapackageField(String column) {
        var title = toTitle(column);
        var field = new LinkedHashMap<String, Object>();
        field.put("name", column);
        field.put("title", title);
        field.put("description", title);
        field.put("type", "string");
        field.put("format", "default");
        field.put("dcterms:isVersionOf", "http://rs.tdwg.org/dwc/terms/%s".formatted(column));
        return field;
    }

    private String toTitle(String value) {
        var words = value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace("_", " ")
                .trim()
                .split("\\s+");

        return Arrays.stream(words)
                .filter(word -> !word.isBlank())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private void writeDatapackage(List<Map<String, Object>> resources) throws Exception {
        var datapackage = new LinkedHashMap<String, Object>();
        datapackage.put("profile", "http://rs.tdwg.org/dwc-dp/1.0/dwc-dp-profile.json");
        datapackage.put("name", "splinker-export");
        datapackage.put("created", OffsetDateTime.now().toString());
        datapackage.put("version", "1.0");
        datapackage.put("resources", resources);

        var gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(Path.of(userDir, "datapackage.json"), gson.toJson(datapackage), StandardCharsets.UTF_8);
    }

    public DarwinCoreArchiveService transferData() throws Exception {
        var message = "Iniciando a transferencia do arquivo";
        ApplicationLog.info(message);

        var rSyncConfig = TransferConfigRepository.getRSyncConfig();
        var port = rSyncConfig.getrSyncPort();
        var destination = "%s::%s".formatted(rSyncConfig.getrSyncDestination(), ds.getToken());

        var args = new String[] {"client", "--port=%s".formatted(port), "-r", this.zipFile, destination };
        /*String[] arrayArgs = Arrays.copyOfRange(args, 1, args.length);
        String logsYajsysc = new YajsyncClient().startSplink(arrayArgs);
        ApplicationLog.info(logsYajsysc);
        if (logsYajsysc.contains("socket open/close error")) {
            throw new IllegalArgumentException("O sistema de recebimento de dados do CRIA esta indisponivel. Entre em contato com o suporte.");
        } else if (logsYajsysc.contains("Error") || logsYajsysc.contains("ERROR")) {
            throw new IllegalArgumentException(logsYajsysc);
        }*/
        return this;
    }

    public void cleanData() throws Exception {
        if (!SystemConfigurationUtil.runInDevelopment()) {
            dropDataTables();
            deleteSentFiles();
        }
    }

    private void dropDataTables() throws Exception {
        var cmd = """
                SELECT name
                FROM sqlite_master
                WHERE type='table' AND name NOT IN
                ('DataSetConfiguration', 'CentralServiceConfiguration',
                'TransferConfiguration','ProxyConfiguration','TransferHistoryDataSet', 'EmailConfiguration');
                """;
        var connString = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());
        try (var conn = DbConnectionUtil.getConnection(connString);
             var selectStatement = conn.createStatement();
             var result = selectStatement.executeQuery(cmd);
             var statement = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                while (result.next()) {
                    var tableName = result.getString("name");
                    var dropCommand = "DROP TABLE %s;".formatted(tableName);
                    statement.addBatch(dropCommand);
                }
                statement.executeBatch();
                conn.commit();
                statement.clearBatch();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void deleteSentFiles() throws Exception {
        var token = TokenRepository.getCurrentToken();
        var cmd = "SELECT id FROM DataSetConfiguration WHERE token = '" + token + "';";
        var connString = LocalDbManager.getLocalDbConnectionString();
        try (var conn = DbConnectionUtil.getConnection(connString);
             var statement = conn.prepareStatement(cmd);
             var result = statement.executeQuery()) {
            while (result.next()) {
                var datasetId = result.getString("id");
                var path = Path.of("%s/%s".formatted(System.getProperty("user.dir"), datasetId));
                if (!Files.exists(path)) {
                    continue;
                }

                try (var files = Files.walk(path)) {
                    files.sorted(Comparator.reverseOrder())
                            .forEach(file -> {
                                try {
                                    Files.deleteIfExists(file);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
            }
        }
    }

    static void sendFileUsingCommandLine(String[] cmd) throws Exception {
        var proc = Runtime.getRuntime().exec(cmd);
        var reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        var err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String line;
        String errLine;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        while ((errLine = err.readLine()) != null) {
            System.out.println(errLine);
        }

        var exitCode = proc.waitFor();

        System.out.println("exitcode " + exitCode);
        System.out.println("completed at " + Instant.now().atZone(ZoneId.systemDefault()));
    }
}
