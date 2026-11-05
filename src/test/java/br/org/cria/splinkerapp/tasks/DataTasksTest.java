package br.org.cria.splinkerapp.tasks;

import br.org.cria.splinkerapp.models.*;
import br.org.cria.splinkerapp.managers.*;
import br.org.cria.splinkerapp.services.implementations.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DataTasksTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void importsOnlyTablesRequestedByApi() throws Exception {
        var ds = DataSetTest.source(DataSourceType.CSV, null, null);
        try (var managers = mockConstruction(FileSourceManager.class); var api = mockStatic(DataSetService.class)) {
            api.when(() -> DataSetService.getDataSourceTableNamesFromApi("test-token")).thenReturn(List.of("records", "places"));
            var task = new ImportDataTask(ds);
            try { assertNull(task.call()); verify(managers.constructed().getFirst()).importData(Set.of("records", "places")); }
            finally { EventBusManager.getEvent("READ_ROW").unregister(task); }
        }
    }
    @Test public void generatesArchiveInOrderAndTransfersIt() throws Exception {
        var service = mock(DarwinCoreArchiveService.class); when(service.readDataFromSource()).thenReturn(service); when(service.generateZIPFile()).thenReturn(service);
        var task = new GenerateDarwinCoreArchiveTask(service);
        try {
            assertSame(service, task.call()); var order = inOrder(service); order.verify(service).readDataFromSource(); order.verify(service).generateZIPFile();
            assertNull(new TransferFileTask(service).call()); verify(service).transferData();
        } finally { EventBusManager.getEvent("WRITE_ROW").unregister(task); }
    }
    @Test public void detectsFewerRowsAndAllowsEqualOrGreaterCounts() throws Exception {
        var ds = mock(DataSet.class); var service = mock(DarwinCoreArchiveService.class); var file = temp.newFile().toPath();
        when(service.getDataSet()).thenReturn(ds); when(service.getTxtFilePath()).thenReturn(file.toString());
        Files.writeString(file, "name\nAna\nBia\n"); var task = new CheckRecordCountTask(service);
        when(ds.getLastRowCount()).thenReturn(3); assertTrue(task.call());
        when(ds.getLastRowCount()).thenReturn(2); assertFalse(task.call());
        when(ds.getLastRowCount()).thenReturn(1); assertFalse(task.call());
        Files.delete(file); assertThrows(java.io.IOException.class, task::call);
    }
}
