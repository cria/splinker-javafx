package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import br.org.cria.splinkerapp.services.interfaces.FileSourceParser;

public class ExcelFileSourceParser extends FileSourceParser{
    String fileSourcePath;
    Workbook workbook;

    public ExcelFileSourceParser(String fileSourcePath) {
        this.fileSourcePath = fileSourcePath;

        try {
            workbook = fileSourcePath.endsWith(".csv") ? 
                            csvToExcel(fileSourcePath) : 
                            new XSSFWorkbook(new FileInputStream(fileSourcePath));

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    protected String buildCreateTableCommand() {
        int numberOfTabs = workbook.getNumberOfSheets();
        var builder = new StringBuilder();
        for (int i = 0; i < numberOfTabs; i++) {
            var sheet = workbook.getSheetAt(i);
            Row headerRow = sheet.getRow(0);
            var tableName = normalizeString(sheet.getSheetName());
            builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));

            for (Cell cell : headerRow) {
                String columnName = normalizeString(cell.getStringCellValue());
                builder.append("%s VARCHAR(1),".formatted(columnName));
            }
            builder.append(");");
        }
        return builder.toString().replace(",);", ");");
    }

    protected String normalizeString(String str) {
        return StringUtils.stripAccents(str.toLowerCase()).replace(" ", "_")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_").trim();
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:splinker.db");
    }

    public void createTableBasedOnSheet() throws SQLException {
        var command = buildCreateTableCommand();
        var conn = getConnection();
        var statement = conn.createStatement();
        var result = statement.executeUpdate(command);
        System.out.println(result);
    }

    protected List<String> getRowAsStringList(Row row, int numberOfCells) {
        var list = new ArrayList<String>();
        var formatter = new DataFormatter();

        for (int colNum = 0; colNum < numberOfCells; colNum++) {

            Cell cell = row.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            var cellValue = formatter.formatCellValue(cell);
            list.add(cellValue);

        }

        return list;

    }

    public void insertDataIntoTable() throws SQLException {
        try {
                int numberOfTabs = workbook.getNumberOfSheets();
                var conn = getConnection();
                for (int i = 0; i < numberOfTabs; i++) {
                    var sheet = workbook.getSheetAt(i);
                    var numberOfRows = sheet.getLastRowNum();
                    var headerRow = sheet.getRow(0);
                    var tableName = normalizeString(sheet.getSheetName());
                    var numberOfColumns = headerRow.getLastCellNum();
                    var columns = getRowAsStringList(headerRow, numberOfColumns).stream().map((col) -> normalizeString(col)).toList();
                    var valuesStr = "?,".repeat(numberOfColumns);
                    var columnNames = String.join(",", columns);
                    for (int j = 1; j < numberOfRows; j++) {
                        var row = sheet.getRow(j);
                        if(row != null) 
                        {
                            var valuesList = getRowAsStringList(row, numberOfColumns).stream().map("'%s'"::formatted).toList();
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
        }


        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    protected Workbook csvToExcel(String csvPath) throws FileNotFoundException, IOException {
        workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("spLinker");
        var reader = new BufferedReader(new FileReader(csvPath));
        var separator = getCsvSeparator(csvPath);
        String line;
        int rowNumber = 0;
        while ((line = reader.readLine()) != null) {
            Row row = sheet.createRow(rowNumber++);
            String[] cells = line.split("%s".formatted(separator));
            int columnNumber = 0;
            for (String cellData : cells) {
                Cell cell = row.createCell(columnNumber++);
                cell.setCellValue(cellData);
            }
        }
        reader.close();
        
        return workbook;
    }

    String getCsvSeparator(String filePath) throws IOException {
        String result = null;
    
        try (var reader = new BufferedReader(new FileReader(filePath))) 
        {
            String firstLine = reader.readLine();
            var searchList =List.of( ",", ";", "\t", "|", ":" );
            int lowestIndex = Integer.MAX_VALUE;
            
            for (String searchItem : searchList) {
                int index = firstLine.indexOf(searchItem);
                if (index != -1 && index < lowestIndex) {
                    lowestIndex = index;
                    result = searchItem;
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return result;
    }
}
