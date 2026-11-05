package br.org.cria.splinkerapp.parsers;

import com.healthmarketscience.jackcess.*;
import com.healthmarketscience.jackcess.query.Query;
import org.junit.Test;
import java.sql.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccessViewsTest extends ParserBaseTest {
    @Test public void importsSavedQueriesAndSkipsReservedAndMissingTables() throws Exception {
        var db = mock(Database.class); var query = mock(Query.class); var hidden = mock(Query.class);
        when(query.getName()).thenReturn("ViewRecords"); when(query.toSQLString()).thenReturn("SELECT Name AS alias FROM Records");
        when(hidden.getName()).thenReturn("sq_hidden");
        when(db.getTableNames()).thenReturn(new LinkedHashSet<>(List.of("select", "Missing")));
        when(db.getQueries()).thenReturn(List.of(query, hidden));
        when(db.getTable("Missing")).thenThrow(new java.io.FileNotFoundException("missing table"));
        var access = mock(Connection.class); var statement = mock(Statement.class); when(access.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenAnswer(i -> {
            var result = mock(ResultSet.class); var meta = mock(ResultSetMetaData.class);
            when(meta.getColumnCount()).thenReturn(1); when(meta.getColumnLabel(1)).thenReturn("alias");
            when(result.getMetaData()).thenReturn(meta); when(result.next()).thenReturn(true, false); when(result.getString(1)).thenReturn("Ana"); return result;
        });
        var dest = mock(Connection.class); var create = mock(Statement.class); var insert = mock(PreparedStatement.class);
        when(dest.createStatement()).thenReturn(create); when(dest.prepareStatement(anyString())).thenReturn(insert);
        try (var builder = mockStatic(DatabaseBuilder.class); var driver = mockStatic(DriverManager.class)) {
            builder.when(() -> DatabaseBuilder.open(any(java.io.File.class))).thenReturn(db);
            driver.when(() -> DriverManager.getConnection(anyString())).thenReturn(access);
            var p = spy(new AccessFileParser("fake.mdb", "")); doReturn(dest).when(p).getConnection();
            assertEquals("CREATE TABLE IF NOT EXISTS viewrecords (`alias` VARCHAR(1));", p.buildCreateTableCommand(null));
            p.insertDataIntoTable(null);
            verify(insert).setString(1, "Ana"); verify(insert).addBatch(); assertEquals(1, p.getCurrentRow());
            verify(db).close(); verify(access).close(); verify(statement).close(); verify(dest, times(2)).close();
        }
    }
    @Test public void handlesEmptyQuerySchemaAndPassword() throws Exception {
        var db = mock(Database.class); var q = mock(Query.class); when(q.getName()).thenReturn("ViewRecords"); when(q.toSQLString()).thenReturn("SELECT 1");
        when(db.getTableNames()).thenReturn(new LinkedHashSet<>()); when(db.getQueries()).thenReturn(List.of(q));
        var c = mock(Connection.class); var s = mock(Statement.class); var rs = mock(ResultSet.class); var meta = mock(ResultSetMetaData.class);
        when(c.createStatement()).thenReturn(s); when(s.executeQuery(anyString())).thenReturn(rs); when(rs.getMetaData()).thenReturn(meta);
        String previous = System.getProperty("jackcess.password");
        try (var builder = mockStatic(DatabaseBuilder.class); var driver = mockStatic(DriverManager.class)) {
            builder.when(() -> DatabaseBuilder.open(any(java.io.File.class))).thenReturn(db); driver.when(() -> DriverManager.getConnection(anyString())).thenReturn(c);
            var p = spy(new AccessFileParser("fake.accdb", "secret")); doReturn(c).when(p).getConnection();
            assertEquals("secret", System.getProperty("jackcess.password"));
            assertEquals("", p.buildCreateTableCommand(null)); p.insertDataIntoTable(null); verify(c, never()).prepareStatement(anyString());
        } finally { if (previous == null) System.clearProperty("jackcess.password"); else System.setProperty("jackcess.password", previous); }
    }
}
