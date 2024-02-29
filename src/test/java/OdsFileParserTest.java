import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.github.javafaker.Faker;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import br.org.cria.splinkerapp.parsers.OdsFileParser;

public class OdsFileParserTest  extends ParserBaseTest {
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    final static String fileName = "test_ods_file.ods";
    final static String tableName = "Sheet1";
    static Faker faker = new Faker();
    static File ods;
    static int odsRowCount = rowCount/30;
    @Test
    public void ParseDataFromOdsFileTest() throws Exception
    {
        var connString = baseConnectionString.formatted("ods");
        System.setProperty("splinker.dbname", connString);
        var parser = new OdsFileParser(ods.getAbsolutePath());
        
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(tableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(odsRowCount, numberOfInsertedRows);
    }

    @BeforeClass
    public static void setUp() throws Exception
    {
        createOdsFile();
    }

    static void createOdsFile() throws Exception
    {
        ods = tempFolder.newFile(fileName);
        SpreadSheet spreadSheet = new SpreadSheet();
        Sheet sheet = new Sheet(tableName, rowCount, 3);
        
        // Add column names
        sheet.getDataRange().getCell(0, 0).setValue("name");
        sheet.getDataRange().getCell(0, 1).setValue("credit_card");
        sheet.getDataRange().getCell(0, 2).setValue("birth_date");
        
        
        for (int i = 1; i <= odsRowCount; i++) 
        {
            var name = faker.name().fullName();
            var ccNum = faker.finance().creditCard();   
            var bDay = faker.date().birthday();
            sheet.getDataRange().getCell(i, 0).setValue(name);
            sheet.getDataRange().getCell(i, 1).setValue(ccNum);
            sheet.getDataRange().getCell(i, 2).setValue(bDay);
        }

        spreadSheet.appendSheet(sheet);
        spreadSheet.save(ods);
    }
}
