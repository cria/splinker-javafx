package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.*;
import br.org.cria.splinkerapp.parsers.*;
import br.org.cria.splinkerapp.services.implementations.*;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FileSourceManagerTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private DataSet source(DataSourceType type, String path) {
        var ds = DataSet.factory("test-token", type, path, null, null, null, null, null, "TEST", "Test", 0, 1, null);
        return ds;
    }
    @Test public void dispatchesEveryLocalFileFormat() throws Exception {
        try (var csv = mockConstruction(CsvFileParser.class); var xls = mockConstruction(XLSFileParser.class);
             var xlsx = mockConstruction(XLSXFileParser.class); var ods = mockConstruction(OdsFileParser.class);
             var dbf = mockConstruction(DbfFileParser.class); var access = mockConstruction(AccessFileParser.class)) {
            var cases = Map.of("CSV", CsvFileParser.class, "tsv", CsvFileParser.class, "txt", CsvFileParser.class,
                    "xls", XLSFileParser.class, "xlsx", XLSXFileParser.class, "ods", OdsFileParser.class,
                    "dbf", DbfFileParser.class, "mdb", AccessFileParser.class, "accdb", AccessFileParser.class);
            for (var entry : cases.entrySet()) {
                var m = new FileSourceManager(source(DataSourceType.CSV, " source." + entry.getKey() + " "));
                assertEquals(entry.getValue(), m.getParser().getClass());
            }
        }
    }
    @Test public void importsSelectionAndUpdatesCount() throws Exception {
        try (var csv = mockConstruction(CsvFileParser.class); var service = mockStatic(DataSetService.class)) {
            var m = new FileSourceManager(source(DataSourceType.CSV, "source.csv"));
            var tables = Set.of("splinker"); when(m.getParser().getTotalRowCount()).thenReturn(42);
            m.importData(tables);
            verify(m.getParser()).createTableBasedOnSheet(tables); verify(m.getParser()).insertDataIntoTable(tables);
            service.verify(() -> DataSetService.updateRowcount("test-token", 42));
        }
    }
    @Test public void rejectsUnsupportedFilesAndRemoteSources() {
        for (var ds : List.of(source(DataSourceType.CSV, "source.pdf"), source(DataSourceType.GoogleSheets, "source.xlsx"),
                source(DataSourceType.Excel, "https://example.org/source.xlsx"), source(DataSourceType.CSV, "https://drive.google.com/file/d/abc/view"))) {
            assertThrows(Exception.class, () -> new FileSourceManager(ds));
        }
    }
    @Test public void downloadsRemoteXlsAndXlsxAndDeletesTemporaryFiles() throws Exception {
        try (var drive = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS);
             var xls = mockConstruction(XLSFileParser.class); var xlsx = mockConstruction(XLSXFileParser.class);
             var service = mockStatic(DataSetService.class)) {
            for (var format : List.of(SpreadsheetFormat.XLS, SpreadsheetFormat.XLSX)) {
                var path = temp.newFile().toPath(); String link = "https://docs.google.com/spreadsheets/d/abc/edit";
                drive.when(() -> GoogleDriveFileService.downloadSpreadsheet(link)).thenReturn(new SpreadsheetRemoteFile(path, format));
                var m = new FileSourceManager(source(DataSourceType.GoogleSheets, link));
                assertTrue(format == SpreadsheetFormat.XLS ? m.getParser() instanceof XLSFileParser : m.getParser() instanceof XLSXFileParser);
                m.importData(null); assertFalse(Files.exists(path));
            }
        }
    }
    @Test public void deletesRemoteFilesOnImportFailure() throws Exception {
        try (var drive = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS); var xlsx = mockConstruction(XLSXFileParser.class)) {
            var path = temp.newFile().toPath(); String link = "https://drive.google.com/file/d/abc/view";
            drive.when(() -> GoogleDriveFileService.downloadSpreadsheet(link)).thenReturn(new SpreadsheetRemoteFile(path, SpreadsheetFormat.XLSX));
            var m = new FileSourceManager(source(DataSourceType.Excel, link));
            doThrow(new java.io.IOException("simulated")).when(m.getParser()).insertDataIntoTable(null);
            assertThrows(java.io.IOException.class, () -> m.importData(null)); assertFalse(Files.exists(path));
        }
    }
    @Test public void deletesUnknownRemoteFormatOnConstructionFailure() throws Exception {
        try (var drive = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            var path = temp.newFile().toPath(); String link = "https://drive.google.com/file/d/abc/view";
            drive.when(() -> GoogleDriveFileService.downloadSpreadsheet(link)).thenReturn(new SpreadsheetRemoteFile(path, SpreadsheetFormat.UNKNOWN));
            assertThrows(IllegalArgumentException.class, () -> new FileSourceManager(source(DataSourceType.Excel, link)));
            assertFalse(Files.exists(path));
        }
    }
    @Test public void simulatesNumbersConversionWithoutMacOs() throws Exception {
        try (var numbers = mockConstruction(NumbersFileParser.class); var conversion = mockStatic(NumbersFileParser.class); var service = mockStatic(DataSetService.class)) {
            conversion.when(() -> NumbersFileParser.numbersToXLSX("source.numbers")).thenReturn("converted.xlsx");
            var m = new FileSourceManager(source(DataSourceType.Numbers, "source.numbers")); m.importData(null);
            verify(numbers.constructed().getFirst()).parseFile("converted.xlsx", null);
        }
    }
}
