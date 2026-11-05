package br.org.cria.splinkerapp.parsers;

import com.linuxense.javadbf.*;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;

public class DbfFileParserTest extends ParserBaseTest {
    private DbfFileParser parser(int count) throws Exception {
        var path = temp.getRoot().toPath().resolve("records.dbf");
        try (var out = Files.newOutputStream(path); var writer = new DBFWriter(out, Charset.forName("windows-1252"))) {
            var name = new DBFField(); name.setName("Name"); name.setType(DBFDataType.CHARACTER); name.setLength(40);
            var number = new DBFField(); number.setName("Count"); number.setType(DBFDataType.NUMERIC); number.setLength(10); number.setDecimalCount(2);
            var active = new DBFField(); active.setName("Active"); active.setType(DBFDataType.LOGICAL);
            var date = new DBFField(); date.setName("Date"); date.setType(DBFDataType.DATE);
            writer.setFields(new DBFField[]{name, number, active, date});
            for (int i = 0; i < count; i++) writer.addRecord(new Object[]{"São Paulo", new BigDecimal("42.25"), true, java.sql.Date.valueOf("2024-02-29")});
        }
        return new DbfFileParser(path.toString());
    }
    @Test public void importsTypedDbfFields() throws Exception {
        var p = parser(2); importFile(p, null);
        var data = rows("records"); assertEquals(2, data.size());
        assertEquals("São Paulo", data.getFirst().get(0)); assertEquals("42.25", data.getFirst().get(1));
        assertEquals("1", data.getFirst().get(2)); assertEquals(2, p.getTotalRowCount()); assertEquals(2, p.getCurrentRow());
    }
    @Test public void skipsExcludedTable() throws Exception {
        var p = parser(0); importFile(p, Set.of("other")); assertEquals(0, p.getCurrentRow());
    }
    @Test public void importsEmptyFile() throws Exception {
        var p = parser(0); importFile(p, Set.of("records")); assertTrue(rows("records").isEmpty());
        assertEquals(List.of("", "a", ""), p.getRowAsStringList(new Object[]{null, "a"}, 3));
        assertEquals(List.of(), p.getRowAsStringList(null, 2));
    }
    @Test public void commitsBatches() throws Exception {
        var p = parser(10001); importFile(p, null); assertEquals(10001, rows("records").size());
    }
    @Test public void rejectsMissingFile() {
        assertThrows(java.io.UncheckedIOException.class, () -> new DbfFileParser(temp.getRoot() + "/missing.dbf"));
    }
}
