import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.util.HashSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.javafaker.Faker;
import br.org.cria.splinkerapp.parsers.XLSFileParser;

public class XLSFileParserTest extends ParserBaseTest {
    
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    final static String oldFormatFilePath = "OldFormat.xls";
    final static String sheetBaseName = "Test Sheet %s";
    static Faker faker = new Faker();
    static File xls;
    static int numberOfXLSrows = rowCount/5; //limite de arquivos XLS = 65535 linhas

    @Test
    public void ParseDataFromXLSFileTest() throws Exception
    {
            
            var connString = baseConnectionString.formatted(tempFolder.getRoot().getAbsolutePath(),"xls");
            System.setProperty("splinker.dbname", connString);
            var tableName = "test_sheet_xls";
            var path = xls.getAbsolutePath();
            var parser = new XLSFileParser(path);
            parser.createTableBasedOnSheet(null);
            parser.insertDataIntoTable(new HashSet<>());
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

    @BeforeClass
    public static void setUp() throws Exception
    {
        xls = tempFolder.newFile(oldFormatFilePath);
        createExcelFile(xls, numberOfXLSrows+1);
    }

    static void createExcelFile(File file, int numberOfContentRows) throws Exception
    {
            var  sheetName = sheetBaseName.formatted("xls");
            var workbook = new HSSFWorkbook();
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
            // Write the workbook to a file
            workbook.write(file);
            workbook.close();
    }
}
