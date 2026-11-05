package br.org.cria.splinkerapp.parsers;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CsvFileParserTest extends ParserBaseTest {
    @Test public void importsEveryDelimiterAndPreservesColumns() throws Exception {
        for (String delimiter : List.of(",", ";", "\t")) {
            var path = text("source.csv", "Name" + delimiter + "Place\nAna" + delimiter + "Brasil\n");
            var parser = new CsvFileParser(path.toString());
            importFile(parser, null);
            assertEquals(List.of(List.of("Ana", "Brasil")), rows("splinker"));
            assertEquals(1, parser.getCurrentRow());
            assertEquals(1, parser.getTotalRowCount());
            assertEquals(delimiter, CsvFileParser.detectSeparator(path.toString()));
        }
    }
    @Test public void preservesQuotedDelimitersQuotesAndMultilineCells() throws Exception {
        var p = new CsvFileParser(text("quoted.csv", "Name,Place\n\"Ana, Maria\",\"A \"\"B\"\"\nC\"\n").toString());
        importFile(p, Set.of("splinker"));
        assertEquals(List.of(List.of("Ana, Maria", "A \"B\" C")), rows("splinker"));
    }
    @Test public void importsSingleColumnAndNullValues() throws Exception {
        var p = new CsvFileParser(text("one.csv", "Name\nAna\nNULL\n").toString());
        importFile(p, null);
        assertEquals(List.of(List.of("Ana"), List.of("")), rows("splinker"));
    }
    @Test public void missingCellsDoNotReusePreviousRowValues() throws Exception {
        var p = new CsvFileParser(text("missing.csv", "Name,Place\nAna,Brasil\nBia\n").toString());
        importFile(p, null);
        assertEquals(Arrays.asList("Bia", null), rows("splinker").get(1));
    }
    @Test public void excludedTableIsPreserved() throws Exception {
        var path = text("selected.csv", "Name,Place\nAna,Brasil\n");
        importFile(new CsvFileParser(path.toString()), null);
        importFile(new CsvFileParser(path.toString()), Set.of("other"));
        assertEquals(1, rows("splinker").size());
    }
    @Test public void commitsAcrossBatchBoundary() throws Exception {
        var p = new CsvFileParser(text("batch.csv", "Name,Place\n" + "Ana,Brasil\n".repeat(10001)).toString());
        importFile(p, null);
        assertEquals(10001, rows("splinker").size());
    }
    @Test public void rollsBackAndClosesOnBatchFailure() throws Exception {
        var p = spy(new CsvFileParser(text("bad.csv", "Name,Place\nAna,Brasil\n").toString()));
        var c = mock(java.sql.Connection.class);
        var s = mock(java.sql.PreparedStatement.class);
        doReturn(c).when(p).getConnection();
        when(c.prepareStatement(anyString())).thenReturn(s);
        when(s.executeBatch()).thenThrow(new java.sql.SQLException("simulated"));
        assertThrows(java.sql.SQLException.class, () -> p.insertDataIntoTable(null));
        verify(c).rollback(); verify(c).setAutoCommit(true); verify(c).close(); verify(s).close();
    }
    @Test public void mapsRowsAndRejectsUndetectableSeparator() throws Exception {
        var p = new CsvFileParser(text("header.csv", "Name,Place\n").toString());
        assertNull(p.getRowAsStringList(null, 2));
        assertEquals(List.of("a", "b"), p.getRowAsStringList(new String[]{"a", "b"}, 2));
        assertThrows(java.io.IOException.class, () -> CsvFileParser.detectSeparator(text("empty.csv", "").toString()));
        assertThrows(java.io.IOException.class, () -> CsvFileParser.detectSeparator(text("plain.csv", "Name").toString()));
        importFile(p, null); assertTrue(rows("splinker").isEmpty());
    }
    @Test public void cleansCellValues() {
        for (String value : Arrays.asList(null, "null", "NULL", "")) assertEquals("", FileParser.getCellValue(value));
        assertEquals("a b c d e f", FileParser.getCellValue(" a\rb\tc\nd\\ne\\rf "));
    }
}
