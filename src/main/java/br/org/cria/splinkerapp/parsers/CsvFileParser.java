package br.org.cria.splinkerapp.parsers;
import java.io.FileReader;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CsvFileParser extends FileParser 
{
    CsvParser parser;
    List<String> columns;
    List<String[]> rows;
    String filePath;
    public CsvFileParser(String filePath) throws Exception
    {

        this.filePath = filePath;
        var settings = new CsvParserSettings();
        settings.setDelimiterDetectionEnabled(true);
        var parser = new CsvParser(settings);
        var fileContent = parser.parseAll(new FileReader(filePath));
        columns = normalizeAllColumns(Arrays.asList(fileContent.get(0)));
        rows = fileContent.subList(1, fileContent.size());
    }

    List<String> normalizeAllColumns(List<String> columnList)
    {
        return columnList.stream().map((e)-> makeColumnName(e))
                .filter(e -> e != null).toList();
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
        return row == null? null : Arrays.asList((String[])row);
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
    public void insertDataIntoTable() throws Exception 
    {
        var conn = getConnection();
        var tableName = getTableName();
        var valuesStr= makeValueString(columns.size());
        var columnNames = String.join(",", columns);
        var columnCount = columns.size();

        for (var row: rows) 
            {
                var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                                                    .replace(",)", ")");
                var statement = conn.prepareStatement(command);
                for (int j = 0; j < columnCount; j++) 
                {
                    var value = row[j];
                    statement.setString(j + 1, value);
                }
                statement.executeUpdate();
                statement.close();   
            }
        conn.close();
    }
}