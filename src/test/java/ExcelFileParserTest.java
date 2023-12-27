import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import com.github.javafaker.Faker;
import br.org.cria.splinkerapp.parsers.ExcelFileParser;

public class ExcelFileParserTest extends ParserBaseTest {
    final String oldFormatFilePath = "OldFormat2.xls";
    final String newFormatFilePath = "NewFormat2.xlsx";
    final String sheetName = "Test Sheet";
    Faker faker = new Faker();
    File xls;
    File xlsx;

    @Test
    public void ParseDataFromXLSFileTest() throws Exception
    {
            var path = xls.getAbsolutePath();
            var parser = new ExcelFileParser(path);
            parser.createTableBasedOnSheet();
            parser.insertDataIntoTable();
            var expected = getParsedDataFromTable("test_sheet", connString);
            var numberOfInsertedRows = expected.size();
            for (var map : expected) 
            {
                var name = map.get("name");
                var ccNum = map.get("credit_card");
                assertNotNull(name);    
                assertNotNull(ccNum);        
            }
            assertEquals(rowCount, numberOfInsertedRows);
    }
    
    @Test
    public void ParseDataFromXLSXFileTest() throws Exception
    {
            var path = xlsx.getAbsolutePath();
            var parser = new ExcelFileParser(path);
            parser.createTableBasedOnSheet();
            parser.insertDataIntoTable();
            var expected = getParsedDataFromTable("test_sheet", connString);
            var numberOfInsertedRows = expected.size();
            for (var map : expected) 
            {
                var name = map.get("name");
                var ccNum = map.get("credit_card");
                assertNotNull(name);    
                assertNotNull(ccNum);        
            }
            assertEquals(rowCount, numberOfInsertedRows);
    }

    @Before
    public void setUp() throws Exception
    {
        xls = folder.newFile(oldFormatFilePath);
        xlsx = folder.newFile(newFormatFilePath);
        createExcelFile(xls);
        createExcelFile(xlsx);
    }

    void createExcelFile(File file) throws Exception
    {
            
            // Create a new HSSFWorkbook (for XLS format) or XSSFWorkBook (for XLSX format)
            Workbook workbook =  file.getAbsolutePath().endsWith("xls") ?
                            new HSSFWorkbook(): new XSSFWorkbook();
            // Create a new sheet
            Sheet sheet = workbook.createSheet(sheetName);
            // Create a row in the sheet
            Row row = sheet.createRow(0);
            // Create cells in the row and set their values
            Cell cell1 = row.createCell(0);
            Cell cell2 = row.createCell(1);
            cell1.setCellValue("Name");
            cell2.setCellValue("Credit Card");
            var numberOfContentRows = rowCount + 1;
            for (int i = 1; i < numberOfContentRows; i++) 
            {
                row = sheet.createRow(i);
                row.createCell(0).setCellValue(faker.name().fullName());
                row.createCell(1).setCellValue(faker.finance().creditCard());   
            }
            var fullPath = file.getAbsolutePath();
            var outputStream = new FileOutputStream(fullPath);
            // Write the workbook to a file
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();        
    }
}
