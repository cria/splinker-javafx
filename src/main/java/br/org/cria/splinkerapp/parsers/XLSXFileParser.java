package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;

public class XLSXFileParser extends FileParser {

    ReadableWorkbook wb;
    String fileSourcePath;
    Stream<Sheet> sheets;

    public XLSXFileParser(String fileSourchPath) throws Exception {
        super();
        this.fileSourcePath = fileSourchPath;
        var file = new File(fileSourcePath);
        var stream = new FileInputStream(file);
        wb = new ReadableWorkbook(stream);
        sheets = wb.getSheets();
    }

    @Override
    protected String buildCreateTableCommand() throws Exception {

        var builder = new StringBuilder();
        sheets.forEach((sheet) -> {
            try {
                var tableName = StringStandards.normalizeString(sheet.getName());
                dropTable(tableName);
                
                var headerRow = sheet.openStream().findFirst().get();
                builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
                var fieldCount = headerRow.getCellCount();
                for (int i = 0; i < fieldCount; i++) {
                    var cellValue = headerRow.getCell(i).asString();
                    if (!StringUtils.isEmpty(cellValue)) {
                        String columnName = makeColumnName(cellValue);
                        builder.append("%s VARCHAR(1),".formatted(columnName));
                    }
                }
                builder.append(");");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        var command = builder.toString().replace(",);", ");");
        
        return command;
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        var fullRow = (Row) row;
        var arr = new String[numberOfColumns];
        
        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            Cell cell = fullRow.getCell(colNum);
            var cellValue = cell.getRawValue();
            arr[colNum] = cellValue == null? "": cellValue.toString();
        }

        return Arrays.asList(arr);
    }

    @Override
    public void insertDataIntoTable() throws Exception 
    {
        Connection conn;
        try 
        {
            sheets = wb.getSheets();
            conn = getConnection();
            conn.setAutoCommit(false);
            var woorkbookIterator = sheets.iterator();
         
          while (woorkbookIterator.hasNext()) 
          {
            
              var sheet = woorkbookIterator.next();
              var lines = sheet.read();
              var headerRow = lines.get(0);
              var tableName = StringStandards.normalizeString(sheet.getName());
              totalColumnCount = headerRow.getCellCount();
              totalRowCount = lines.size() -1;
              
              List<String> columns = getRowAsStringList(headerRow, totalColumnCount).stream()
                      .map((col) -> makeColumnName(col))
                      .toList();

              var valuesStr = "?,".repeat(totalColumnCount);
              var columnNames = String.join(",", columns);
              var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                              .replace(",)", ")");
              var statement = conn.prepareStatement(command);
              //sublist exclui o elemento na posição toIndex;
              var rows = lines.subList(1, lines.size());
              rows.forEach((row)->
              {
               try {
                if (row != null) 
                { 
                    var cellIterator = row.iterator();
                    var index = 1;
                    while (cellIterator.hasNext()) 
                    {
                        var cell = cellIterator.next();
                        var value = "";
                        if(cell != null)
                        {
                            value = cell.getRawValue();
                        }
                        statement.setString(index, value);
                        index++;
                    }
                    statement.addBatch();
                }
                currentRow++;
                if (currentRow % 10_000 == 0) 
                {
                    statement.executeBatch();
                    conn.commit();
                    statement.clearBatch();
                    
                }
                readRowEventBus.post(currentRow);
                
               } catch (Exception e) {
                throw new RuntimeException(e);
               }
              });
              statement.executeBatch();
              conn.commit();
              statement.clearBatch();
              statement.close();
          }
          
          conn.setAutoCommit(true);
          conn.close();
        }catch (Exception e) 
        {
            Sentry.captureException(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
