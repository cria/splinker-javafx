package br.org.cria.splinkerapp.parsers;

import java.io.FileReader;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CsvFileParser extends FileParser 
{

    CSVReader  reader;
    String limiter;
    List<String> columns;
    String filePath;
    public CsvFileParser(String filePath) throws Exception
    {
        this.filePath = filePath;
        reader = new CSVReader(new FileReader(filePath));
        var columnRow = reader.readNext();
        limiter = getCsvSeparator(columnRow[0]);
        columns = getRowAsStringList(columnRow[0], columnRow.length).stream().map((e)-> makeColumnName(normalizeString(e))).toList();
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
        var fullRow = (String) row;
        var splittedRows = fullRow.split(limiter);
        return Arrays.asList(splittedRows);
    }
    @Override
    protected String buildCreateTableCommand() 
    {
        var tableName = getTableName();
        var columnNames = String.join(",", columns.stream().map((e)-> "%s VARCHAR(1)".formatted(e)).toList());
        var command = createTableCommand.formatted(tableName,columnNames).replace(",)"," );");
        return command;
    }

    @Override
    public void insertDataIntoTable() throws Exception {
        var conn = getConnection();
        var tableName = getTableName();
        var valuesStr= makeValueString(columns.size());
        var columnNames = String.join(",", columns);
        CSVParser csvParser = new CSVParserBuilder().withSeparator(limiter.toCharArray()[0]).build(); // custom separator
        try(CSVReader reader = new CSVReaderBuilder(
                new FileReader(filePath))
                .withCSVParser(csvParser)   // custom CSV parser
                .withSkipLines(1)           // skip the first line, header info
                .build()){
            List<String[]> rows = reader.readAll();
            var rowCount = rows.size();
            var columnCount = columns.size();
            for (int i = 0; i < rowCount; i++) 
            {
                var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
                var statement = conn.prepareStatement(command);
                for (int j = 0; j < columnCount; j++) 
                {
                    statement.setString(j + 1, rows.get(i)[j]);
                }
                statement.executeUpdate();    
            }
        }
        conn.close();
    }
}
