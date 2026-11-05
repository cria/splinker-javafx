package br.org.cria.splinkerapp.parsers;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.Assert.*;

/** Real temporary SQLite destination; never touches the user's database. */
public abstract class ParserBaseTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    protected String url;
    private String previous;
    @Before public void isolateDatabase() {
        previous = System.getProperty("splinker.dbname");
        url = "jdbc:sqlite:" + temp.getRoot().toPath().resolve("destination.db");
        System.setProperty("splinker.dbname", url);
    }
    @After public void restoreDatabase() {
        if (previous == null) System.clearProperty("splinker.dbname");
        else System.setProperty("splinker.dbname", previous);
    }
    protected List<List<String>> rows(String table) throws Exception {
        var rows = new ArrayList<List<String>>();
        try (var c = DriverManager.getConnection(url); var s = c.createStatement();
             var r = s.executeQuery("SELECT * FROM " + table)) {
            while (r.next()) {
                var row = new ArrayList<String>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) row.add(r.getString(i));
                rows.add(row);
            }
        }
        return rows;
    }
    protected void importFile(FileParser parser, Set<String> tables) throws Exception {
        parser.createTableBasedOnSheet(tables);
        parser.insertDataIntoTable(tables);
    }
    protected Path text(String name, String content) throws Exception {
        return Files.writeString(temp.getRoot().toPath().resolve(name), content);
    }
}
