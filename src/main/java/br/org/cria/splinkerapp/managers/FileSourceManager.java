package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.parsers.CsvFileParser;
import br.org.cria.splinkerapp.parsers.DbfFileParser;
import br.org.cria.splinkerapp.parsers.ExcelFileParser;
import br.org.cria.splinkerapp.parsers.FileParser;
import br.org.cria.splinkerapp.parsers.NumbersFileParser;
import br.org.cria.splinkerapp.parsers.OdsFileParser;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;

public class FileSourceManager {

    public static Service<Void> processData(DataSet ds) throws Exception
    {
        var filePath = ds.getDataSetFilePath().toLowerCase();
        FileParser fileParser = null;
        var dwcManager = new DarwinCoreArchiveService(ds);
        var isExcel = filePath.endsWith(".xlsx") ||filePath.endsWith(".xls");
        var isCsv = filePath.endsWith(".csv") || filePath.endsWith(".tsv") || filePath.endsWith(".txt");
        var isOds = filePath.endsWith(".ods");
        var isDbf = filePath.endsWith(".dbf");
        var isNumbers = filePath.endsWith(".numbers");
        var unsupportedFileFormat = !(isExcel || isCsv || isOds || isDbf || isCsv || isNumbers);

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
        if(!isNumbers)
        {
            fileParser.createTableBasedOnSheet();
            fileParser.insertDataIntoTable();
        }
        else
        {
            var fp = new NumbersFileParser();
            var finalFile = NumbersFileParser.numbersToXLSX(filePath);
            fp.parseFile(finalFile);
        }
      
        dwcManager.readDataFromSource()
        .generateTXTFile()
        .generateZIPFile();
        return dwcManager.transferData();
    }
    
}
