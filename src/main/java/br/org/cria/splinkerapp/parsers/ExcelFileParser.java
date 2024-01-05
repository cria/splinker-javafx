package br.org.cria.splinkerapp.parsers;

import java.io.FileInputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import br.org.cria.splinkerapp.utils.StringStandards;

public class ExcelFileParser extends FileParser {
    String fileSourcePath;
    Workbook workbook;

    public ExcelFileParser(String fileSourcePath) throws Exception {
        this.fileSourcePath = fileSourcePath;
        var isXLSX = fileSourcePath.endsWith(".xlsx"); 
        workbook = isXLSX ? new XSSFWorkbook(OPCPackage.open(fileSourcePath)) : 
        new HSSFWorkbook(new FileInputStream(fileSourcePath));
       
    }

    @Override
    protected String buildCreateTableCommand() throws Exception {
        int numberOfTabs = workbook.getNumberOfSheets();
        var builder = new StringBuilder();
        for (int i = 0; i < numberOfTabs; i++) {
            var sheet = workbook.getSheetAt(i);
            Row headerRow = sheet.getRow(0);
            var tableName = StringStandards.normalizeString(sheet.getSheetName());
            dropTable(tableName);
            builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));

            for (Cell cell : headerRow) {
                var cellValue = cell.getStringCellValue();
                if (!StringUtils.isEmpty(cellValue)) {
                    String columnName = makeColumnName(cellValue);
                    builder.append("%s VARCHAR(1),".formatted(columnName));
                }

            }
            builder.append(");");
        }
        var command = builder.toString().replace(",);", ");");
        return command;
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        var fullRow = (Row) row;
        var list = new ArrayList<String>();
        var formatter = new DataFormatter();

        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            Cell cell = fullRow.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            var cellValue = formatter.formatCellValue(cell);
            list.add(cellValue);
        }

        return list;

    }

    @Override
    public void insertDataIntoTable() throws Exception 
    {
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
        Connection conn;
          try 
          {
            conn = getConnection();
            conn.setAutoCommit(false);
            var woorkbookIterator = workbook.sheetIterator();
            var formatter = new DataFormatter();
            while (woorkbookIterator.hasNext()) 
            {
                var sheet = woorkbookIterator.next();
                var sheetIterator = sheet.iterator();
                var headerRow = sheetIterator.next();
                var tableName = StringStandards.normalizeString(sheet.getSheetName());
                headerRow.forEach(e -> 
                {
                    if (StringUtils.isEmpty(e.getStringCellValue())) 
                    {
                        headerRow.removeCell(e);
                    }
                });
                int numberOfColumns = headerRow.getLastCellNum();
                //int numberOfRows = sheet.getLastRowNum() + 1;
                List<String> columns = getRowAsStringList(headerRow, numberOfColumns).stream()
                        .map((col) -> makeColumnName(col))
                        .toList();
                var valuesStr = "?,".repeat(numberOfColumns);
                var columnNames = String.join(",", columns);
                var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                                .replace(",)", ")");
                var statement = conn.prepareStatement(command);
                var rowIndex = 0;
                while (sheetIterator.hasNext()) 
                {
                    rowIndex++;
                    var row = sheetIterator.next();
                    if (row != null) 
                    { 
                        var cellIterator = row.cellIterator();
                        while (cellIterator.hasNext()) 
                        {
                            var cell = cellIterator.next();
                            var index = cell.getColumnIndex() + 1;
                            var value = formatter.formatCellValue(cell);
                            statement.setString(index, value);
                        }
                        statement.addBatch();
                    }
                    if (rowIndex % 10 == 0) 
                    {
                        statement.executeBatch();
                        conn.commit();
                        statement.clearBatch();
                    }
                }
                statement.close();
            }
            
            conn.setAutoCommit(true);
            conn.close();
          }catch (Exception e) 
          {
            
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        });
        completableFuture.get();
    }
}
