package br.org.cria.splinkerapp.parsers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.github.miachm.sods.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MultiSheetImportTest extends ParserBaseTest {
    @Parameterized.Parameters(name = "{0}") public static Object[] formats() { return new Object[]{"xls", "xlsx", "ods"}; }
    @Parameterized.Parameter public String format;
    private FileParser parser() throws Exception {
        var path = temp.getRoot().toPath().resolve("multiple." + format);
        if (format.equals("ods")) {
            var book = new SpreadSheet();
            for (String name : List.of("First", "Second")) {
                var sheet = new com.github.miachm.sods.Sheet(name, 4, 1); sheet.getRange(0, 0).setValue("Name");
                for (int i = 1; i <= 3; i++) sheet.getRange(i, 0).setValue(name + i);
                book.appendSheet(sheet);
            }
            book.save(path.toFile()); return new OdsFileParser(path.toString());
        }
        try (Workbook book = format.equals("xls") ? new HSSFWorkbook() : new XSSFWorkbook()) {
            for (String name : List.of("First", "Second")) {
                var sheet = book.createSheet(name); sheet.createRow(0).createCell(0).setCellValue("Name");
                for (int i = 1; i <= 3; i++) sheet.createRow(i).createCell(0).setCellValue(name + i);
            }
            try (var out = Files.newOutputStream(path)) { book.write(out); }
        }
        return format.equals("xls") ? new XLSFileParser(path.toString()) : new XLSXFileParser(path.toString());
    }
    @Test public void accumulatesCountsAcrossAllSelectedSheets() throws Exception {
        var p = parser(); importFile(p, null);
        assertEquals(List.of(List.of("First1"), List.of("First2"), List.of("First3")), rows("first"));
        assertEquals(3, rows("second").size()); assertEquals(6, p.getCurrentRow()); assertEquals(6, p.getTotalRowCount());
    }
    @Test public void preservesUnselectedTablesAndCountsOnlyImportedRows() throws Exception {
        importFile(parser(), null);
        var p = parser(); importFile(p, Set.of("first"));
        assertEquals(3, rows("second").size()); assertEquals(3, p.getCurrentRow()); assertEquals(3, p.getTotalRowCount());
    }
}
