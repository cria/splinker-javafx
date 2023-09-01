package br.org.cria.splinkerapp.services.implementations;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class FileSourceParser {
    String fileSourcePath;
    Workbook workbook;

    public FileSourceParser(String fileSourcePath) {
        this.fileSourcePath = fileSourcePath;

        try {
            workbook = new XSSFWorkbook(new FileInputStream(fileSourcePath));

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    String buildCreateTableCommand() {
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

    String normalizeString(String str) {
        var replacedStr = str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        var normalizedStr = Normalizer.normalize(replacedStr, Normalizer.Form.NFD);
        var replacedAndNormalizedStr = normalizedStr.replaceAll("[^a-zA-Z0-9]+", "_").trim();

        return replacedAndNormalizedStr;
    }

    Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:splinker.db");
    }

   public void createTableBasedOnSheet() throws SQLException {
        var command = buildCreateTableCommand();
        var conn = getConnection();
        var statement = conn.createStatement();
        var result = statement.executeUpdate(command);
        System.out.println(result);
    }

    List<String> getRowAsStringList(Row row) {
        var list = new ArrayList<String>();
        var formatter = new DataFormatter();
        var numberOfCells = 81;//row.getPhysicalNumberOfCells();
        

        for (int colNum = 0; colNum < numberOfCells; colNum++) {

            Cell cell = row.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            var cellValue = formatter.formatCellValue(cell);
            list.add(cellValue);
         
         }

        return list;

    }

   public void insertDataIntoTable() throws SQLException {
        int numberOfTabs = workbook.getNumberOfSheets();
        var conn = getConnection();
        for (int i = 0; i < numberOfTabs; i++) {
            var sheet = workbook.getSheetAt(i);
            var numberOfRows = sheet.getLastRowNum();
            sheet.getPhysicalNumberOfRows();
            var headerRow = sheet.getRow(0);
            var tableName = normalizeString(sheet.getSheetName());
            var columns =  getRowAsStringList(headerRow).stream().map((col)-> normalizeString(col)).toList();
            var columnNames = String.join(",", columns);
            for (int j = 1; j < numberOfRows; j++) {
                var row = sheet.getRow(j);
                var valuesList = getRowAsStringList(row).stream().map("'%s'"::formatted).toList();
                var values = String.join(",", valuesList);
                var commandBase = "INSERT INTO %s (%s) VALUES (%s);";
                var command = commandBase.formatted(tableName, columnNames, values).replace(",)", ")");
                var statement = conn.createStatement();
                var result = statement.executeUpdate(command);

            }
        }
    }
}
