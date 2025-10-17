package br.org.cria.splinkerapp.parsers;

import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
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

    private final String fileSourcePath;
    private final DataFormatter formatter;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");

    private static final int BATCH_SIZE = 1_000;

    public XLSXFileParser(String fileSourcePath) throws Exception {
        super();
        this.fileSourcePath = fileSourcePath;
        this.formatter = new DataFormatter(Locale.getDefault());
        ZipSecureFile.setMinInflateRatio(0.0);
    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        StringBuilder builder = new StringBuilder();

        try (OPCPackage pkg = OPCPackage.open(fileSourcePath)) {
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();

            XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (it.hasNext()) {
                try (InputStream sheetIs = it.next()) {
                    String rawSheetName = it.getSheetName();
                    final String tableName = StringStandards.normalizeString(rawSheetName);

                    if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) {
                        continue;
                    }

                    HeaderOnlyHandler headerHandler = new HeaderOnlyHandler();
                    parseSheet(styles, sst, sheetIs, headerHandler);

                    List<String> headerCells = headerHandler.getHeaderCells();
                    if (headerCells == null || headerCells.isEmpty()) {
                        continue;
                    }

                    dropTable(tableName);

                    builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
                    for (String cellValue : headerCells) {
                        if (!StringUtils.isEmpty(cellValue)) {
                            String columnName = makeColumnName(cellValue);
                            if (StringUtils.isNotBlank(columnName)) {
                                builder.append("%s VARCHAR(1),".formatted(columnName));
                            }
                        }
                    }
                    builder.append(");");
                } catch (StopParsingSheetException ignore) {
                    // usado para interromper parsing do sheet após o header
                }
            }
        }

        return builder.toString().replace(",);", ");");
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        try (Connection conn = getConnection(); OPCPackage pkg = OPCPackage.open(fileSourcePath)) {
            conn.setAutoCommit(false);

            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();

            XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (it.hasNext()) {
                try (InputStream sheetIs = it.next()) {
                    String rawSheetName = it.getSheetName();
                    final String tableName = StringStandards.normalizeString(rawSheetName);

                    if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) {
                        continue;
                    }

                    DataInsertHandler handler = new DataInsertHandler(tableName, conn);
                    parseSheet(styles, sst, sheetIs, handler);

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


    private static class StopParsingSheetException extends SAXException {
        StopParsingSheetException() { super("Stop after header"); }
    }

    private class HeaderOnlyHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private List<String> currentRow;
        private List<String> headerCells;
        private int lastCol = -1;

        List<String> getHeaderCells() { return headerCells; }

        @Override
        public void startRow(int rowNum) {
            currentRow = new ArrayList<>();
            lastCol = -1;
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) {
                headerCells = currentRow;
                try {
                    throw new StopParsingSheetException();
                } catch (StopParsingSheetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
            int colIndex = colIndexFromCellRef(cellReference);
            for (int i = lastCol + 1; i < colIndex; i++) currentRow.add("");
            currentRow.add(formattedValue == null ? "" : formattedValue);
            lastCol = colIndex;
        }

        @Override public void headerFooter(String text, boolean isHeader, String tagName) {}

        private int colIndexFromCellRef(String cellRef) {
            int idx = 0;
            for (int i = 0; i < cellRef.length(); i++) {
                char ch = cellRef.charAt(i);
                if (Character.isLetter(ch)) idx = idx * 26 + (Character.toUpperCase(ch) - 'A' + 1);
                else break;
            }
            return idx - 1;
        }
    }

    private class DataInsertHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final String tableName;
        private final Connection conn;

        private List<String> currentRow;
        private List<String> columns;
        private java.sql.PreparedStatement ps;
        private int countInBatch = 0;
        private boolean headerProcessed = false;
        private int lastCol = -1;

        DataInsertHandler(String tableName, Connection conn) {
            this.tableName = tableName;
            this.conn = conn;
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
                    columns = currentRow.stream()
                            .map(XLSXFileParser.this::makeColumnName)
                            .filter(col -> col != null && !col.isBlank())
                            .collect(Collectors.toList());

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

                int size = columns.size();
                for (int i = 0; i < size; i++) {
                    String v = (i < currentRow.size() ? currentRow.get(i) : "");
                    ps.setString(i + 1, v);
                }
                ps.addBatch();
                countInBatch++;
                currentRow();

                if (countInBatch >= BATCH_SIZE) {
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
        public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
            int colIndex = colIndexFromCellRef(cellReference);
            for (int i = lastCol + 1; i < colIndex; i++) currentRow.add("");
            currentRow.add(formattedValue == null ? "" : formattedValue);
            lastCol = colIndex;
        }

        @Override public void headerFooter(String text, boolean isHeader, String tagName) {}

        void finish() throws Exception {
            if (ps != null) {
                ps.executeBatch();
                ps.close();
            }
        }

        private void currentRow() {
            XLSXFileParser.this.currentRow++;
            XLSXFileParser.this.totalRowCount++;
            readRowEventBus.post(XLSXFileParser.this.currentRow);
        }

        private int colIndexFromCellRef(String cellRef) {
            int idx = 0;
            for (int i = 0; i < cellRef.length(); i++) {
                char ch = cellRef.charAt(i);
                if (Character.isLetter(ch)) idx = idx * 26 + (Character.toUpperCase(ch) - 'A' + 1);
                else break;
            }
            return idx - 1;
        }
    }

    private void parseSheet(StylesTable styles, ReadOnlySharedStringsTable sst, InputStream sheetIs,
                            XSSFSheetXMLHandler.SheetContentsHandler handler) throws Exception {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(new XSSFSheetXMLHandler(styles, null, sst, handler, formatter, false));
        parser.parse(new InputSource(sheetIs));
    }


    @Override
    protected List<String> getRowAsStringList(Object rowObj, int numberOfColumns) {
        Row row = (Row) rowObj;
        String[] arr = new String[numberOfColumns];
        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            org.apache.poi.ss.usermodel.Cell cell = row.getCell(colNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            arr[colNum] = getCellStringValue(cell);
        }
        return Arrays.asList(arr);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (org.apache.poi.ss.usermodel.Cell cell : row) {
            if (!getCellStringValue(cell).isEmpty()) return false;
        }
        return true;
    }

    public String getCellStringValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();

            switch (type) {
                case STRING:
                    return getCellValue(cell.getStringCellValue());
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return dateFmt.format(cell.getDateCellValue());
                    }
                    String formatted = formatter.formatCellValue(cell);
                    try {
                        double raw = cell.getNumericCellValue();
                        return formatNumericNoSci(raw);
                    } catch (Exception e) {
                        return formatted == null ? "" : formatted;
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case ERROR:
                    byte code = cell.getErrorCellValue();
                    FormulaError fe = FormulaError.forInt(code);
                    return fe != null ? fe.getString() : "#ERROR";
                case BLANK:
                default:
                    String v = formatter.formatCellValue(cell);
                    return v != null ? v : "";
            }
        } catch (Exception e) {
            try {
                String v = formatter.formatCellValue(cell);
                return v != null ? v : "";
            } catch (Exception ignore) {
                return "";
            }
        }
    }

    private String formatNumericNoSci(double d) {
        BigDecimal bd = BigDecimal.valueOf(d);
        if (bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0) {
            return bd.setScale(0, RoundingMode.DOWN).toPlainString();
        }
        return bd.stripTrailingZeros().toPlainString();
    }
}
