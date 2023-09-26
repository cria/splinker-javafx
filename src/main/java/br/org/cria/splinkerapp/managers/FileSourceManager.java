package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.services.implementations.CsvFileParser;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DbfFileParser;
import br.org.cria.splinkerapp.services.implementations.ExcelFileParser;
import br.org.cria.splinkerapp.services.implementations.FileParser;
import br.org.cria.splinkerapp.services.implementations.OdsFileParser;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class FileSourceManager {

    public static Service<Void> processData(String filePath) throws Exception
    {
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
                        
                        if(isExcel ||isCsv)
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
                        var dwcManager = new DarwinCoreArchiveService();
                        dwcManager.readDataFromSource(new DataSource(type))
                        .generateTXTFile()
                        .generateZIPFile()
                        .transferData();
                        return null;
                    }
                };
            }
        };

    }
    
}
