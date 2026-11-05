package br.org.cria.splinkerapp.parsers;

import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NumbersFileParserTest extends ParserBaseTest {
    @Test public void importsConvertedWorkbookThroughXlsxParser() throws Exception {
        var path = SpreadsheetFixtures.create(temp.getRoot().toPath().resolve("converted.xlsx"), true, 2);
        var p = new NumbersFileParser(); p.parseFile(path.toString());
        assertEquals(2, rows("records").size()); assertEquals(2, p.getCurrentRow()); assertEquals(2, p.getTotalRowCount());
    }
    @Test public void passesSelectionToConvertedWorkbook() throws Exception {
        try (var xlsx = mockConstruction(XLSXFileParser.class)) {
            new NumbersFileParser().parseFile("converted.xlsx", Set.of("records"));
            var p = xlsx.constructed().getFirst(); verify(p).createTableBasedOnSheet(Set.of("records")); verify(p).insertDataIntoTable(Set.of("records"));
        }
    }
    @Test public void convertsUsingSimulatedMacOsProcess() throws Exception {
        var process = mock(Process.class); when(process.waitFor()).thenReturn(0);
        try (var builders = mockConstruction(ProcessBuilder.class, (b, context) -> {
            when(b.redirectErrorStream(true)).thenReturn(b); when(b.redirectOutput(ProcessBuilder.Redirect.DISCARD)).thenReturn(b); when(b.start()).thenReturn(process);
            assertEquals("osascript", context.arguments().getFirst() instanceof String[] cmd ? cmd[0] : "unexpected constructor");
        })) {
            assertEquals("/tmp/My \"File\".xlsx", NumbersFileParser.numbersToXLSX("/tmp/My \"File\".NUMBERS"));
            verify(process).destroy();
        }
    }
    @Test public void reportsConversionFailureAndAlwaysDestroysProcess() throws Exception {
        var process = mock(Process.class); when(process.waitFor()).thenReturn(1);
        try (var builders = mockConstruction(ProcessBuilder.class, (b, context) -> {
            when(b.redirectErrorStream(true)).thenReturn(b); when(b.redirectOutput(ProcessBuilder.Redirect.DISCARD)).thenReturn(b); when(b.start()).thenReturn(process);
        })) {
            assertThrows(IOException.class, () -> NumbersFileParser.numbersToXLSX("source.numbers")); verify(process).destroy();
        }
        assertThrows(IllegalArgumentException.class, () -> NumbersFileParser.numbersToXLSX("source.csv"));
        assertThrows(IllegalArgumentException.class, () -> NumbersFileParser.numbersToXLSX(null));
    }
    @Test public void preservesInterruption() throws Exception {
        var process = mock(Process.class); when(process.waitFor()).thenThrow(new InterruptedException("cancelled"));
        try (var builders = mockConstruction(ProcessBuilder.class, (b, context) -> {
            when(b.redirectErrorStream(true)).thenReturn(b); when(b.redirectOutput(ProcessBuilder.Redirect.DISCARD)).thenReturn(b); when(b.start()).thenReturn(process);
        })) {
            assertThrows(InterruptedException.class, () -> NumbersFileParser.numbersToXLSX("source.numbers"));
            assertTrue(Thread.currentThread().isInterrupted()); verify(process).destroy();
        } finally { Thread.interrupted(); }
    }
    @Test public void requiresConversionBeforeDirectImport() throws Exception {
        var p = new NumbersFileParser();
        assertThrows(UnsupportedOperationException.class, () -> p.insertDataIntoTable(null));
        assertThrows(UnsupportedOperationException.class, () -> p.buildCreateTableCommand(null));
        assertThrows(UnsupportedOperationException.class, () -> p.getRowAsStringList(null, 0));
    }
}
