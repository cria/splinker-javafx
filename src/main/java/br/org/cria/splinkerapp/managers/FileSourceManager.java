package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.parsers.CsvFileParser;
import br.org.cria.splinkerapp.parsers.DbfFileParser;
import br.org.cria.splinkerapp.parsers.ExcelFileParser;
import br.org.cria.splinkerapp.parsers.FileParser;
import br.org.cria.splinkerapp.parsers.OdsFileParser;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class FileSourceManager {

    public static Service<Void> processData(String fileToProcess) throws Exception
    {
        var filePath = fileToProcess.toLowerCase();
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask()
            {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception
                    {
                        
                        FileParser fileParser = null;
                        DataSourceType type = null;
                        var isExcel = filePath.endsWith(".xlsx") ||filePath.endsWith(".xls");
                        var isCsv = filePath.endsWith(".csv");
                        var isOds = filePath.endsWith(".ods");
                        var isDbf = filePath.endsWith(".dbf");
                        
                        if(isExcel)
                        {
                            fileParser = new ExcelFileParser(filePath);
                            type = DataSourceType.Excel;
                        }
                        if(isCsv)
                        {
                            fileParser = new CsvFileParser(filePath);
                            type = DataSourceType.CSV;
                        }
                        if(isOds)
                        {
                            fileParser = new OdsFileParser(filePath);
                            type = DataSourceType.LibreOfficeCalcODS;
                        }
                        if(isDbf)
                        {
                            fileParser = new DbfFileParser(filePath);
                            type = DataSourceType.dBaseDBF;
                        }
                        fileParser.createTableBasedOnSheet();
                        fileParser.insertDataIntoTable();
                        // var dwcManager = new DarwinCoreArchiveService();
                        // dwcManager.readDataFromSource(new DataSource(type))
                        // .generateTXTFile()
                        // .generateZIPFile()
                        // .transferData();
                        return null;
                    }
                };
            }
        };

    }
    
}
