package br.org.cria.splinkerapp.utils;

import br.org.cria.splinkerapp.managers.LocalDbManager;
import org.junit.Test;
import java.sql.*;
import java.net.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DataUtilitiesTest {
    @Test public void configuresOnlyLocalSqliteConnections() throws Exception {
        var c = mock(Connection.class); var s = mock(Statement.class); when(c.createStatement()).thenReturn(s);
        try (var driver = mockStatic(DriverManager.class)) {
            String local = LocalDbManager.getLocalDbConnectionString();
            driver.when(() -> DriverManager.getConnection(local)).thenReturn(c);
            assertSame(c, DbConnectionUtil.getConnection(local));
            verify(s).execute("PRAGMA busy_timeout = 10000"); verify(s).execute("PRAGMA journal_mode = WAL"); verify(s).execute("PRAGMA synchronous = NORMAL"); verify(s).close();
            driver.when(() -> DriverManager.getConnection("jdbc:sqlite:isolated.db", "user", "pass")).thenReturn(c);
            assertSame(c, DbConnectionUtil.getConnection("jdbc:sqlite:isolated.db", "user", "pass"));
            verify(c, times(1)).createStatement();
        }
    }
    @Test public void opensPostgresWithAndWithoutCredentialsAndPropagatesDriverErrors() throws Exception {
        var c = mock(Connection.class); String url = "jdbc:postgresql://db.invalid/collection";
        try (var driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(url)).thenReturn(c);
            driver.when(() -> DriverManager.getConnection(url, "user", "pass")).thenReturn(c);
            assertSame(c, DbConnectionUtil.getConnection(url)); assertSame(c, DbConnectionUtil.getConnection(url, "user", "pass"));
            verify(c, times(2)).getCatalog(); verify(c, never()).createStatement();
            var failure = new SQLException("offline"); driver.when(() -> DriverManager.getConnection("invalid")).thenThrow(failure);
            assertSame(failure, assertThrows(SQLException.class, () -> DbConnectionUtil.getConnection("invalid")));
        }
    }
    @Test public void extractsTablesFromSqlAndNormalizesAccents() {
        assertEquals(new LinkedHashSet<>(List.of("records", "places", "updates", "target")), SQLiteTableExtractor.extrairTabelas("SELECT *\nFROM [Records] JOIN places ON 1=1; UPDATE updates SET x=1; INSERT INTO target SELECT * FROM records"));
        assertEquals(Set.of(), SQLiteTableExtractor.extrairTabelas("SELECT 1"));
        assertEquals("sao_paulo_123", StringStandards.normalizeString(" São--Paulo 123 "));
        assertTrue(SQLKeywordChecker.isReservedSQLKeyword("SELECT")); assertFalse(SQLKeywordChecker.isReservedSQLKeyword("records"));
    }
    @Test public void rendersFullExceptionChainAndNulls() {
        assertEquals("", ExceptionUtils.getStackTraceAsString(null));
        var error = new SQLException("query failed", "08001", 12, new IllegalStateException("cause"));
        error.setNextException(new SQLException("next")); error.addSuppressed(new RuntimeException("suppressed"));
        String description = DatabaseLogUtil.describeSqlException(error);
        for (String value : List.of("query failed", "08001", "12", "cause", "next", "suppressed")) assertTrue(value, description.contains(value));
        assertTrue(ExceptionUtils.getStackTraceAsString(error).contains("query failed"));
        assertEquals("SQLException=null", DatabaseLogUtil.describeSqlException(null));
        assertEquals("dataset=null", DatabaseLogUtil.describeDataSet(null)); assertEquals("props=null", DatabaseLogUtil.describeConnectionProperties(null));
        var properties = new Properties(); properties.put("host", "db.invalid"); assertTrue(DatabaseLogUtil.describeConnectionProperties(properties).contains("host=db.invalid"));
        assertNull(DatabaseLogUtil.showJdbcUrlWithCredentials(null));
    }
    @Test public void socketFactoryUsesDirectSocketsAndBindsOptionalLocalAddress() throws Exception {
        var factory = spy(new DirectSocketFactory()); var socket = mock(Socket.class); doReturn(socket).when(factory).createSocket();
        var ip = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        assertSame(socket, factory.createSocket("127.0.0.1", 1234)); assertSame(socket, factory.createSocket(ip, 1234));
        assertSame(socket, factory.createSocket("127.0.0.1", 1234, ip, 4321)); assertSame(socket, factory.createSocket(ip, 1234, ip, 4321));
        verify(socket, times(4)).connect(new InetSocketAddress(ip, 1234)); verify(socket, times(2)).bind(new InetSocketAddress(ip, 4321));
        try (var actual = new DirectSocketFactory().createSocket()) { assertFalse(actual.isConnected()); }
    }
}
