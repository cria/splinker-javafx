package br.org.cria.splinkerapp.parsers;

import com.healthmarketscience.jackcess.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AccessFileParserTest extends ParserBaseTest {
    private AccessFileParser parser(Database.FileFormat format) throws Exception {
        var path = temp.getRoot().toPath().resolve("source" + format.getFileExtension());
        try (var db = DatabaseBuilder.create(format, path.toFile())) {
            var table = new TableBuilder("Records").addColumn(new ColumnBuilder("Name", DataType.TEXT))
                    .addColumn(new ColumnBuilder("Place", DataType.TEXT)).toTable(db);
            table.addRow("Ana", "Brasil"); table.addRow("Bia", null);
            new TableBuilder("Ignored").addColumn(new ColumnBuilder("Name", DataType.TEXT)).toTable(db).addRow("Other");
        }
        return new AccessFileParser(path.toString(), "");
    }
    @Test public void importsMdbAndAccdbFormats() throws Exception {
        for (var format : List.of(Database.FileFormat.V2000, Database.FileFormat.V2003, Database.FileFormat.V2007, Database.FileFormat.V2010)) {
            var p = parser(format); importFile(p, Set.of("records"));
            assertEquals(List.of(List.of("Ana", "Brasil"), List.of("Bia", "")), rows("records"));
            assertEquals(2, p.getCurrentRow()); assertEquals(2, p.getTotalRowCount());
        }
    }
    @Test public void importsAllTablesWhenSelectionIsNull() throws Exception {
        var p = parser(Database.FileFormat.V2000); importFile(p, null); assertEquals(List.of(List.of("Other")), rows("ignored"));
    }
    @Test public void emptySelectionImportsNothingAndClosesResources() throws Exception {
        var p = parser(Database.FileFormat.V2000); importFile(p, Set.of());
        assertTrue(p.accessConnection.isClosed()); assertTrue(p.stmt.isClosed());
        assertEquals(0, p.getCurrentRow());
    }
    @Test public void readsViewColumnAliasesThroughJdbc() throws Exception {
        var p = parser(Database.FileFormat.V2000);
        assertEquals(List.of("alias"), p.gerarColunasDasViews("SELECT Name AS alias FROM Records"));
        assertEquals(List.of("a"), p.getRowAsStringList(List.of("a"), 1));
        p.insertDataIntoTable(Set.of());
    }
    @Test public void rejectsMissingFile() {
        assertThrows(Exception.class, () -> new AccessFileParser(temp.getRoot() + "/missing.mdb", ""));
    }
}
