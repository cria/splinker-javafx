package br.org.cria.splinkerapp.parsers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import br.org.cria.splinkerapp.utils.StringStandards;

public abstract class FileParser {
    protected final String CONNECTION_STRING = "jdbc:sqlite:splinker.db";
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
    protected Connection getConnection() throws SQLException { return DriverManager.getConnection(CONNECTION_STRING); }
    //protected String getTableName(){ return "spLinker".formatted(getClass().getSimpleName().toLowerCase()).replace("fileparser", "");}
    protected String getTableName(){ return "spLinker";}
    public abstract void insertDataIntoTable() throws Exception;
    protected abstract List<String> getRowAsStringList(Object row, int numberOfColumns);
    protected abstract String buildCreateTableCommand() throws Exception;

    protected String makeValueString(int numberOfColumns) { return "?,".repeat(numberOfColumns); }
    protected String makeColumnName(String originalField) {
        var chars = Arrays.asList(""," ","\t","\n");
        var isEmptyString = chars.contains(originalField);
        var column = isEmptyString ? 
        null : "`%s`".formatted(StringStandards.normalizeString(originalField));
        return column;}
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
