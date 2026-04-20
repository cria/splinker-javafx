import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService.SpreadsheetFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

public class GoogleDriveFileServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void ExtractsFileIdFromSharedDriveUrl() {
        String url = "https://drive.google.com/file/d/1AbCdEfGhIJkLmNoPq/view?usp=sharing";
        assertEquals("1AbCdEfGhIJkLmNoPq", GoogleDriveFileService.extractFileId(url));
    }

    @Test
    public void ExtractsFileIdFromGoogleSheetsEditUrl() {
        String url = "https://docs.google.com/spreadsheets/d/1AbCdEfGhIJkLmNoPq/edit?usp=sharing";
        assertEquals("1AbCdEfGhIJkLmNoPq", GoogleDriveFileService.extractFileId(url));
    }

    @Test
    public void ExtractsFileIdFromOpenIdUrl() {
        String url = "https://drive.google.com/open?id=1AbCdEfGhIJkLmNoPq";
        assertEquals("1AbCdEfGhIJkLmNoPq", GoogleDriveFileService.extractFileId(url));
    }

    @Test
    public void IdentifiesGoogleSheetsUrl() {
        String url = "https://docs.google.com/spreadsheets/d/1AbCdEfGhIJkLmNoPq/edit?usp=sharing";
        assertEquals(true, GoogleDriveFileService.isGoogleSheetsUrl(url));
    }

    @Test
    public void ValidatesExistingLocalPath() throws Exception {
        File file = tempFolder.newFile("sample-local.xlsx");
        GoogleDriveFileService.validateAccess(file.getAbsolutePath());
    }

    @Test
    public void FailsForMissingLocalPath() throws Exception {
        try {
            GoogleDriveFileService.validateAccess(tempFolder.getRoot().getAbsolutePath() + File.separator + "missing.xlsx");
            fail("Expected validation to fail for missing file");
        } catch (IllegalArgumentException ex) {
            assertEquals("O arquivo informado nao foi encontrado.", ex.getMessage());
        }
    }

    @Test
    public void DetectsXlsFileByMagicBytes() throws Exception {
        File file = tempFolder.newFile("sample.xls");
        try (Workbook workbook = new HSSFWorkbook(); FileOutputStream output = new FileOutputStream(file)) {
            workbook.createSheet("Sheet 1").createRow(0).createCell(0).setCellValue("Name");
            workbook.write(output);
        }

        assertEquals(SpreadsheetFormat.XLS, GoogleDriveFileService.detectSpreadsheetFormat(file.toPath()));
    }

    @Test
    public void DetectsXlsxFileByMagicBytes() throws Exception {
        File file = tempFolder.newFile("sample.xlsx");
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream output = new FileOutputStream(file)) {
            workbook.createSheet("Sheet 1").createRow(0).createCell(0).setCellValue("Name");
            workbook.write(output);
        }

        assertEquals(SpreadsheetFormat.XLSX, GoogleDriveFileService.detectSpreadsheetFormat(file.toPath()));
    }
}
