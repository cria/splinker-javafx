package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dhatim.fastexcel.reader.*;
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
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {

        var builder = new StringBuilder();
        sheets.forEach((sheet) -> {
            try {
                var tableName = StringStandards.normalizeString(sheet.getName());
                dropTable(tableName);
                if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return;

                var headerRow = sheet.openStream().findFirst().get();
                builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
                var fieldCount = headerRow.getCellCount();
                for (int i = 0; i < fieldCount; i++) {
                    var cell = headerRow.getCell(i);
                    String cellValue = "";
                    try {
                        cellValue = cell.asString();
                    } catch (Exception e) {
                        if (cell != null) {
                            cellValue = cell.toString();
                        }
                    }
                    if (cellValue != null && cellValue.startsWith("[FORMULA")) {
                        int startIndex = cellValue.indexOf("\"");
                        int endIndex = cellValue.lastIndexOf("\"");
                        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                            cellValue = cellValue.substring(startIndex + 1, endIndex);
                        }
                    }
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
            String cellValue = "";
            if (cell != null) {
                cellValue = cell.getRawValue();
            }
            arr[colNum] = cellValue;
        }

        return Arrays.asList(arr);
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        Connection conn;
        try {
            sheets = wb.getSheets();
            conn = getConnection();
            conn.setAutoCommit(false);
            var woorkbookIterator = sheets.iterator();

            while (woorkbookIterator.hasNext()) {

                var sheet = woorkbookIterator.next();
                var lines = sheet.read();
                var headerRow = lines.get(0);
                var tableName = StringStandards.normalizeString(sheet.getName());
                if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) continue;
                totalColumnCount = headerRow.getCellCount();
                totalRowCount = 0;

                List<String> columns = getRowAsStringList(headerRow, totalColumnCount).stream()
                        .map(this::makeColumnName)
                        .filter(col -> col != null && !col.trim().isEmpty())
                        .toList();

                var valuesStr = "?,".repeat(columns.size());
                var columnNames = String.join(",", columns);
                var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                        .replace(",)", ")");
                var statement = conn.prepareStatement(command);
                //sublist exclui o elemento na posição toIndex;
                var rows = lines.subList(1, lines.size());
                rows.stream().filter(row -> row != null && row.getCellCount() != 0 && !isRowEmpty(row)).forEach((row) ->
                {
                    try {
                        for (int i = 0; i < columns.size(); i++) {
                            Cell cell = null;
                            String valueData = null;
                            try {
                                cell = row.getCell(i);
                                if (CellType.NUMBER.equals(cell.getType())){
                                    double num = Double.parseDouble(cell.getRawValue());
                                    if (num >= 20000 && num <= 50000 && num % 1 == 0) {
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                                        valueData = LocalDate.of(1899, 12, 30)
                                                .plusDays((long) num)
                                                .format(formatter);
                                    }
                                }
                            } catch (Exception ignored) {

                            }

                            if (valueData != null) {
                                statement.setString(i + 1, valueData);
                            } else {
                                var isNullCell = cell == null;
                                var value = isNullCell ? "" : getCellValue(cell.getRawValue());
                                statement.setString(i + 1, value);
                            }
                        }
                        statement.addBatch();
                        currentRow++;
                        totalRowCount ++;
                        if (currentRow % 10_000 == 0) {
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
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            var isNullCell = cell == null;
            var value = isNullCell ? "" : getCellValue(cell.getRawValue());
            if (!value.isEmpty()) {
                return false;
            }
        }

        return true;
    }

}
