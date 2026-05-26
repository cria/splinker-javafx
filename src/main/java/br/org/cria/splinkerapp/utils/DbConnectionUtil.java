package br.org.cria.splinkerapp.utils;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.managers.LocalDbManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DbConnectionUtil {
    private static final int SQLITE_BUSY_TIMEOUT_MS = 10000;

    private DbConnectionUtil() {
    }

    public static Connection getConnection(String url) throws SQLException {
        logPostgreSqlAttempt(url, false, null);
        Connection connection = DriverManager.getConnection(url);
        configure(connection, url);
        logPostgreSqlSuccess(connection, url);
        return connection;
    }

    public static Connection getConnection(String url, String username, String password) throws SQLException {
        logPostgreSqlAttempt(url, true, username);
        Connection connection = DriverManager.getConnection(url, username, password);
        configure(connection, url);
        logPostgreSqlSuccess(connection, url);
        return connection;
    }

    private static void configure(Connection connection, String url) throws SQLException {
        if (!isLocalSqlite(url)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = " + SQLITE_BUSY_TIMEOUT_MS);
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }

    private static boolean isLocalSqlite(String url) {
        if (url == null) {
            return false;
        }

        return url.startsWith("jdbc:sqlite:")
                && url.contains(LocalDbManager.getDbFilePath());
    }

    private static void logPostgreSqlAttempt(String url, boolean withCredentials, String username) {
        if (!isPostgreSql(url)) {
            return;
        }

        ApplicationLog.info("[POSTGRES] DbConnectionUtil chamando DriverManager. url=%s, withCredentials=%s, user=%s"
                .formatted(DatabaseLogUtil.showJdbcUrlWithCredentials(url), withCredentials, username));
    }

    private static void logPostgreSqlSuccess(Connection connection, String url) throws SQLException {
        if (!isPostgreSql(url)) {
            return;
        }

        ApplicationLog.info("[POSTGRES] DbConnectionUtil recebeu conexao do DriverManager. url=%s, autoCommit=%s, catalog=%s, schema=%s, readOnly=%s"
                .formatted(DatabaseLogUtil.showJdbcUrlWithCredentials(url), connection.getAutoCommit(),
                        connection.getCatalog(), connection.getSchema(), connection.isReadOnly()));
    }

    private static boolean isPostgreSql(String url) {
        return url != null && url.startsWith("jdbc:postgresql:");
    }
}
