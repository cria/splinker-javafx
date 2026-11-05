package br.org.cria.splinkerapp.repositories;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.sql.*;

public abstract class RepositoryTestBase extends BaseRepository {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    protected String url;
    private String previousConnection, previousToken, previousDir;
    @Before public void setupDatabase() throws Exception {
        previousConnection = LOCAL_DB_CONNECTION; previousToken = System.getProperty("splinker_token"); previousDir = System.getProperty("user.dir");
        url = "jdbc:sqlite:" + temp.getRoot().toPath().resolve("test.db"); LOCAL_DB_CONNECTION = url;
        System.clearProperty("splinker_token"); System.setProperty("user.dir", temp.getRoot().getAbsolutePath());
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            s.executeUpdate(Files.readString(Path.of(previousDir, "src/main/resources/scripts/sql/create_tables.sql")));
        }
    }
    @After public void restoreGlobals() {
        LOCAL_DB_CONNECTION = previousConnection;
        if (previousToken == null) System.clearProperty("splinker_token"); else System.setProperty("splinker_token", previousToken);
        System.setProperty("user.dir", previousDir);
    }
    protected void sql(String command) throws Exception {
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) { s.executeUpdate(command); }
    }
}
