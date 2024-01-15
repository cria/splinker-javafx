package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import br.org.cria.splinkerapp.utils.StringStandards;

public class OdsFileParser extends FileParser{
    private String filePath;
    private SpreadSheet spreadSheet;
    public OdsFileParser(String filePath)  throws Exception
    {
        this.filePath = filePath;
        var file = new File(this.filePath);
        //this.spreadSheet = new SpreadSheet(file);
        this.spreadSheet = new SpreadSheet(new FileInputStream(file));
    }

    @Override
    public void insertDataIntoTable() throws Exception 
    {
        int numberOfTabs = spreadSheet.getSheets().size();
        var conn = getConnection();
        for (int i = 0; i < numberOfTabs; i++) 
        {
            var sheet = spreadSheet.getSheet(i);
            totalRowCount = sheet.getMaxRows();
            var tableName = StringStandards.normalizeString(sheet.getName());
            var columns = new ArrayList<String>();
            var numberOfColumns = sheet.getMaxColumns();
            IntStream.range(0, numberOfColumns).forEach(n ->
            { 
                var field = sheet.getRange(0, n);
                var value = field.getValue();
                if(value!= null)
                {
                    columns.add(makeColumnName(value.toString()));
                }
                
            });
            numberOfColumns = columns.size(); // número real de colunas
            var valuesStr = makeValueString(numberOfColumns);
            
            var columnNames = String.join(",", columns);
  
            for (int j = 1; j < totalRowCount; j++) 
            {
                currentRow = j;
                var sheetRow = new ArrayList<Range>();
                IntStream.range(0, numberOfColumns).forEach(n -> sheetRow.add(sheet.getRange(currentRow, n)));
                var row = getRowAsStringList(sheetRow, numberOfColumns);
                var valuesList = row.stream().toList();
                var commandBase = "INSERT INTO %s (%s) VALUES (%s);";
                var command = commandBase.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
                var statement = conn.prepareStatement(command);
                    
                for (int k = 0; k < valuesList.size(); k++) 
                {
                    statement.setString(k+1, valuesList.get(k));    
                }
                statement.executeUpdate();
                statement.close();  
                readRowEventBus.post(currentRow);                      
            }
        }
        conn.close();
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) 
    {
        
        final var fullRow = (List<Range>) row;
        var list = new ArrayList<String>();
        for (int colNum = 0; colNum < numberOfColumns; colNum++) 
        {
            var column = fullRow.get(colNum);
            var value = column.getValue();
            list.add(value == null? "": value.toString());
        }

        return list;

    }
    @Override
    protected String buildCreateTableCommand() throws Exception
    {
            var builder = new StringBuilder();
            for (Sheet sheet : spreadSheet.getSheets()) 
            {
                var numberOfColumns = sheet.getMaxColumns();
                var tableName = StringStandards.normalizeString(sheet.getName());
                dropTable(tableName);
                builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
 
                for (int i = 0; i < numberOfColumns; i++) 
                {
                    var column = sheet.getRange(0, i);
                    var value = column.getValue();
                    if(value != null)
                    {
                        String columnName = makeColumnName(value.toString());
                        builder.append("%s VARCHAR(1),".formatted(columnName));
                    }
                    
                }
                builder.append(");");
            }
        var command = builder.toString().replace(",);", ");");
        return command;
    }
}
