package br.org.cria.splinkerapp.parsers;

import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class XLSXFileParser extends FileParser {

    private Workbook workbook;
    private String fileSourcePath;
    private FormulaEvaluator evaluator;
    private final DataFormatter formatter;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");

    public XLSXFileParser(String fileSourcePath) throws Exception {
        super();
        this.formatter = new DataFormatter(Locale.getDefault());
        this.fileSourcePath = fileSourcePath;
        org.apache.poi.openxml4j.util.ZipSecureFile.setMinInflateRatio(0.0);
    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        try (FileInputStream fis = new FileInputStream(fileSourcePath)) {
            workbook = new XSSFWorkbook(fis);
        }
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
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
    public String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        try {
            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    evaluator.evaluateFormulaCell(cell);
                } catch (RuntimeException ignore) { /* usa valor em cache */ }
            }

            final CellType effectiveType = (cell.getCellType() == CellType.FORMULA)
                    ? cell.getCachedFormulaResultType()
                    : cell.getCellType();

            switch (effectiveType) {
                case STRING:
                    return getCellValue(cell.getStringCellValue());

                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return dateFmt.format(cell.getDateCellValue());
                    }

                    String formatted = formatter.formatCellValue(cell, evaluator);
                    try {
                        double raw = cell.getNumericCellValue();
                        return formatNumericNoSci(raw);
                    } catch (Exception e) {
                        return formatted;
                    }

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                case ERROR:
                    byte code = cell.getErrorCellValue();
                    FormulaError fe = FormulaError.forInt(code);
                    return fe != null ? fe.getString() : "#ERROR";

                case BLANK:
                default:
                    String v = formatter.formatCellValue(cell, evaluator);
                    return v != null ? v : "";
            }
        } catch (Exception e) {
            try {
                return formatter.formatCellValue(cell, evaluator);
            } catch (Exception ignore) {
                return "";
            }
        }
    }

    private String formatNumericNoSci(double d) {
        BigDecimal bd = BigDecimal.valueOf(d);
        if (bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0) {
            // inteiro
            return bd.setScale(0, RoundingMode.DOWN).toPlainString();
        }
        return bd.stripTrailingZeros().toPlainString();
    }


}
