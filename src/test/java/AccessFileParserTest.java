import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.TableBuilder;
import br.org.cria.splinkerapp.parsers.AccessFileParser;
import com.healthmarketscience.jackcess.Database.FileFormat;

public class AccessFileParserTest extends ParserBaseTest {
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();
    
    static String filePath = "testAccess_%s.mdb";
    static String tableName = "TestData%s";
    static List<FileFormat> unsupportedFormats = Arrays.asList(FileFormat.GENERIC_JET4, FileFormat.V1997, FileFormat.MSISAM);
    static List<FileFormat> fileformats = Arrays.asList(FileFormat.values()).stream()
            .filter(e -> !unsupportedFormats.contains(e)).toList();

    @Test
    public void parseV2000AccessFileTest() throws Exception {

        var format = FileFormat.V2000.name();
        var connString = baseConnectionString.formatted(format);
        System.setProperty("splinker.dbname", connString);
        var fileName = "%s/%s".formatted(folder.getRoot().getAbsolutePath(), filePath.formatted(format));
        var formattedTableName = tableName.formatted(format);
        var parser = new AccessFileParser(fileName, "");
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(formattedTableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }

    @Test
    public void parseV2003AccessFileTest() throws Exception
    {
        var format = FileFormat.V2003.name();
        var connString = baseConnectionString.formatted(format);
        System.setProperty("splinker.dbname", connString);
        var fileName = "%s/%s".formatted(folder.getRoot().getAbsolutePath(), filePath.formatted(format));
        var formattedTableName = tableName.formatted(format);
        var parser = new AccessFileParser(fileName, "");
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(formattedTableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }

    @Test
    public void parseV2007AccessFileTest() throws Exception
    {
        var format = FileFormat.V2007.name();
        var connString = baseConnectionString.formatted(format);
        System.setProperty("splinker.dbname", connString);
        var fileName = "%s/%s".formatted(folder.getRoot().getAbsolutePath(), filePath.formatted(format));
        var formattedTableName = tableName.formatted(format);
        var parser = new AccessFileParser(fileName, "");
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(formattedTableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }

    @Test
    public void parseV2010AccessFileTest() throws Exception
    {
        var format = FileFormat.V2010.name();
        var connString = baseConnectionString.formatted(format);
        System.setProperty("splinker.dbname", connString);
        var fileName = "%s/%s".formatted(folder.getRoot().getAbsolutePath(), filePath.formatted(format));
        var formattedTableName = tableName.formatted(format);
        var parser = new AccessFileParser(fileName, "");
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(formattedTableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }

    @Test
    public void parseV2016AccessFileTest() throws Exception
    {
        var format = FileFormat.V2016.name();
        var connString = baseConnectionString.formatted(format);
        System.setProperty("splinker.dbname", connString);
        var fileName = "%s/%s".formatted(folder.getRoot().getAbsolutePath(), filePath.formatted(format));
        var formattedTableName = tableName.formatted(format);
        var parser = new AccessFileParser(fileName, "");
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(formattedTableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);
            assertNotNull(ccNum);
            assertNotNull(bDate);
        }
        assertEquals(rowCount, numberOfInsertedRows);
    }

    @Test
    public void parseV2019AccessFileTest() throws Exception
    {
        var format = FileFormat.V2019.name();
        var connString = baseConnectionString.formatted(format);
        System.setProperty("splinker.dbname", connString);
        var fileName = "%s/%s".formatted(folder.getRoot().getAbsolutePath(), filePath.formatted(format));
        var formattedTableName = tableName.formatted(format);
        var parser = new AccessFileParser(fileName, "");
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable(formattedTableName, connString);
        var numberOfInsertedRows = expected.size();

        for (var map : expected) {
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
    public static void setUp() throws Exception {
        for (var format : fileformats) 
        {
            var fileName = filePath.formatted(format.name());
            createAccessFiles(format, fileName);
        }
    }

    @AfterClass
    public static void tearDown()
    {
        try 
        {
            for (var format : fileformats) 
            {
                var formatName = format.name();
                Files.delete(Path.of("splinker_%s.db".formatted(formatName)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createAccessFiles(FileFormat format, String fileName) throws Exception {
        int i;
        File dbFile = folder.newFile(fileName);
        Database db = DatabaseBuilder.create(format, dbFile);
        var formattedTableName = tableName.formatted(format.name());
        var table = new TableBuilder(formattedTableName)
                .addColumn(new ColumnBuilder("Name", DataType.TEXT))
                .addColumn(new ColumnBuilder("Credit Card", DataType.TEXT))
                .addColumn(new ColumnBuilder("Birth Date", DataType.TEXT))
                .toTable(db);

        for (i = 0; i < rowCount; i++) {
            var name = faker.name().fullName();
            var ccNum = faker.finance().creditCard();
            var date = faker.date().birthday().toString();
            var values = new String[] { name, ccNum, date };
            table.addRow(values);
        }
        db.close();
    }
}
