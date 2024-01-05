package br.org.cria.splinkerapp.parsers;
import java.io.File;
import java.io.FileReader;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CsvFileParser extends FileParser 
{
    CsvParser parser;
    List<String> columns;
    //Iterator<String[]> rows;
    Iterator<String[]> iterator;
    String filePath;
    public CsvFileParser(String filePath) throws Exception
    {

        this.filePath = filePath;
        var settings = new CsvParserSettings();
        settings.setDelimiterDetectionEnabled(true);
        var parser = new CsvParser(settings);
        iterator = parser.iterate(new File(filePath)).iterator();
        columns = normalizeAllColumns(Arrays.asList(iterator.next()));
      //  rows = fileContent.subList(1, fileContent.size()).iterator();
    }

    List<String> normalizeAllColumns(List<String> columnList)
    {
        return columnList.stream().map((e)-> makeColumnName(e))
                .filter(e -> e != null).toList();
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
        conn.setAutoCommit(false);
        var tableName = getTableName();
        var valuesStr= makeValueString(columns.size());
        var columnNames = String.join(",", columns);
        var columnCount = columns.size();
        var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                                        .replace(",)", ")");
        var statement = conn.prepareStatement(command);
        var index = 0;
        while(iterator.hasNext())
        {
            var row = iterator.next();
            for (int j = 0; j < columnCount; j++) 
            {
                var value = row[j];
                statement.setString(j + 1, value);
            }
            statement.addBatch();
            index++;
            if (index % 10 == 0) 
            {
                    statement.executeBatch();
                    conn.commit();
                    statement.clearBatch();
            }
        }
        conn.setAutoCommit(true);
        conn.close();
    }
}