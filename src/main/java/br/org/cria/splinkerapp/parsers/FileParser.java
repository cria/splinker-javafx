package br.org.cria.splinkerapp.parsers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import br.org.cria.splinkerapp.utils.StringStandards;

public abstract class FileParser {
    protected FileParser() throws Exception
    {
        dropTable(null);
    }
    protected void dropTable(String tableName) throws Exception{
        if(tableName == null)
        {
            tableName = getTableName();
        }
        var conn = getConnection();
        var statement = conn.createStatement();
        statement.execute("DROP TABLE IF EXISTS %s;".formatted(tableName));
        statement.close();
        conn.close();
    }
    protected static String createTableCommand = "CREATE TABLE IF NOT EXISTS %s (%s)";
    protected static String insertIntoCommand = "INSERT INTO %s (%s) VALUES (%s);";
    protected Connection getConnection() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:splinker.db"); }
    protected String getTableName(){ return "spLinker_%s".formatted(getClass().getSimpleName().toLowerCase()).replace("fileparser", "");}
    public abstract void insertDataIntoTable() throws Exception;
    protected abstract List<String> getRowAsStringList(Object row, int numberOfColumns);
    protected abstract String buildCreateTableCommand() throws Exception;

    protected String makeValueString(int numberOfColumns) { return "?,".repeat(numberOfColumns); }
    protected String makeColumnName(String originalField) { return "`%s`".formatted(StringStandards.normalizeString(originalField));}
    public void createTableBasedOnSheet() throws Exception {
        var command = buildCreateTableCommand();
        var conn = getConnection();
        var statement = conn.createStatement();
        var result = statement.executeUpdate(command);
        statement.close();
        conn.close();
        System.out.println(result);
    }


    
}
