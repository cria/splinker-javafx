package br.org.cria.splinkerapp.parsers;

import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelFileParser extends FileParser {
    String fileSourcePath;
    Workbook workbook;

    public ExcelFileParser(String fileSourcePath) throws Exception {
        this.fileSourcePath = fileSourcePath;
        workbook = new XSSFWorkbook(new FileInputStream(fileSourcePath));
    }

    @Override
    protected String buildCreateTableCommand() throws Exception
    {
        int numberOfTabs = workbook.getNumberOfSheets();
        var builder = new StringBuilder();
        for (int i = 0; i < numberOfTabs; i++) 
        {
            var sheet = workbook.getSheetAt(i);
            Row headerRow = sheet.getRow(0);
            var tableName = normalizeString(sheet.getSheetName());
            dropTable(tableName);
            builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));

            for (Cell cell : headerRow) 
            {
                String columnName = makeColumnName(cell.getStringCellValue());
                builder.append("%s VARCHAR(1),".formatted(columnName));
            }
            builder.append(");");
        }
        var command = builder.toString().replace(",);", ");");
        return command;
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) 
    {
        var fullRow = (Row) row;
        var list = new ArrayList<String>();
        var formatter = new DataFormatter();

        for (int colNum = 0; colNum < numberOfColumns; colNum++) 
        {

            Cell cell = fullRow.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            var cellValue = formatter.formatCellValue(cell);
            list.add(cellValue);
        }
        return list;

    }

    @Override
    public void insertDataIntoTable() throws Exception 
    {

        int numberOfSheets = workbook.getNumberOfSheets();
        var conn = getConnection();
        for (int i = 0; i < numberOfSheets; i++) 
        {
            var sheet = workbook.getSheetAt(i);
            var numberOfRows = sheet.getLastRowNum();
            var headerRow = sheet.getRow(0);
            var tableName = normalizeString(sheet.getSheetName());
            var numberOfColumns = headerRow.getLastCellNum();
            var columns = getRowAsStringList(headerRow, numberOfColumns).stream().map((col) -> makeColumnName(col))
                    .toList();
            var valuesStr = "?,".repeat(numberOfColumns);
            var columnNames = String.join(",", columns);
            for (int j = 1; j < numberOfRows; j++) 
            {
                var row = sheet.getRow(j);
                if (row != null) 
                {
                    var valuesList = getRowAsStringList(row, numberOfColumns).stream().map("'%s'"::formatted).toList();
                    var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
                    var statement = conn.prepareStatement(command);

                    for (int k = 0; k < valuesList.size(); k++) 
                    {
                        statement.setString(k + 1, valuesList.get(k));
                    }
                    statement.executeUpdate();
                    statement.close();
                }

            }
            conn.close();
        }
    }

}
