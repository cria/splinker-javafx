package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.parsers.AccessFileParser;
import br.org.cria.splinkerapp.parsers.CsvFileParser;
import br.org.cria.splinkerapp.parsers.DbfFileParser;
import br.org.cria.splinkerapp.parsers.XLSFileParser;
import br.org.cria.splinkerapp.parsers.XLSXFileParser;
import br.org.cria.splinkerapp.parsers.FileParser;
import br.org.cria.splinkerapp.parsers.NumbersFileParser;
import br.org.cria.splinkerapp.parsers.OdsFileParser;
import br.org.cria.splinkerapp.services.implementations.DataSetService;

public class FileSourceManager {
    DataSet ds;
    FileParser fileParser;
    String filePath;

    public FileSourceManager(DataSet ds) throws Exception {
        this.ds = ds;
        this.filePath = ds.getDataSetFilePath().toLowerCase();
        buildFileParser();
    }

    public FileParser getParser() {
        return this.fileParser;
    }

    void buildFileParser() throws Exception {
        var isAccessDb = filePath.endsWith("mdb");
        var isXLS = filePath.endsWith(".xls");
        var isXLSX = filePath.endsWith(".xlsx");
        var isCsv = filePath.endsWith(".csv") || filePath.endsWith(".tsv") || filePath.endsWith(".txt");
        var isOds = filePath.endsWith(".ods");
        var isDbf = filePath.endsWith(".dbf");
        var isNumbers = filePath.endsWith(".numbers");
        var unsupportedFileFormat = !(isAccessDb || isXLS || isXLSX || isOds || isDbf || isCsv || isNumbers);

        if (unsupportedFileFormat) {
            throw new Exception("Formato de arquivo não suportado");
        }
        if (isAccessDb) {
            fileParser = new AccessFileParser(filePath, ds.getDbPassword());
        }
        if (isXLS) {
            fileParser = new XLSFileParser(filePath);
        }
        if (isXLSX) {
            fileParser = new XLSXFileParser(filePath);
        }
        if (isCsv) {
            fileParser = new CsvFileParser(filePath);
        }
        if (isOds) {
            fileParser = new OdsFileParser(filePath);
        }
        if (isDbf) {
            fileParser = new DbfFileParser(filePath);
        }
    }

    public void importData() throws Exception {
        var isNotMacOsNumbersFile = ds.getType() != DataSourceType.Numbers;
        if (isNotMacOsNumbersFile) {
            fileParser.createTableBasedOnSheet();
            fileParser.insertDataIntoTable();

            var token = ds.getToken();
            var totalRowCount = fileParser.getTotalRowCount();
            DataSetService.updateRowcount(token, totalRowCount);

        } else {
            var fp = new NumbersFileParser();
            var finalFile = NumbersFileParser.numbersToXLSX(filePath);
            fp.parseFile(finalFile);
        }
    }

}
