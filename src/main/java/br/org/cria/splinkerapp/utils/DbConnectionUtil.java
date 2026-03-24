package br.org.cria.splinkerapp.utils;

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
        Connection connection = DriverManager.getConnection(url);
        configure(connection, url);
        return connection;
    }

    public static Connection getConnection(String url, String username, String password) throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);
        configure(connection, url);
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
}
