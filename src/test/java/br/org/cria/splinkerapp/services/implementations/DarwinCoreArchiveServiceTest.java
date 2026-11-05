package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.*;
import br.org.cria.splinkerapp.repositories.*;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.utils.SystemConfigurationUtil;
import org.junit.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DarwinCoreArchiveServiceTest extends RepositoryTestBase {
    private DataSet source() throws Exception {
        var ds = spy(DataSetTest.source(DataSourceType.CSV, null, null));
        doAnswer(i -> DriverManager.getConnection(url)).when(ds).getDataSetConnection(); return ds;
    }
    @Test public void exportsSqlResultAndZipsOnlyRegularOutputFiles() throws Exception {
        sql("CREATE TABLE records (name TEXT, place TEXT); INSERT INTO records VALUES ('Ana', 'Brasil'), ('Bia', NULL)");
        try (var api = mockStatic(DataSetService.class)) {
            api.when(() -> DataSetService.getSQLCommandFromApi("test-token")).thenReturn("SELECT * FROM records");
            var ds = source(); var service = new DarwinCoreArchiveService(ds);
            assertSame(ds, service.getDataSet()); assertSame(service, service.readDataFromSource());
            assertEquals("name\tplace\nAna\tBrasil\nBia\t", Files.readString(Path.of(service.getTxtFilePath())));
            assertEquals(2, service.getCurrentRowCount()); assertEquals(2, service.getTotalRowCount());
            Files.createDirectory(Path.of(service.userDir, "ignored")); assertSame(service, service.generateZIPFile());
            try (var zip = new ZipFile(service.zipFile)) { assertEquals(1, zip.size()); assertNotNull(zip.getEntry("occurrence.txt")); }
            Files.writeString(Path.of(service.userDir, "stale"), "old"); new DarwinCoreArchiveService(ds);
            assertFalse(Files.exists(Path.of(service.userDir, "stale")));
        }
    }
    @Test public void exportsDwcDpWithCsvEscapingAndRelationalSchema() throws Exception {
        sql("CREATE TABLE records (occurrenceID TEXT, scientific_name TEXT); INSERT INTO records VALUES ('a,b', 'A \"B\"'), ('c', NULL)");
        try (var api = mockStatic(DataSetService.class)) {
            var resource = new LinkedHashMap<String, Object>(); resource.put("name", "occurrence"); resource.put("sql", "SELECT * FROM records");
            resource.put("primaryKey", "occurrenceID"); resource.put("foreignKeys", List.of());
            api.when(() -> DataSetService.getDwcDpFromApi("test-token")).thenReturn(List.of(resource, Map.of("name", "event", "sql", "SELECT * FROM records")));
            var service = new DarwinCoreArchiveService(source()).readDataFromSource();
            assertEquals("occurrenceID,scientific_name\n\"a,b\",\"A \"\"B\"\"\"\nc,", Files.readString(Path.of(service.getTxtFilePath())));
            assertEquals(4, service.getCurrentRowCount()); assertEquals(2, service.getTotalRowCount());
            var json = com.google.gson.JsonParser.parseString(Files.readString(Path.of(service.userDir, "datapackage.json"))).getAsJsonObject();
            var schema = json.getAsJsonArray("resources").get(0).getAsJsonObject().getAsJsonObject("schema");
            assertEquals("occurrenceID", schema.get("primaryKey").getAsString()); assertTrue(schema.getAsJsonArray("foreignKeys").isEmpty());
            assertEquals("Scientific Name", schema.getAsJsonArray("fields").get(1).getAsJsonObject().get("title").getAsString());
            service.generateZIPFile(); try (var zip = new ZipFile(service.zipFile)) { assertEquals(3, zip.size()); }
        }
    }
    @Test public void countsOtherResourcesAndRejectsAbsentMapping() throws Exception {
        try (var api = mockStatic(DataSetService.class)) {
            api.when(() -> DataSetService.getSQLCommandFromApi("test-token")).thenReturn(" ");
            api.when(() -> DataSetService.getDwcDpFromApi("test-token")).thenReturn(List.of(Map.of("name", "event", "sql", "SELECT 'e' AS eventID")));
            var service = new DarwinCoreArchiveService(source()).readDataFromSource(); assertEquals(1, service.getTotalRowCount());
            api.when(() -> DataSetService.getDwcDpFromApi("test-token")).thenReturn(List.of());
            assertThrows(IllegalArgumentException.class, service::readDataFromSource);
        }
    }
    @Test public void cleansOnlyImportTablesAndSentFilesInProduction() throws Exception {
        sql("CREATE TABLE imported (id INT); INSERT INTO DataSetConfiguration (id, token, datasource_type) VALUES (123, 'test-token', 'CSV')");
        try (var local = mockStatic(LocalDbManager.class); var mode = mockStatic(SystemConfigurationUtil.class)) {
            local.when(LocalDbManager::getLocalDbConnectionString).thenReturn(url); local.when(LocalDbManager::getDbFilePath).thenReturn("unused-local.db"); TokenRepository.setCurrentToken("test-token");
            var previous = System.getProperty("splinker.connection"); System.setProperty("splinker.connection", url);
            try {
                var service = new DarwinCoreArchiveService(source()); Files.writeString(Path.of(service.userDir, "sent"), "data");
                mode.when(SystemConfigurationUtil::runInDevelopment).thenReturn(true); service.cleanData(); assertTrue(Files.exists(Path.of(service.userDir)));
                mode.when(SystemConfigurationUtil::runInDevelopment).thenReturn(false); service.cleanData(); assertFalse(Files.exists(Path.of(service.userDir)));
                try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
                    assertThrows(SQLException.class, () -> s.executeQuery("SELECT * FROM imported"));
                    try (var r = s.executeQuery("SELECT COUNT(*) FROM DataSetConfiguration")) { assertTrue(r.next()); assertEquals(1, r.getInt(1)); }
                }
                service.cleanData(); // An already deleted working directory is harmless.
            } finally { if (previous == null) System.clearProperty("splinker.connection"); else System.setProperty("splinker.connection", previous); }
        }
    }
    @Test public void resolvesTransferDestinationWithoutContactingServer() throws Exception {
        try (var config = mockStatic(TransferConfigRepository.class)) {
            config.when(TransferConfigRepository::getRSyncConfig).thenReturn(new RSyncConfig(873, "rsync.invalid"));
            var service = new DarwinCoreArchiveService(source()); assertSame(service, service.transferData());
        }
    }
}
