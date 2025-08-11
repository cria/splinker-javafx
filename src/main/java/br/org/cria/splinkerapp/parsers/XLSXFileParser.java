package br.org.cria.splinkerapp.parsers;

import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class XLSXFileParser extends FileParser {

    private Workbook workbook;
    private String fileSourcePath;

    public XLSXFileParser(String fileSourcePath) throws Exception {
        super();
        this.fileSourcePath = fileSourcePath;
        org.apache.poi.openxml4j.util.ZipSecureFile.setMinInflateRatio(0.0);
    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        try (FileInputStream fis = new FileInputStream(fileSourcePath)) {
            workbook = new XSSFWorkbook(fis);
        }
        StringBuilder builder = new StringBuilder();

        for (Sheet sheet : workbook) {
            String tableName = StringStandards.normalizeString(sheet.getSheetName());
            dropTable(tableName);

            if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) {
                continue;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) continue;

            builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));

            for (Cell cell : headerRow) {
                String cellValue = getCellStringValue(cell);
                if (!StringUtils.isEmpty(cellValue)) {
                    String columnName = makeColumnName(cellValue);
                    builder.append("%s VARCHAR(1),".formatted(columnName));
                }
            }
            builder.append(");");
        }

        return builder.toString().replace(",);", ");");
    }

    @Override
    protected List<String> getRowAsStringList(Object rowObj, int numberOfColumns) {
        Row row = (Row) rowObj;
        String[] arr = new String[numberOfColumns];

        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            Cell cell = row.getCell(colNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            arr[colNum] = getCellStringValue(cell);
        }

        return Arrays.asList(arr);
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            for (Sheet sheet : workbook) {
                String tableName = StringStandards.normalizeString(sheet.getSheetName());
                if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) continue;

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                totalColumnCount = headerRow.getPhysicalNumberOfCells();
                totalRowCount = 0;

                List<String> columns = getRowAsStringList(headerRow, totalColumnCount).stream()
                        .map(this::makeColumnName)
                        .filter(col -> col != null && !col.trim().isEmpty())
                        .collect(Collectors.toList());

                String valuesStr = "?,".repeat(columns.size());
                String columnNames = String.join(",", columns);
                String command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                        .replace(",)", ")");

                var statement = conn.prepareStatement(command);

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null || isRowEmpty(row)) continue;

                    for (int i = 0; i < columns.size(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        statement.setString(i + 1, getCellStringValue(cell));
                    }

                    statement.addBatch();
                    currentRow++;
                    totalRowCount++;

                    if (currentRow % 10_000 == 0) {
                        statement.executeBatch();
                        conn.commit();
                        statement.clearBatch();
                    }
                    readRowEventBus.post(currentRow);
                }

                statement.executeBatch();
                conn.commit();
                statement.close();
            }

            conn.setAutoCommit(true);
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (!getCellStringValue(cell).isEmpty()) return false;
        }
        return true;
    }

    /**
     * Retorna valor da célula como String, detectando datas automaticamente.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        if (numericValue <= Integer.MAX_VALUE && numericValue >= Integer.MIN_VALUE) {
                            return String.valueOf((int) numericValue);
                        } else {
                            return String.valueOf((long) numericValue);
                        }
                    } else {
                        return String.valueOf(numericValue);
                    }
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                return switch (cellValue.getCellType()) {
                    case STRING -> cellValue.getStringValue();
                    case NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            yield cell.getLocalDateTimeCellValue().toString();
                        }
                        yield String.valueOf(cellValue.getNumberValue());
                    }
                    case BOOLEAN -> String.valueOf(cellValue.getBooleanValue());
                    default -> "";
                };
            case BLANK:
            default:
                return "";
        }
    }
}
