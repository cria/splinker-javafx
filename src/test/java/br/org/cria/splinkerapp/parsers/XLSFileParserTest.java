package br.org.cria.splinkerapp.parsers;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class XLSFileParserTest extends ParserBaseTest {
    private XLSFileParser parser(int count) throws Exception {
        return new XLSFileParser(SpreadsheetFixtures.create(temp.getRoot().toPath().resolve("source.xls"), false, count).toString());
    }
    @Test public void importsTypedCellsAndCountsRows() throws Exception {
        var p = parser(2); importFile(p, null);
        assertEquals(List.of(List.of("Ana", "42", "TRUE"), List.of("Ana", "42", "TRUE")), rows("records"));
        assertEquals(2, p.getTotalRowCount()); assertEquals(2, p.getCurrentRow());
    }
    @Test public void skipsUnselectedSheets() throws Exception {
        var p = parser(1); importFile(p, Set.of("other")); assertEquals(0, p.getCurrentRow());
    }
    @Test public void importsHeaderOnly() throws Exception {
        var p = parser(0); importFile(p, Set.of("records")); assertTrue(rows("records").isEmpty());
    }
    @Test public void importsMissingCellsAndEvaluatesFormulas() throws Exception {
        var p = parser(1); var row = p.workbook.getSheetAt(0).getRow(1);
        row.removeCell(row.getCell(0)); row.getCell(1).setCellFormula("1+2");
        importFile(p, null);
        assertEquals(List.of("", "3", "TRUE"), rows("records").getFirst());
    }
    @Test public void commitsBatches() throws Exception {
        var p = parser(10001); importFile(p, null); assertEquals(10001, rows("records").size());
    }
    @Test public void rollsBackOnFailure() throws Exception {
        var p = spy(parser(1)); var c = mock(java.sql.Connection.class);
        doReturn(c).when(p).getConnection();
        when(c.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("simulated"));
        assertThrows(RuntimeException.class, () -> p.insertDataIntoTable(null));
        verify(c).rollback(); verify(c).setAutoCommit(true); verify(c).close();
    }
}
