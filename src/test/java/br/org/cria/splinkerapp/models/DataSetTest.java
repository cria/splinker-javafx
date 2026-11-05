package br.org.cria.splinkerapp.models;

import br.org.cria.splinkerapp.utils.*;
import org.junit.Test;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DataSetTest {
    public static DataSet source(DataSourceType type, String user, String password) {
        return DataSet.factory("test-token", type, "source.csv", "db.invalid", "collection", user, password,
                "1234", "TEST", "Test collection", 10, 123, LocalDateTime.of(2024, 1, 1, 0, 0));
    }
    @Test public void classifiesEverySupportedSourceAndBuildsConnectionStrings() {
        var fileTypes = EnumSet.of(DataSourceType.CSV, DataSourceType.Excel, DataSourceType.GoogleSheets,
                DataSourceType.LibreOfficeCalc, DataSourceType.Numbers, DataSourceType.dBase);
        for (var type : DataSourceType.values()) {
            var ds = source(type, "reader", "test-password");
            assertEquals(type.name(), fileTypes.contains(type), ds.isFile());
            assertEquals(type == DataSourceType.Access, ds.isAccessDb());
            assertEquals(!fileTypes.contains(type) && type != DataSourceType.Access, ds.isSQLDatabase());
            assertEquals(type, ds.getType()); assertEquals("test-token", ds.getToken());
            assertEquals("source.csv", ds.getDataSetFilePath()); assertEquals("db.invalid", ds.getDbHost());
            assertEquals("collection", ds.getDbName()); assertEquals("reader", ds.getDbUser());
            assertEquals("test-password", ds.getDbPassword()); assertEquals("1234", ds.getDbPort());
            assertEquals("TEST", ds.getDataSetAcronym()); assertEquals("Test collection", ds.getDataSetName());
            assertEquals(10, ds.getLastRowCount()); assertEquals(123, ds.getId()); assertNotNull(ds.getUpdatedAt()); assertNotNull(ds.getCreatedAt());
        }
        assertEquals("jdbc:mysql://db.invalid/collection?user=reader&password=test-password", source(DataSourceType.MySQL, "reader", "test-password").getConnectionString());
        assertEquals("jdbc:mariadb://db.invalid:1234/collection?user=reader&password=test-password", source(DataSourceType.MariaDB, "reader", "test-password").getConnectionString());
        assertEquals("jdbc:postgresql://db.invalid:1234/collection?user=reader&password=test-password", source(DataSourceType.PostgreSQL, "reader", "test-password").getConnectionString());
        assertEquals("jdbc:oracle:thin:reader/test-password@db.invalid:1234:collection", source(DataSourceType.Oracle, "reader", "test-password").getConnectionString());
        assertEquals("jdbc:sqlserver://db.invalid:1234;encrypt=true;trustServerCertificate=true;databaseName=collection;user=reader;password=test-password", source(DataSourceType.SQLServer, "reader", "test-password").getConnectionString());
        assertNull(source(null, null, null).getConnectionString());
    }
    @Test public void connectsAllSqlSourcesUsingMockJdbcOnly() throws Exception {
        try (var jdbc = mockStatic(DbConnectionUtil.class); var logs = mockStatic(DatabaseLogUtil.class)) {
            var c = mock(Connection.class);
            for (var type : EnumSet.of(DataSourceType.MySQL, DataSourceType.MariaDB, DataSourceType.PostgreSQL, DataSourceType.SQLServer, DataSourceType.Oracle)) {
                jdbc.when(() -> DbConnectionUtil.getConnection(anyString(), eq("reader"), eq("test-password"))).thenReturn(c);
                var ds = source(type, "reader", "test-password");
                assertSame(c, ds.getDataSetConnection());
                String expected = ds.getConnectionString() + (type == DataSourceType.PostgreSQL
                        ? "&sslmode=require&socketFactory=br.org.cria.splinkerapp.utils.DirectSocketFactory" : "");
                jdbc.verify(() -> DbConnectionUtil.getConnection(expected, "reader", "test-password"));
            }
        }
    }
    @Test public void handlesAnonymousAndAccessCredentials() throws Exception {
        try (var jdbc = mockStatic(DbConnectionUtil.class); var logs = mockStatic(DatabaseLogUtil.class)) {
            var c = mock(Connection.class);
            jdbc.when(() -> DbConnectionUtil.getConnection(anyString())).thenReturn(c);
            jdbc.when(() -> DbConnectionUtil.getConnection(anyString(), anyString(), anyString())).thenReturn(c);
            for (String user : Arrays.asList(null, "", "  ")) {
                var ds = source(DataSourceType.MySQL, user, null); assertSame(c, ds.getDataSetConnection());
                jdbc.verify(() -> DbConnectionUtil.getConnection(ds.getConnectionString()));
            }
            for (String password : Arrays.asList(null, "", "  ")) {
                var ds = source(DataSourceType.Access, "reader", password); assertSame(c, ds.getDataSetConnection());
            }
            assertSame(c, source(DataSourceType.Access, "reader", "secret").getDataSetConnection());
            assertSame(c, source(DataSourceType.PostgreSQL, "", "").getDataSetConnection());
        }
    }
    @Test public void postgresFallsBackToUnencryptedConnection() throws Exception {
        try (var jdbc = mockStatic(DbConnectionUtil.class); var logs = mockStatic(DatabaseLogUtil.class)) {
            var c = mock(Connection.class); var ds = source(DataSourceType.PostgreSQL, "reader", "test-password");
            jdbc.when(() -> DbConnectionUtil.getConnection(contains("sslmode=require"), eq("reader"), eq("test-password"))).thenThrow(new SQLException("SSL unavailable"));
            jdbc.when(() -> DbConnectionUtil.getConnection(contains("sslmode=disable"), eq("reader"), eq("test-password"))).thenReturn(c);
            assertSame(c, ds.getDataSetConnection());
        }
    }
    @Test public void preservesBothPostgresFailures() throws Exception {
        try (var jdbc = mockStatic(DbConnectionUtil.class); var logs = mockStatic(DatabaseLogUtil.class)) {
            var first = new SQLException("SSL failed"); var second = new SQLException("connection failed");
            jdbc.when(() -> DbConnectionUtil.getConnection(contains("sslmode=require"), anyString(), anyString())).thenThrow(first);
            jdbc.when(() -> DbConnectionUtil.getConnection(contains("sslmode=disable"), anyString(), anyString())).thenThrow(second);
            assertSame(second, assertThrows(SQLException.class, () -> source(DataSourceType.PostgreSQL, "reader", "secret").getDataSetConnection()));
            assertArrayEquals(new Throwable[]{first}, second.getSuppressed());
        }
    }
    @Test public void updatesConfigurationProperties() {
        var ds = source(DataSourceType.CSV, null, null);
        ds.setId(2); ds.setLastRowCount(3); ds.setType(DataSourceType.Excel); ds.setDbHost("host"); ds.setDbName("db");
        ds.setDbUser("user"); ds.setDbPassword("pass"); ds.setDbPort("42"); ds.setDataSourceFilePath("file.xlsx");
        ds.setToken("new"); ds.setDataSetName("name"); ds.setDataSetAcronym("ACR"); var now = LocalDateTime.now(); ds.setUpdatedAt(now);
        assertEquals(2, ds.getId()); assertEquals(3, ds.getLastRowCount()); assertEquals(DataSourceType.Excel, ds.getType());
        assertEquals("host", ds.getDbHost()); assertEquals("db", ds.getDbName()); assertEquals("user", ds.getDbUser());
        assertEquals("pass", ds.getDbPassword()); assertEquals("42", ds.getDbPort()); assertEquals("file.xlsx", ds.getDataSetFilePath());
        assertEquals("new", ds.getToken()); assertEquals("name", ds.getDataSetName()); assertEquals("ACR", ds.getDataSetAcronym()); assertEquals(now, ds.getUpdatedAt());
    }
}
