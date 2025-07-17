package br.org.cria.splinkerapp.parsers;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class NumbersFileParser extends FileParser {

    public NumbersFileParser() throws Exception {
        super();
    }

    public static String numbersToXLSX(String path) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        var finalFile = path.replace(".numbers", ".xlsx");
        // var manager = new ScriptEngineManager();
        // var engine = manager.getEngineByName("AppleScript");
        // if(engine == null)
        // {
        //     engine = manager.getEngineByName("AppleScriptEngine");
        // }
        String cmd = """
                'set inputFilePath to "%s"
                set outputFilePath to "%s"
                tell application "Numbers"
                    open document inputFilePath
                end tell
                tell application "Numbers"
                    export document active document as type "Microsoft Excel" to outputFilePath
                end tell
                tell application "Numbers"
                    close document active document
                end tell'
                """.formatted(path, finalFile);
        processBuilder.command("osascript", "-e", cmd);
        Process process = processBuilder.start();
        var result = process.waitFor();
        var exit = process.exitValue();
        var z = new byte[]{};
        var bla = process.getOutputStream();
        bla.write(z);
        var asd = bla.toString();
        process.destroy();
        // engine.eval(cmd);
        return finalFile;
    }

    public void parseFile(String filePath) throws Exception {
        var file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(fis);
        Sheet sheet = workbook.getSheet("");// workbook.getSheetAt(0);
        for (Row row : sheet) {
            for (Cell cell : row) {
                // Get the cell value
                String cellValue = cell.getStringCellValue();
                // Do something with the cell value
                System.out.println("Cell Value: " + cellValue);
            }
        }
        // Close the workbook and input stream
        workbook.close();
        fis.close();
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'insertDataIntoTable'");
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        throw new UnsupportedOperationException("Unimplemented method 'getRowAsStringList'");
    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'buildCreateTableCommand'");
    }

}
