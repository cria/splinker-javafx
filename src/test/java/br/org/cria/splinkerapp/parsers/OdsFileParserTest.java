package br.org.cria.splinkerapp.parsers;

import com.github.miachm.sods.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OdsFileParserTest extends ParserBaseTest {
    private OdsFileParser parser(int count) throws Exception {
        var path = temp.getRoot().toPath().resolve("source.ods");
        var book = new SpreadSheet(); var sheet = new Sheet("Records", count + 1, 2);
        sheet.getRange(0, 0).setValue("Name"); sheet.getRange(0, 1).setValue("Place");
        for (int i = 1; i <= count; i++) {
            sheet.getRange(i, 0).setValue("Ana" + i); sheet.getRange(i, 1).setValue("Brasil");
        }
        book.appendSheet(sheet); book.save(path.toFile()); return new OdsFileParser(path.toString());
    }
    @Test public void importsRows() throws Exception {
        var p = parser(2); importFile(p, null);
        assertEquals(List.of(List.of("Ana1", "Brasil"), List.of("Ana2", "Brasil")), rows("records"));
        assertEquals(2, p.getCurrentRow());
    }
    @Test public void skipsUnselectedSheets() throws Exception {
        var p = parser(1); importFile(p, Set.of("other")); assertEquals(0, p.getCurrentRow());
    }
    @Test public void importsHeaderOnly() throws Exception {
        var p = parser(0); importFile(p, Set.of("records")); assertTrue(rows("records").isEmpty());
    }
    @Test public void convertsNullCells() throws Exception {
        var p = parser(0); var sheet = new Sheet("Nulls", 1, 2); sheet.getRange(0, 1).setValue("value");
        assertEquals(List.of("", "value"), p.getRowAsStringList(List.of(sheet.getRange(0, 0), sheet.getRange(0, 1)), 2));
    }
    @Test public void commitsBatches() throws Exception {
        var p = parser(10001); importFile(p, null); assertEquals(10001, rows("records").size());
    }
    @Test public void rollsBackOnFailure() throws Exception {
        var p = spy(parser(1)); var c = mock(java.sql.Connection.class);
        doReturn(c).when(p).getConnection(); when(c.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("simulated"));
        assertThrows(java.sql.SQLException.class, () -> p.insertDataIntoTable(null));
        verify(c).rollback(); verify(c).setAutoCommit(true); verify(c).close();
    }
}
