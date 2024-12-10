package br.org.cria.splinkerapp.parsers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.google.common.eventbus.EventBus;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.utils.StringStandards;

public abstract class FileParser {
    protected EventBus readRowEventBus;
    protected int totalRowCount = 0;
    ;
    protected int currentRow = 0;
    protected int totalColumnCount = 0;
    protected final String CONNECTION_STRING = System.getProperty("splinker.dbname", LocalDbManager.getLocalDbConnectionString());

    protected FileParser() throws Exception {
        readRowEventBus = EventBusManager.getEvent(EventTypes.READ_ROW.name());
    }

    public int getTotalRowCount() {
        return totalRowCount;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    protected void dropTable(String tableName) throws Exception {
        if (tableName == null) {
            tableName = getTableName();
        }
        var conn = getConnection();
        var statement = conn.createStatement();
        statement.execute("DROP TABLE IF EXISTS %s;".formatted(tableName));
        statement.close();
        conn.close();
    }

    @SuppressWarnings("null")
    protected String getCellValue(String cell) {
        var isNull = cell == null;
        var hasNullValue = isNull ? true : cell.toLowerCase() != "null";
        var value = hasNullValue ? "" : cell.replace("\r", " ").replace("\t", " ");
        return value;
    }

    protected static String createTableCommand = "CREATE TABLE IF NOT EXISTS %s (%s)";
    protected static String insertIntoCommand = "INSERT INTO %s (%s) VALUES (%s);";

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_STRING);
    }

    protected String getTableName() {
        return "spLinker";
    }

    public abstract void insertDataIntoTable() throws Exception;

    protected abstract List<String> getRowAsStringList(Object row, int numberOfColumns);

    protected abstract String buildCreateTableCommand() throws Exception;

    protected String makeValueString(int numberOfColumns) {
        return "?,".repeat(numberOfColumns);
    }

    protected String makeColumnName(String originalField) {
        var chars = Arrays.asList("", " ", "\t", "\n");
        var isEmptyString = chars.contains(originalField);
        var column = isEmptyString ?
                null : "`%s`".formatted(StringStandards.normalizeString(originalField));
        return column;
    }

    public void createTableBasedOnSheet() throws Exception {
        var command = buildCreateTableCommand();
        var conn = getConnection();
        var statement = conn.createStatement();
        statement.executeUpdate(command);
        statement.close();
        conn.close();
    }
}
