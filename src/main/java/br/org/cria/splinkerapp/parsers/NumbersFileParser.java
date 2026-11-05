package br.org.cria.splinkerapp.parsers;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Locale;

/** Converts Numbers using the macOS application, then uses the standard XLSX importer. */
public class NumbersFileParser extends FileParser {
    public NumbersFileParser() throws Exception { super(); }

    public static String numbersToXLSX(String path) throws Exception {
        if (path == null || !path.toLowerCase(Locale.ROOT).endsWith(".numbers")) {
            throw new IllegalArgumentException("Informe um arquivo .numbers.");
        }
        String finalFile = path.substring(0, path.length() - ".numbers".length()) + ".xlsx";
        String input = path.replace("\\", "\\\\").replace("\"", "\\\"");
        String output = finalFile.replace("\\", "\\\\").replace("\"", "\\\"");
        String script = """
                set inputFilePath to POSIX file "%s"
                set outputFilePath to POSIX file "%s"
                tell application "Numbers"
                    set sourceDocument to open inputFilePath
                    try
                        export sourceDocument to outputFilePath as Microsoft Excel
                    on error errorMessage
                        close sourceDocument saving no
                        error errorMessage
                    end try
                    close sourceDocument saving no
                end tell
                """.formatted(input, output);
        Process process = new ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            int exit = process.waitFor();
            if (exit != 0) throw new IOException("Falha ao converter arquivo Numbers. Codigo de saida: " + exit);
            return finalFile;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally { process.destroy(); }
    }

    public void parseFile(String filePath) throws Exception { parseFile(filePath, null); }

    public void parseFile(String filePath, Set<String> tables) throws Exception {
        var parser = new XLSXFileParser(filePath);
        parser.createTableBasedOnSheet(tables);
        parser.insertDataIntoTable(tables);
        currentRow = parser.getCurrentRow();
        totalRowCount = parser.getTotalRowCount();
    }

    @Override public void insertDataIntoTable(Set<String> tables) {
        throw new UnsupportedOperationException("Converta o arquivo Numbers antes de importar.");
    }
    @Override protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        throw new UnsupportedOperationException("Converta o arquivo Numbers antes de importar.");
    }
    @Override protected String buildCreateTableCommand(Set<String> tables) {
        throw new UnsupportedOperationException("Converta o arquivo Numbers antes de importar.");
    }
}
