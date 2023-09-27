package br.org.cria.splinkerapp.parsers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

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
        
        getConnection().createStatement().execute("DROP TABLE IF EXISTS %s;".formatted(tableName));
    }
    protected static String createTableCommand = "CREATE TABLE IF NOT EXISTS %s (%s)";
    protected static String insertIntoCommand = "INSERT INTO %s (%s) VALUES (%s);";
    protected Connection getConnection() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:splinker.db"); }
    protected String getTableName(){ return "spLinker_%s".formatted(getClass().getSimpleName().toLowerCase()).replace("fileparser", "");}
    public abstract void insertDataIntoTable() throws Exception;
    protected abstract List<String> getRowAsStringList(Object row, int numberOfColumns);
    protected abstract String buildCreateTableCommand() throws Exception;

    protected String normalizeString(String str) 
    {
        return StringUtils.stripAccents(str.toLowerCase()).trim()//.replace(" ", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_");
    }
    protected String makeValueString(int numberOfColumns) { return "?,".repeat(numberOfColumns); }
    protected String makeColumnName(String originalField) { return "`%s`".formatted(normalizeString(originalField));}
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
