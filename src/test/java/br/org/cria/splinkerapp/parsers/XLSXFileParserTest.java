package br.org.cria.splinkerapp.parsers;

import org.junit.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class XLSXFileParserTest extends ParserBaseTest {
    private XLSXFileParser parser(int count) throws Exception {
        return new XLSXFileParser(SpreadsheetFixtures.create(temp.getRoot().toPath().resolve("source.xlsx"), true, count).toString());
    }
    @Test public void importsTypedCellsAndCountsRows() throws Exception {
        var p = parser(2); importFile(p, null);
        assertEquals(List.of(List.of("Ana", "42", "TRUE"), List.of("Ana", "42", "TRUE")), rows("records"));
        assertEquals(2, p.getTotalRowCount()); assertEquals(2, p.getCurrentRow());
    }
    @Test public void skipsUnselectedSheets() throws Exception {
        var p = parser(1); importFile(p, Set.of("other")); assertEquals(0, p.getCurrentRow());
    }
    @Test public void importsHeaderOnly() throws Exception {
        var p = parser(0); importFile(p, Set.of("records")); assertTrue(rows("records").isEmpty());
    }
    @Test public void commitsAcrossBatchBoundary() throws Exception {
        var p = parser(1001); importFile(p, null); assertEquals(1001, rows("records").size());
    }
    @Test public void handlesSparseHeadersEmptySheetsAndNumericSheetNames() throws Exception {
        var path = temp.getRoot().toPath().resolve("sparse.xlsx");
        try (var w = new XSSFWorkbook()) {
            w.createSheet("Empty");
            var sheet = w.createSheet("123 Records");
            var header = sheet.createRow(0); header.createCell(0).setCellValue("Name"); header.createCell(27).setCellValue("Place");
            sheet.createRow(1).createCell(27).setCellValue("Brasil");
            sheet.createRow(2).createCell(0).setCellValue(" ");
            try (var out = Files.newOutputStream(path)) { w.write(out); }
        }
        var p = new XLSXFileParser(path.toString()); importFile(p, null);
        assertEquals(List.of(List.of("", "Brasil")), rows("_123_records"));
    }
    @Test public void convertsEveryCellTypeWithoutScientificNotation() throws Exception {
        var p = parser(0);
        try (var w = new XSSFWorkbook()) {
            var r = w.createSheet().createRow(0); var c = r.createCell(0);
            assertEquals("", p.getCellStringValue(null)); assertEquals("", p.getCellStringValue(c));
            c.setCellValue(" A\nB "); assertEquals("A B", p.getCellStringValue(c));
            c.setCellValue(true); assertEquals("true", p.getCellStringValue(c));
            c.setCellValue(1234567890123d); assertEquals("1234567890123", p.getCellStringValue(c));
            c.setCellValue(1.25); assertEquals("1.25", p.getCellStringValue(c));
            c.setCellValue(java.sql.Date.valueOf("2024-02-29"));
            var style = w.createCellStyle(); style.setDataFormat(w.createDataFormat().getFormat("m/d/yy")); c.setCellStyle(style);
            assertEquals("29/02/2024", p.getCellStringValue(c));
            c.setCellStyle(w.createCellStyle()); c.setCellErrorValue(FormulaError.DIV0.getCode());
            assertEquals("#DIV/0!", p.getCellStringValue(c));
            c.setCellFormula("1+2"); w.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(c);
            assertEquals("3", p.getCellStringValue(c));
            assertEquals(List.of("3", ""), p.getRowAsStringList(r, 2));
        }
        assertFalse(XLSXFileParser.startsWithNumber(null)); assertFalse(XLSXFileParser.startsWithNumber(""));
        assertFalse(XLSXFileParser.startsWithNumber("A1")); assertTrue(XLSXFileParser.startsWithNumber("1A"));
    }
    @Test public void rejectsMalformedWorkbook() throws Exception {
        var p = new XLSXFileParser(text("bad.xlsx", "invalid").toString());
        assertThrows(Exception.class, () -> p.createTableBasedOnSheet(null));
        assertThrows(RuntimeException.class, () -> p.insertDataIntoTable(null));
    }
}
