package br.org.cria.splinkerapp.parsers;

import com.linuxense.javadbf.*;
import org.junit.Test;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DbfTypesTest extends ParserBaseTest {
    @Test public void bindsMemoNumericLogicalDateAndTimestampValues() throws Exception {
        var types = List.of(DBFDataType.MEMO, DBFDataType.MEMO, DBFDataType.CURRENCY, DBFDataType.CURRENCY,
                DBFDataType.DOUBLE, DBFDataType.DOUBLE, DBFDataType.DOUBLE,
                DBFDataType.LOGICAL, DBFDataType.LOGICAL, DBFDataType.LOGICAL, DBFDataType.LOGICAL,
                DBFDataType.DATE, DBFDataType.DATE, DBFDataType.DATE, DBFDataType.DATE,
                DBFDataType.TIMESTAMP, DBFDataType.TIMESTAMP, DBFDataType.TIMESTAMP,
                DBFDataType.BLOB, DBFDataType.CHARACTER);
        var date = java.sql.Date.valueOf("2024-02-29");
        Object[] values = {"São".getBytes(Charset.forName("windows-1252")), "memo", "1,25", " ", 2.5, "3,5", " ",
                true, "Y", "N", " ", date, "20240229", "29/02/2024", "invalid", date, "2024-02-29 12:30:00", "invalid", "raw", null};
        var fields = new ArrayList<DBFField>();
        for (int i = 0; i < types.size(); i++) {
            var field = mock(DBFField.class); when(field.getName()).thenReturn("field" + i); when(field.getType()).thenReturn(types.get(i));
            when(field.getLength()).thenReturn(10); when(field.getDecimalCount()).thenReturn(2); fields.add(field);
        }
        var file = temp.newFile("typed.dbf"); var conn = mock(Connection.class); var statement = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(statement); when(conn.createStatement()).thenReturn(mock(Statement.class));
        try (var readers = mockConstruction(DBFReader.class, (r, ctx) -> {
            when(r.getFieldCount()).thenReturn(fields.size());
            when(r.getField(anyInt())).thenAnswer(i -> fields.get(i.getArgument(0)));
            when(r.nextRecord()).thenReturn(new Object[]{null}, values, null); when(r.getRecordCount()).thenReturn(1);
        })) {
            var parser = spy(new DbfFileParser(file.toString())); doReturn(conn).when(parser).getConnection();
            var ddl = parser.buildCreateTableCommand(null); assertTrue(ddl.contains("LONGTEXT")); assertTrue(ddl.contains("TIMESTAMP")); assertTrue(ddl.contains("DECIMAL(10,2)"));
            parser.insertDataIntoTable(null);
            verify(statement).setString(1, "São"); verify(statement).setString(2, "memo"); verify(statement).setBigDecimal(3, new BigDecimal("1.25"));
            verify(statement).setNull(4, Types.DECIMAL); verify(statement).setDouble(5, 2.5); verify(statement).setDouble(6, 3.5);
            verify(statement).setNull(7, Types.DOUBLE); verify(statement).setBoolean(8, true); verify(statement).setBoolean(9, true); verify(statement).setBoolean(10, false);
            verify(statement).setNull(11, Types.BOOLEAN); verify(statement).setDate(12, date); verify(statement).setDate(13, date); verify(statement).setDate(14, date);
            verify(statement).setString(15, "invalid"); verify(statement).setTimestamp(16, new Timestamp(date.getTime()));
            verify(statement).setTimestamp(17, Timestamp.valueOf("2024-02-29 12:30:00")); verify(statement).setString(18, "invalid");
            verify(statement).setString(19, "raw"); verify(statement).setString(20, ""); verify(statement).addBatch();
            assertEquals(1, parser.getCurrentRow()); verify(readers.constructed().getFirst()).close(); verify(conn, times(2)).close();
        }
    }
    @Test public void closesResourcesOnSqlFailure() throws Exception {
        var file = temp.newFile("bad.dbf"); var field = mock(DBFField.class); when(field.getType()).thenReturn(DBFDataType.CHARACTER);
        var c = mock(Connection.class); var s = mock(PreparedStatement.class); when(c.prepareStatement(anyString())).thenReturn(s);
        when(s.executeBatch()).thenThrow(new SQLException("simulated"));
        try (var readers = mockConstruction(DBFReader.class, (r, ctx) -> { when(r.getFieldCount()).thenReturn(1); when(r.getField(0)).thenReturn(field); })) {
            var p = spy(new DbfFileParser(file.toString())); doReturn(c).when(p).getConnection();
            assertThrows(SQLException.class, () -> p.insertDataIntoTable(null)); verify(s).close(); verify(c).rollback(); verify(c).close(); verify(readers.constructed().getFirst()).close();
        }
    }
}
