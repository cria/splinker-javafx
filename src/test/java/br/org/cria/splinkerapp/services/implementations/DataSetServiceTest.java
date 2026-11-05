package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.*;
import br.org.cria.splinkerapp.repositories.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DataSetServiceTest extends RepositoryTestBase {
    private static List<Double> encoded(String value) {
        var result = new ArrayList<Double>(); for (byte b : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) result.add((double)(b & 255)); return result;
    }
    @Test public void savesReadsUpdatesAndDeletesEverySource() throws Exception {
        assertFalse(DataSetService.hasConfiguration()); assertFalse(DataSetService.hasConfiguration("missing"));
        assertNull(DataSetService.getDataSet("missing")); assertTrue(DataSetService.getAllDataSets().isEmpty());
        int id = 1;
        for (var type : DataSourceType.values()) {
            var token = type.name(); DataSetService.saveDataSet(token, type, "ACR", "Name", id++);
            assertFalse(DataSetService.hasConfiguration(token));
            if (type == DataSourceType.Access) DataSetService.saveAccessDataSource(token, "source.mdb", "secret");
            else if (DataSetTest.source(type, null, null).isSQLDatabase()) DataSetService.saveSQLDataSource(token, "host.invalid", "1234", "db", "user", "secret");
            else DataSetService.saveSpreadsheetDataSource(token, "source.csv");
            assertTrue(DataSetService.hasConfiguration(token)); assertTrue(DataSetService.hasConfiguration());
            DataSetService.updateRowcount(token, 42); var ds = DataSetService.getDataSet(token);
            assertEquals(type, ds.getType()); assertEquals(42, ds.getLastRowCount());
            var args = new HashMap<String, String>(); args.put("token", token); args.put("dataset_name", "Updated"); args.put("updated_at", "2024-02-29 12:30:00");
            DataSetService.updateDataSource(args); assertEquals("Updated", DataSetService.getDataSet(token).getDataSetName());
            assertEquals(2024, DataSetService.getDataSet(token).getUpdatedAt().getYear());
        }
        assertEquals(DataSourceType.values().length, DataSetService.getAllDataSets().size());
        DataSetService.deleteDataSet("CSV"); assertNull(DataSetService.getDataSet("CSV"));
        sql("INSERT INTO DataSetConfiguration (id, datasource_type) VALUES (100, 'CSV')");
        assertTrue(DataSetService.getAllDataSets().contains(null));
    }
    @Test public void persistsSqlAndTransferHistory() throws Exception {
        DataSetService.saveDataSet("token", DataSourceType.CSV, "ACR", "Name", 1);
        DataSetService.saveSQLCommand("token", encoded("SELECT *\nFROM Records"));
        assertEquals("SELECT * FROM Records", DataSetService.getSQLCommand("token"));
        DataSetService.saveSQLCommand("token", encoded("SELECT * FROM Other")); assertEquals("SELECT * FROM Other", DataSetService.getSQLCommand("token"));
        TokenRepository.setCurrentToken("token"); assertTrue(DataSetService.getTransferHistory().isEmpty());
        var args = new HashMap<String, String>(); args.put("token", "token"); args.put("last_rowcount", "42"); args.put("updated_at", "2024-02-29");
        DataSetService.insertTransferHistory(args); var history = DataSetService.getTransferHistory();
        assertEquals(1, history.size()); assertEquals("42", history.getFirst().getRowcount()); assertEquals("2024-02-29", history.getFirst().getDate());
    }
    @Test public void acceptsApiObjectOrFirstListItemAndRejectsUnexpectedResponses() throws Exception {
        try (var http = mockStatic(HttpService.class)) {
            var config = Map.of("sql_command", encoded("SELECT * FROM Records"));
            http.when(() -> HttpService.getJson(anyString())).thenReturn(config);
            assertEquals(config, DataSetService.getConfigurationDataFromAPI("token"));
            http.when(() -> HttpService.getJson(anyString())).thenReturn(List.of(config));
            assertEquals(config, DataSetService.getConfigurationDataFromAPI("token"));
            for (Object bad : Arrays.asList(null, "invalid", List.of(), List.of("invalid"))) {
                http.when(() -> HttpService.getJson(anyString())).thenReturn(bad);
                assertThrows(Exception.class, () -> DataSetService.getConfigurationDataFromAPI("token"));
            }
        }
    }
    @Test public void readsSqlAndDwcDpFromSimulatedApi() throws Exception {
        try (var http = mockStatic(HttpService.class)) {
            http.when(() -> HttpService.getJson(anyString())).thenReturn(Map.of("sql_command", encoded("SELECT * FROM Records JOIN Places ON 1=1")));
            assertEquals("SELECT * FROM Records JOIN Places ON 1=1", DataSetService.getSQLCommandFromApi("token"));
            assertEquals(List.of("records", "places"), DataSetService.getDataSourceTableNamesFromApi("token"));
            var resource = Map.of("name", "occurrence", "sql", "SELECT * FROM Records", "primaryKey", "id", "foreignKeys", List.of());
            http.when(() -> HttpService.getJson(anyString())).thenReturn(Map.of("dwc_dp", List.of("invalid", Map.of("name", "missingSql"), Map.of("sql", "missingName"), resource)));
            assertEquals(List.of(resource), DataSetService.getDwcDpFromApi("token")); assertEquals(List.of("records"), DataSetService.getDataSourceTableNamesFromApi("token"));
            assertNull(DataSetService.getSQLCommandFromApi("token"));
            http.when(() -> HttpService.getJson(anyString())).thenReturn(Map.of());
            assertEquals(List.of(), DataSetService.getDwcDpFromApi("token")); assertEquals(List.of(), DataSetService.getDataSourceTableNamesFromApi("token"));
        }
    }
}
