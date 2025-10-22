package br.org.cria.splinkerapp.parsers;

import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.ILogger;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;
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
        StringBuilder builder = new StringBuilder();

        ZipSecureFile.setMinInflateRatio(0.0);

        try (OPCPackage pkg = OPCPackage.open(fileSourcePath)) {
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            DataFormatter formatter = new DataFormatter(Locale.getDefault());

            XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (it.hasNext()) {
                try (InputStream sheetIs = it.next()) {
                    String rawSheetName = it.getSheetName();
                    String tableName = StringStandards.normalizeString(rawSheetName);

                    if (startsWithNumber(tableName)) {
                        tableName = "_" + tableName;
                    }

                    if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) {
                        continue;
                    }

                    List<String> headerCells = readHeaderRowSax(styles, sst, formatter, sheetIs);
                    if (headerCells == null || headerCells.isEmpty()) continue;

                    dropTable(tableName);

                    builder.append("CREATE TABLE IF NOT EXISTS ")
                            .append(tableName)
                            .append(" (");

                    for (String cellValue : headerCells) {
                        if (!StringUtils.isEmpty(cellValue)) {
                            String columnName = makeColumnName(cellValue);
                            if (org.apache.commons.lang3.StringUtils.isNotBlank(columnName)) {
                                builder.append(columnName).append(" VARCHAR(1),");
                            }
                        }
                    }
                    builder.append(");");
                }
            }
        }
        return builder.toString().replace(",);", ");");
    }

    public static boolean startsWithNumber(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return Character.isDigit(value.charAt(0));
    }

    private List<String> readHeaderRowSax(StylesTable styles,
                                          ReadOnlySharedStringsTable sst,
                                          DataFormatter formatter,
                                          InputStream sheetInputStream) throws Exception {
        List<String> header = new ArrayList<>();

        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(new XSSFSheetXMLHandler(
                styles, null, sst,
                new XSSFSheetXMLHandler.SheetContentsHandler() {
                    int lastCol = -1;
                    List<String> currentRow;

                    @Override
                    public void startRow(int rowNum) {
                        currentRow = new ArrayList<>();
                        lastCol = -1;
                    }

                    @Override
                    public void endRow(int rowNum) {
                        if (rowNum == 0) {
                            header.addAll(currentRow);
                            throw new RuntimeException();
                        }
                    }

                    @Override
                    public void cell(String cellRef, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
                        int colIndex = colIndexFromCellRef(cellRef);
                        for (int i = lastCol + 1; i < colIndex; i++) currentRow.add("");
                        currentRow.add(formattedValue == null ? "" : formattedValue);
                        lastCol = colIndex;
                    }

                    @Override
                    public void headerFooter(String text, boolean isHeader, String tagName) {}

                    private int colIndexFromCellRef(String ref) {
                        int idx = 0;
                        for (int i = 0; i < ref.length(); i++) {
                            char ch = ref.charAt(i);
                            if (Character.isLetter(ch)) idx = idx * 26 + (Character.toUpperCase(ch) - 'A' + 1);
                            else break;
                        }
                        return idx - 1;
                    }
                },
                formatter,
                false
        ));

        try {
            parser.parse(new InputSource(sheetInputStream));
        } catch (RuntimeException stop) {
            // esperado: paramos logo após a primeira linha
        }
        return header;
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
        final int BATCH_SIZE = 1_000;

        ZipSecureFile.setMinInflateRatio(0.0);

        try (Connection conn = getConnection(); OPCPackage pkg = OPCPackage.open(fileSourcePath)) {
            conn.setAutoCommit(false);

            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            DataFormatter formatter = new DataFormatter();

            XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (it.hasNext()) {
                try (InputStream sheetIs = it.next()) {
                    String rawSheetName = it.getSheetName();
                    String tableName = StringStandards.normalizeString(rawSheetName);

                    if (startsWithNumber(tableName)) {
                        tableName = "_" + tableName;
                    }

                    if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) {
                        continue;
                    }

                    DataInsertSaxHandler handler = new DataInsertSaxHandler(
                            tableName, conn, formatter, BATCH_SIZE
                    );

                    XMLReader parser = XMLHelper.newXMLReader();
                    parser.setContentHandler(new XSSFSheetXMLHandler(
                            styles, null, sst, handler, formatter, false
                    ));
                    parser.parse(new InputSource(sheetIs));

                    handler.finish();
                    conn.commit();
                }
            }

            conn.setAutoCommit(true);
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class DataInsertSaxHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final String tableName;
        private final Connection conn;
        private final DataFormatter formatter;
        private final int batchSize;

        private List<String> currentRow;
        private int lastCol = -1;
        private boolean headerProcessed = false;

        private List<String> columns;
        private List<Integer> headerIndexes;
        private java.sql.PreparedStatement ps;
        private int countInBatch = 0;

        DataInsertSaxHandler(String tableName, Connection conn, DataFormatter formatter, int batchSize) {
            this.tableName = tableName;
            this.conn = conn;
            this.formatter = formatter;
            this.batchSize = batchSize;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = new ArrayList<>();
            lastCol = -1;
        }

        @Override
        public void endRow(int rowNum) {
            try {
                if (!headerProcessed && rowNum == 0) {
                    totalColumnCount = currentRow.size();

                    headerIndexes = new ArrayList<>();
                    columns = new ArrayList<>();
                    for (int idx = 0; idx < currentRow.size(); idx++) {
                        String raw = currentRow.get(idx);
                        if (raw == null || raw.isBlank()) continue;
                        String col = makeColumnName(raw);
                        if (col != null && !col.isBlank()) {
                            headerIndexes.add(idx);
                            columns.add(col);
                        }
                    }

                    if (columns.isEmpty()) {
                        headerProcessed = true;
                        return;
                    }

                    String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
                    String columnNames = String.join(",", columns);
                    String command = insertIntoCommand
                            .formatted(tableName, columnNames, placeholders)
                            .replace(",)", ")");

                    ps = conn.prepareStatement(command);
                    headerProcessed = true;
                    countInBatch = 0;
                    totalRowCount = 0;
                    return;
                }

                if (!headerProcessed || columns == null || columns.isEmpty()) return;

                if (isCurrentRowEmpty()) return;

                for (int i = 0; i < headerIndexes.size(); i++) {
                    int srcIdx = headerIndexes.get(i);
                    String v = (srcIdx < currentRow.size()) ? currentRow.get(srcIdx) : "";
                    ps.setString(i + 1, v);
                }

                ps.addBatch();
                countInBatch++;
                XLSXFileParser.this.currentRow++;
                XLSXFileParser.this.totalRowCount++;
                readRowEventBus.post(XLSXFileParser.this.currentRow);

                if (countInBatch >= batchSize) {
                    ps.executeBatch();
                    conn.commit();
                    ps.clearBatch();
                    countInBatch = 0;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void cell(String cellRef, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
            int colIndex = colIndexFromCellRef(cellRef);
            for (int i = lastCol + 1; i < colIndex; i++) currentRow.add("");
            currentRow.add(formattedValue == null ? "" : formattedValue);
            lastCol = colIndex;
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {}

        void finish() throws Exception {
            if (ps != null) {
                ps.executeBatch();
                ps.close();
            }
        }

        private boolean isCurrentRowEmpty() {
            if (currentRow == null || currentRow.isEmpty()) return true;
            for (String v : currentRow) {
                if (v != null && !v.isBlank()) return false;
            }
            return true;
        }

        private int colIndexFromCellRef(String ref) {
            int idx = 0;
            for (int i = 0; i < ref.length(); i++) {
                char ch = ref.charAt(i);
                if (Character.isLetter(ch)) idx = idx * 26 + (Character.toUpperCase(ch) - 'A' + 1);
                else break;
            }
            return idx - 1;
        }
    }


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
