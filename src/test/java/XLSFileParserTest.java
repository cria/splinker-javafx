import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.Test;
import com.github.javafaker.Faker;
import br.org.cria.splinkerapp.parsers.XLSFileParser;

public class XLSFileParserTest extends ParserBaseTest {
    
    final static String oldFormatFilePath = "%sOldFormat2.xls".formatted(baseDir);
    final static String newFormatFilePath = "%sNewFormat2.xlsx".formatted(baseDir);
    final static String sheetBaseName = "Test Sheet %s";
    static Faker faker = new Faker();
    static File xls;
    static File xlsx;
    static int numberOfXLSrows = rowCount/5; //limite de arquivos XLS = 65535 linhas

    @Test
    public void ParseDataFromXLSFileTest() throws Exception
    {
            xls = new File(oldFormatFilePath);
            var connString = baseConnectionString.formatted("xls");
            System.setProperty("splinker.dbname", connString);
            var tableName = "test_sheet_xls";
            var path = xls.getAbsolutePath();
            var parser = new XLSFileParser(path);
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
            assertEquals(numberOfXLSrows, numberOfInsertedRows);
    }
    
    @Test
    public void ParseDataFromXLSXFileTest() throws Exception
    {
        xlsx = new File(newFormatFilePath);
        var connString = baseConnectionString.formatted("xlsx");
        System.setProperty("splinker.dbname", connString);
        var tableName = "test_sheet_xlsx";
        var path = xlsx.getAbsolutePath();
        var parser = new XLSFileParser(path);
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
    // @BeforeClass
    // public static void setUp() throws Exception
    // {
    //     xls = new File(oldFormatFilePath);
    //     xlsx = new File(newFormatFilePath);
    //     createExcelFile(xls, numberOfXLSrows+1);
    //     createExcelFile(xlsx, rowCount+1);
    // }

    @AfterClass
    public static void tearDown()
    {
        if(!isRunningOnGithub)
        {
            try 
            {
                Files.delete(Path.of("splinker_xls.db"));
                Files.delete(Path.of("splinker_xlsx.db"));   
            } catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }
    
    static void createExcelFile(File file, int numberOfContentRows) throws Exception
    {
            System.out.println("Creating file %s...\n".formatted(file.getAbsolutePath()));
            var isOldFormat = file.getAbsolutePath().endsWith("xls");
            var sheetName = sheetBaseName.formatted("xlsx");
            // Create a new HSSFWorkbook (for XLS format) or XSSFWorkBook (for XLSX format)
            Workbook workbook = new XSSFWorkbook();
            if(isOldFormat)
            {
                sheetName = sheetBaseName.formatted("xls");
                workbook = new HSSFWorkbook();
            }
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