package br.org.cria.splinkerapp.parsers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.exceptions.CsvValidationException;

public abstract class FileParser {
    protected String tableName;
    protected Connection getConnection() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:splinker.db"); }
    protected String getTableName(){ return "spLinker_%s".formatted(getClass().getSimpleName());}
    public abstract void insertDataIntoTable() throws SQLException, CsvValidationException, IOException;
    protected abstract List<String> getRowAsStringList(Object row, int numberOfColumns);
    protected abstract String buildCreateTableCommand();

    protected String normalizeString(String str) 
    {
        return StringUtils.stripAccents(str.toLowerCase()).replace(" ", "_")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_").trim();
    }
    protected String makeValueString(int numberOfColumns) { return "?,".repeat(numberOfColumns); }
    protected String makeColumnName(String originalField) { return "`%s`".formatted(normalizeString(originalField));}
    public void createTableBasedOnSheet() throws SQLException {
        var command = buildCreateTableCommand();
        var conn = getConnection();
        var statement = conn.createStatement();
        var result = statement.executeUpdate(command);
        System.out.println(result);
    }


    
}
