package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;

public class XLSFileParser extends FileParser {
    String fileSourcePath;
    Workbook workbook;

    public XLSFileParser(String fileSourcePath) throws Exception {
        this.fileSourcePath = fileSourcePath;
        var excelFile = new File(fileSourcePath);
        var stream = new FileInputStream(excelFile);
        workbook = new HSSFWorkbook(stream);
    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        int numberOfTabs = workbook.getNumberOfSheets();
        var builder = new StringBuilder();
        for (int i = 0; i < numberOfTabs; i++) {
            var sheet = workbook.getSheetAt(i);
            Row headerRow = sheet.getRow(0);
            var tableName = StringStandards.normalizeString(sheet.getSheetName());
            dropTable(tableName);

            if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) continue;

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
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        Connection conn;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            var woorkbookIterator = workbook.sheetIterator();
            var formatter = new DataFormatter();
            while (woorkbookIterator.hasNext()) {
                var sheet = woorkbookIterator.next();
                totalRowCount = sheet.getLastRowNum();
                var sheetIterator = sheet.iterator();
                var headerRow = sheetIterator.next();
                var tableName = StringStandards.normalizeString(sheet.getSheetName());

                if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) continue;

                headerRow.forEach(e ->
                {
                    if (StringUtils.isEmpty(e.getStringCellValue())) {
                        headerRow.removeCell(e);
                    }
                });
                totalColumnCount = headerRow.getLastCellNum();
                List<String> columns = getRowAsStringList(headerRow, totalColumnCount).stream()
                        .map((col) -> makeColumnName(col))
                        .toList();

                var valuesStr = "?,".repeat(totalColumnCount);
                var columnNames = String.join(",", columns);
                var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                        .replace(",)", ")");
                var statement = conn.prepareStatement(command);
                while (sheetIterator.hasNext()) {
                    var row = sheetIterator.next();
                    if (row != null) {
                        for (int i = 0; i < columns.size(); i++) {
                            Cell cell = null;
                            try {
                                cell = row.getCell(i);
                            } catch (Exception e) {

                            }
                            var isNullCell = cell == null;
                            var value = isNullCell ? "" : getCellValue(formatter.formatCellValue(cell));
                            statement.setString(i + 1, value);
                        }
                        statement.addBatch();
                    }
                    currentRow++;
                    if (currentRow % 10_000 == 0) {
                        statement.executeBatch();
                        conn.commit();
                        statement.clearBatch();

                    }
                    readRowEventBus.post(currentRow);
                }

                statement.executeBatch();
                conn.commit();
                statement.clearBatch();
                statement.close();
            }

            conn.setAutoCommit(true);
            conn.close();
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
