import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.github.javafaker.Faker;
import br.org.cria.splinkerapp.parsers.XLSXFileParser;

public class XLSXFileParserTest extends ParserBaseTest {
    
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    
    final static String newFormatFilePath = "NewFormat.xlsx";
    final static String sheetBaseName = "Test Sheet %s";
    static Faker faker = new Faker();
    static File xlsx;
    
    @Test
    public void ParseDataFromXLSXFileTest() throws Exception
    {
        var connString = baseConnectionString.formatted(tempFolder.getRoot().getAbsolutePath(),"xlsx");
        System.setProperty("splinker.dbname", connString);
        var tableName = "test_sheet_xlsx";
        var path = xlsx.getAbsolutePath();
        var parser = new XLSXFileParser(path);
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(tableName, connString);
        var numberOfInsertedRows = expected.size();
        for (var map : expected) 
        {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);    
            assertNotNull(ccNum);        
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }

    @BeforeClass
    public static void setUp() throws Exception
    {
        xlsx = tempFolder.newFile(newFormatFilePath);
        createExcelFile(xlsx, rowCount+1);
    }
    
    static void createExcelFile(File file, int numberOfContentRows) throws Exception
    {
            var sheetName = sheetBaseName.formatted("xlsx");
            // Create a new XSSFWorkBook (for XLSX format)
            Workbook workbook = new XSSFWorkbook();
            // Create a new sheet
            Sheet sheet = workbook.createSheet(sheetName);
            // Create a row in the sheet
            Row row = sheet.createRow(0);
            // Create cells in the row and set their values
            Cell cell1 = row.createCell(0);
            Cell cell2 = row.createCell(1);
            Cell cell3 = row.createCell(2);
            cell1.setCellValue("Name");
            cell2.setCellValue("Credit Card");
            cell3.setCellValue("Birth Date");
            for (int i = 1; i < numberOfContentRows; i++) 
            {
                row = sheet.createRow(i);
                row.createCell(0).setCellValue(faker.name().fullName());
                row.createCell(1).setCellValue(faker.finance().creditCard());   
                row.createCell(2).setCellValue(faker.date().birthday().toString());
            }
            var fullPath = file.getAbsolutePath();
            var outputStream = new FileOutputStream(fullPath);
            // Write the workbook to a file
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();        
    }
}
