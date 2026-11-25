package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.RepositoryTestBase;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;

public class SQLiteSourceTest extends RepositoryTestBase {
    private static final String TOKEN = "sqlite-token";
    private static final int DATASET_ID = 321;

    @Test
    public void savesSQLiteSourceAndExportsBothPackageFormatsDirectlyFromAReadOnlyConnection() throws Exception {
        var source = temp.getRoot().toPath().resolve("coleção #1% dados.sqlite3");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + source);
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE records (id INTEGER, name TEXT)");
            statement.executeUpdate("INSERT INTO records VALUES (1, 'Ipe'), (2, NULL)");
            statement.executeUpdate("CREATE VIEW occurrences AS SELECT id AS occurrenceID, name FROM records");
        }
        DataSetService.saveDataSet(TOKEN, DataSourceType.SQLite, "ACR", "Collection", DATASET_ID);
        DataSetService.saveSQLiteDataSource(TOKEN, source.toString());

        var dataset = DataSetService.getDataSet(TOKEN);
        assertEquals(DataSourceType.SQLite, dataset.getType());
        assertEquals(source.toRealPath(), Path.of(dataset.getDataSetFilePath()));
        assertTrue(dataset.isSQLDatabase());
        assertFalse(dataset.isFile());
        assertTrue(DataSetService.hasConfiguration(TOKEN));
        try (var connection = dataset.getDataSetConnection(); var statement = connection.createStatement()) {
            assertTrue(connection.isReadOnly());
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM records"));
        }

        var query = "SELECT occurrenceID, name FROM occurrences ORDER BY occurrenceID";
        try (var api = mockStatic(DataSetService.class)) {
            api.when(() -> DataSetService.getSQLCommandFromApi(TOKEN)).thenReturn(query);
            var archive = new DarwinCoreArchiveService(dataset).readDataFromSource().generateZIPFile();
            assertEquals("occurrenceID\tname\n1\tIpe\n2\t", Files.readString(Path.of(archive.getTxtFilePath())));
            assertEquals(2, archive.getTotalRowCount());
            try (var zip = new ZipFile(archive.zipFile)) {
                assertEquals(1, zip.size());
                assertNotNull(zip.getEntry("occurrence.txt"));
            }

            api.when(() -> DataSetService.getSQLCommandFromApi(TOKEN)).thenReturn(null);
            api.when(() -> DataSetService.getDwcDpFromApi(TOKEN))
                    .thenReturn(List.of(Map.of("name", "occurrence", "sql", query)));
            var packageArchive = new DarwinCoreArchiveService(dataset).readDataFromSource().generateZIPFile();
            assertEquals("occurrenceID,name\n1,Ipe\n2,", Files.readString(Path.of(packageArchive.getTxtFilePath())));
            assertEquals(2, packageArchive.getTotalRowCount());
            try (var zip = new ZipFile(packageArchive.zipFile)) {
                assertNotNull(zip.getEntry("occurrence.csv"));
                assertNotNull(zip.getEntry("datapackage.json"));
            }
        }
    }

    @Test
    public void rejectsMissingInvalidAndReservedFilesWithoutSavingOrDeletingThem() throws Exception {
        DataSetService.saveDataSet(TOKEN, DataSourceType.SQLite, "ACR", "Collection", DATASET_ID);
        var missing = temp.getRoot().toPath().resolve("missing.sqlite");
        assertThrows(IllegalArgumentException.class, () -> DataSetService.saveSQLiteDataSource(TOKEN, missing.toString()));
        assertFalse(Files.exists(missing));

        var invalid = Files.writeString(temp.getRoot().toPath().resolve("invalid.sqlite"), "plain text");
        assertThrows(IllegalArgumentException.class, () -> DataSetService.saveSQLiteDataSource(TOKEN, invalid.toString()));
        assertFalse(DataSetService.hasConfiguration(TOKEN));

        var export = Files.createDirectory(temp.getRoot().toPath().resolve(Integer.toString(DATASET_ID)));
        var reserved = Files.copy(temp.getRoot().toPath().resolve("test.db"), export.resolve("source.sqlite"));
        assertThrows(IllegalArgumentException.class, () -> DataSetService.saveSQLiteDataSource(TOKEN, reserved.toString()));
        var dataset = DataSetService.getDataSet(TOKEN);
        dataset.setDataSourceFilePath(reserved.toString());
        assertThrows(IllegalArgumentException.class, () -> new DarwinCoreArchiveService(dataset));
        assertTrue(Files.exists(reserved));
    }
}
