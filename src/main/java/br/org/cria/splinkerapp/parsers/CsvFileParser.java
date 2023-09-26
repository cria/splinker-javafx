package br.org.cria.splinkerapp.parsers;

import java.io.FileReader;
import java.sql.SQLException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CsvFileParser extends FileParser {

    CSVReader  reader;
    String limiter;
    String[] columnRow;
    public CsvFileParser(String filePath) throws Exception
    {
        reader = new CSVReader(new FileReader(filePath));
        columnRow = reader.readNext();
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
    public List<String> getRowAsStringList(Object row, int numberOfColumns) 
    {
        String[] fullRow = (String[]) row;
        return Arrays.asList(fullRow);
    }

    @Override
    protected String buildCreateTableCommand() 
    {
        var tableName = "spLinker";
        List<String> columns = getRowAsStringList(columnRow, columnRow.length).stream().map((e)-> "%s VARCHAR(1),".formatted(makeColumnName(e))).toList();
        var columnNames = String.join(",", columns);
        var command = "CREATE TABLE IF NOT EXISTS %s (%s)".formatted(tableName,columnNames);
        return command;
    }

    @Override
    public void insertDataIntoTable() throws SQLException, CsvValidationException, IOException {
        String[] nextLine;
        var conn = getConnection();
        var tableName = "spLinker";
        var valuesStr= makeValueString(columnRow.length);
        var commandBase = "INSERT INTO %s (%s) VALUES (%s);";
        var columns = getRowAsStringList(columnRow, columnRow.length).stream().map((e)->makeColumnName(e)).toList();
        var columnNames = String.join(",", columns);
        while ((nextLine = reader.readNext()) != null) 
        {
            var command = commandBase.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
            var statement = conn.prepareStatement(command);
            for (int i = 0; i < columnRow.length; i++) 
            {
                statement.setString(i + 1, nextLine[i]);
            }
            statement.executeUpdate();
        }
        conn.close();
    }
}
