package br.org.cria.splinkerapp.services.implementations;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

public class OdsParser {
    private String filePath;
    private Connection conn;
    private SpreadSheet spreadSheet;
    public OdsParser(String filePath) {
        this.filePath = filePath;
        try 
        {
            this.spreadSheet = new SpreadSheet(new File(this.filePath));
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:splinker.db");

        } 
        catch (ClassNotFoundException | SQLException e) 
        {
            e.printStackTrace();
         } 
         catch (IOException e) 
         {
            // TODO Auto-generated catch block
            e.printStackTrace();
            }
    }

    public Service processFiles()
    {
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask()
            {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception
                    {
                        createTables();
                        insertRows();
                        return null;
                    }
                };
            }
        };

    }

    public void createTables() 
    {
        try 
        {
            var builder = new StringBuilder();
            for (Sheet sheet : spreadSheet.getSheets()) 
            {
                var numberOfColumns = sheet.getMaxColumns();
                var tableName = normalizeString(sheet.getName());
                builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
 
                for (int i = 0; i < numberOfColumns; i++) 
                {
                    var column = sheet.getRange(0, i);
                    var value = column.getValue();
                    String columnName = normalizeString(value.toString());
                    builder.append("%s VARCHAR(1),".formatted(columnName));
                }
                builder.append(");");
            }
            var command = builder.toString().replace(",);", ");");
            PreparedStatement pstmt = conn.prepareStatement(command);
                pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String normalizeString(String str) 
    {
        return StringUtils.stripAccents(str.toLowerCase()).replace(" ", "_")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_").trim();
    }


    public void insertRows() throws SQLException {
        try {
            int numberOfTabs = spreadSheet.getSheets().size();
            var conn = getConnection();
            for (int i = 0; i < numberOfTabs; i++) 
            {
                var sheet = spreadSheet.getSheet(i);
                var numberOfRows = sheet.getMaxRows();
                var tableName = normalizeString(sheet.getName());
                var numberOfColumns = sheet.getMaxColumns();
                var columns = getRowAsStringList(sheet, 0, numberOfColumns).stream().map((col) -> normalizeString(col)).toList();
                var valuesStr = "?,".repeat(numberOfColumns);
                var columnNames = String.join(",", columns);
                for (int j = 1; j < numberOfRows; j++) {
                    var row = getRowAsStringList(sheet, j, numberOfColumns);
                    var valuesList = row.stream().map("'%s'"::formatted).toList();
                    var commandBase = "INSERT INTO %s (%s) VALUES (%s);";
                    var command = commandBase.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
                    var statement = conn.prepareStatement(command);
                    
                    for (int k = 0; k < valuesList.size(); k++) 
                    {
                        statement.setString(k+1, valuesList.get(k));    
                    }
                    statement.executeUpdate();    
                    
                    
                }
            }
            closeConnection();
        } 
        catch (IllegalArgumentException e) 
        {
            e.printStackTrace();
        }
    }

    public void closeConnection() 
    {
        try 
        {
            conn.close();
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        }
    }


    protected List<String> getRowAsStringList(Sheet sheet, int rowNumber, int numberOfColumns) {
        var list = new ArrayList<String>();
        for (int colNum = 0; colNum < numberOfColumns; colNum++) 
        {
            var column = sheet.getRange(rowNumber, colNum);
            var value = column.getValue();
            list.add(value == null? "": value.toString());
        }

        return list;

    }

    Connection getConnection() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:splinker.db"); }
}
