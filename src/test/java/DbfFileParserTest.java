import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.javafaker.Faker;
import com.linuxense.javadbf.DBFDataType;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFWriter;
import br.org.cria.splinkerapp.parsers.DbfFileParser;
import br.org.cria.splinkerapp.utils.StringStandards;

public class DbfFileParserTest extends ParserBaseTest {
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    final static String fileName = "test_dbf_data.dbf";
    static Faker faker = new Faker();
    static File dbf;

    @Test
    public void ParseDataFromDbfFileTest() throws Exception
    {
        var connString = baseConnectionString.formatted(tempFolder.getRoot().getAbsolutePath(),"dbf");
        System.setProperty("splinker.dbname", connString);
        var tableName = "test_dbf_data";
        var parser = new DbfFileParser(dbf.getAbsolutePath());
        
        parser.createTableBasedOnSheet(null);
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(tableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("Name");
            var ccNum = map.get("CreditCard");
            var bDate = map.get("BirthDate");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }
    
    @BeforeClass
    public static void setUp() throws Exception
    {
        dbf = tempFolder.newFile(fileName);
        createDbfFile(dbf);
    }

    static void createDbfFile(File file) throws Exception
    {
        var dbfWriter = new DBFWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
        var nameField = new DBFField("Name", DBFDataType.CHARACTER);
        var ccNumField = new DBFField("CreditCard", DBFDataType.CHARACTER);
        var bDayField = new DBFField("BirthDate", DBFDataType.DATE);
        var fields = new DBFField[]{nameField, ccNumField, bDayField}; 
        
        dbfWriter.setFields(fields);

        for (int i = 0; i < rowCount; i++) 
        {
            var name = faker.name().fullName();
            var ccNum = faker.finance().creditCard();   
            var bDay = faker.date().birthday();
            Object[] rowData = {name, ccNum, bDay};
            dbfWriter.addRecord(rowData);
        }

        dbfWriter.close();
    }
    @Override
    protected List<Map<String, String>> getParsedDataFromTable(String tableName, String connString) throws Exception
    {
        var values = new ArrayList<Map<String, String>>();
        var cmd = "SELECT * FROM %s;".formatted(StringStandards.normalizeString(tableName));
        var conn = DriverManager.getConnection(connString);
        var stm = conn.createStatement();
        var result = stm.executeQuery(cmd);
        while (result.next()) 
        {
            var name = result.getString("Name");
            var ccNum = result.getString("CreditCard");
            var bDay = result.getString("BirthDate");
            var row = Map.ofEntries(entry("Name", name),entry("CreditCard", ccNum), 
                                    entry("BirthDate",bDay));
            values.add(row);
        }
        result.close();
        conn.close();
        return values;
    }

}
