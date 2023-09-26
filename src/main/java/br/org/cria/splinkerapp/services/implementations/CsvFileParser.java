package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvFileParser extends FileParser{

    BufferedReader reader;
    String limiter;
    List<String> lines = new ArrayList<String>();
    public CsvFileParser(String filePath) throws Exception
    {
        reader = new BufferedReader(new FileReader(filePath));
        var fullPath = Paths.get(filePath);
        lines = Files.readAllLines(fullPath);
    }
    
    String getCsvSeparator(String firstLine) throws IOException 
    {
        String result = null;
        var searchList =List.of( ",", ";", "\t", "|", ":" );
        int lowestIndex = Integer.MAX_VALUE;
        for (String searchItem : searchList) 
        {
            int index = firstLine.indexOf(searchItem);
            
            if (index != -1 && index < lowestIndex)
            {
                lowestIndex = index;
                    result = searchItem;
            }
        }
        return result;
    }

    @Override
    public List<String> getRowAsStringList() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRowAsStringList'");
    }

    @Override
    protected String buildCreateTableCommand() 
    {
        var tableName = "spLinker";
        List<String> columns = Arrays.stream(lines.get(0).split(limiter)).map((e)-> "%s VARCHAR(1),".formatted(makeColumnName(e))).toList();
        var columnNames = String.join(",", columns);
        var command = "CREATE TABLE IF NOT EXISTS %s (%s)".formatted(tableName,columnNames);
        return command;
    }

    @Override
    protected void extractColumnNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'extractColumnNames'");
    }

    @Override
    protected void readRows() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readRows'");
    }

    @Override
    public void insertData() throws SQLException {
        var tableName = "spLinker";
        var columns =  Arrays.stream(lines.get(0).split(limiter)).map((e)-> makeColumnName(e)).toList();
        var columnNames = String.join(",", columns);
        var rows = lines.subList(1, lines.size()-1);
        var conn = getConnection();
        var commandBase = "INSERT INTO %s (%s) VALUES (%s);";
        var valuesStr= makeValueString(columns.size());
        var command = commandBase.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
        var statement = conn.prepareStatement(command);
        
        for(String row: rows)
        {
            var valuesList = Arrays.stream(row.split(limiter)).map("'%s'"::formatted).toList();
        
            for (int k = 0; k < columns.size(); k++) 
            {
                statement.setString(k+1, valuesList.get(k));    
            }
        }
        statement.executeUpdate();    
        
        throw new UnsupportedOperationException("Unimplemented method 'insertData'");
    }
    
}
