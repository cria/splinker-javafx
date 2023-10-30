package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.parsers.CsvFileParser;
import br.org.cria.splinkerapp.parsers.DbfFileParser;
import br.org.cria.splinkerapp.parsers.ExcelFileParser;
import br.org.cria.splinkerapp.parsers.FileParser;
import br.org.cria.splinkerapp.parsers.OdsFileParser;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;

public class FileSourceManager {

    public static Service<Void> processData(String fileToProcess) throws Exception
    {
        var filePath = fileToProcess.toLowerCase();
        FileParser fileParser = null;
        var dwcManager = new DarwinCoreArchiveService();
        var isExcel = filePath.endsWith(".xlsx") ||filePath.endsWith(".xls");
        var isCsv = filePath.endsWith(".csv");
        var isOds = filePath.endsWith(".ods");
        var isDbf = filePath.endsWith(".dbf");
        var unsupportedFileFormat = !(isExcel || isCsv || isOds || isDbf);

        if(unsupportedFileFormat)
        {
            throw new Exception("Formato de arquivo não suportado");
        }
        if(isExcel)
        {
            fileParser = new ExcelFileParser(filePath);
        }
        if(isCsv)
        {
            fileParser = new CsvFileParser(filePath);
        }
        if(isOds)
        {
            fileParser = new OdsFileParser(filePath);
        }
        if(isDbf)
        {
            fileParser = new DbfFileParser(filePath);
        }

        fileParser.createTableBasedOnSheet();
        fileParser.insertDataIntoTable();
        var ds = DataSourceRepository.getDataSource();
        dwcManager.readDataFromSource(ds)
        .generateTXTFile()
        .generateZIPFile();
        return dwcManager.transferData();
    }
    
}
