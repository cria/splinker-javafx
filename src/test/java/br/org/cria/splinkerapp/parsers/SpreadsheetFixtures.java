package br.org.cria.splinkerapp.parsers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.file.*;

final class SpreadsheetFixtures {
    static Path create(Path path, boolean xlsx, int count) throws Exception {
        try (Workbook w = xlsx ? new XSSFWorkbook() : new HSSFWorkbook()) {
            var sheet = w.createSheet("Records");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Name");
            header.createCell(1).setCellValue("Count");
            header.createCell(2).setCellValue("Active");
            for (int i = 1; i <= count; i++) {
                var row = sheet.createRow(i);
                row.createCell(0).setCellValue("Ana");
                row.createCell(1).setCellValue(42);
                row.createCell(2).setCellValue(true);
            }
            try (var out = Files.newOutputStream(path)) { w.write(out); }
        }
        return path;
    }
}
