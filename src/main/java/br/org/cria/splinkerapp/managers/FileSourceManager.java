package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.parsers.AccessFileParser;
import br.org.cria.splinkerapp.parsers.CsvFileParser;
import br.org.cria.splinkerapp.parsers.DbfFileParser;
import br.org.cria.splinkerapp.parsers.FileParser;
import br.org.cria.splinkerapp.parsers.NumbersFileParser;
import br.org.cria.splinkerapp.parsers.OdsFileParser;
import br.org.cria.splinkerapp.parsers.XLSFileParser;
import br.org.cria.splinkerapp.parsers.XLSXFileParser;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService.SpreadsheetFormat;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService.SpreadsheetRemoteFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class FileSourceManager {
    DataSet ds;
    FileParser fileParser;
    String filePath;
    Path temporaryResolvedFile;

    public FileSourceManager(DataSet ds) throws Exception {
        this.ds = ds;
        if (ds.getDataSetFilePath() != null) {
            this.filePath = ds.getDataSetFilePath().trim();
        }

        try {
            buildFileParser();
        } catch (Exception ex) {
            cleanupTemporaryResolvedFile();
            throw ex;
        }
    }

    public FileParser getParser() {
        return this.fileParser;
    }

    void buildFileParser() throws Exception {
        var remoteSpreadsheet = resolveGoogleDriveSpreadsheetIfNeeded();
        if (remoteSpreadsheet != null) {
            configureGoogleDriveSpreadsheetParser(remoteSpreadsheet);
            return;
        }

        var lowerCasePath = filePath.toLowerCase(Locale.ROOT);
        var isAccessDb = lowerCasePath.endsWith(".mdb") || lowerCasePath.endsWith(".accdb");
        var isXLS = lowerCasePath.endsWith(".xls");
        var isXLSX = lowerCasePath.endsWith(".xlsx");
        var isCsv = lowerCasePath.endsWith(".csv") || lowerCasePath.endsWith(".tsv") || lowerCasePath.endsWith(".txt");
        var isOds = lowerCasePath.endsWith(".ods");
        var isDbf = lowerCasePath.endsWith(".dbf");
        var isNumbers = lowerCasePath.endsWith(".numbers");
        var unsupportedFileFormat = !(isAccessDb || isXLS || isXLSX || isOds || isDbf || isCsv || isNumbers);

        if (unsupportedFileFormat) {
            throw new Exception("Formato de arquivo nao suportado");
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

    public void importData(Set<String> tabelas) throws Exception {
        try {
            var isNotMacOsNumbersFile = ds.getType() != DataSourceType.Numbers;
            if (isNotMacOsNumbersFile) {
                fileParser.createTableBasedOnSheet(tabelas);
                fileParser.insertDataIntoTable(tabelas);

                var token = ds.getToken();
                var totalRowCount = fileParser.getTotalRowCount();
                DataSetService.updateRowcount(token, totalRowCount);

            } else {
                var fp = new NumbersFileParser();
                var finalFile = NumbersFileParser.numbersToXLSX(filePath);
                fp.parseFile(finalFile);
            }
        } finally {
            cleanupTemporaryResolvedFile();
        }
    }

    private SpreadsheetRemoteFile resolveGoogleDriveSpreadsheetIfNeeded() throws Exception {
        if (!GoogleDriveFileService.isRemotePath(filePath)) {
            return null;
        }

        if (!GoogleDriveFileService.isGoogleDriveUrl(filePath)) {
            throw new IllegalArgumentException("Links remotos para planilhas devem ser informados via Google Drive.");
        }

        if (ds.getType() != DataSourceType.Excel) {
            throw new IllegalArgumentException("Links do Google Drive sao suportados apenas para arquivos Excel (.xls e .xlsx).");
        }

        return GoogleDriveFileService.downloadSpreadsheet(filePath);
    }

    private void configureGoogleDriveSpreadsheetParser(SpreadsheetRemoteFile remoteFile) throws Exception {
        temporaryResolvedFile = remoteFile.getLocalFile();
        String localPath = remoteFile.getLocalFile().toAbsolutePath().toString();

        if (remoteFile.getFormat() == SpreadsheetFormat.XLS) {
            fileParser = new XLSFileParser(localPath);
            return;
        }

        if (remoteFile.getFormat() == SpreadsheetFormat.XLSX) {
            fileParser = new XLSXFileParser(localPath);
            return;
        }

        throw new IllegalArgumentException("O arquivo remoto do Google Drive deve estar em formato .xls ou .xlsx.");
    }

    private void cleanupTemporaryResolvedFile() throws Exception {
        if (temporaryResolvedFile != null) {
            Files.deleteIfExists(temporaryResolvedFile);
            temporaryResolvedFile = null;
        }
    }
}
