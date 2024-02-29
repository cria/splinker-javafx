import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import br.org.cria.splinkerapp.parsers.CsvFileParser;
import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CsvFileParserTest extends ParserBaseTest {
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    
    FileWriter writer;
    
    static Map<String, String> fileList = new HashMap<String, String>();
    static Map<String, String> separators = Map.ofEntries(entry("tab", "\t"),
            entry("comma", ","), entry("semicolon", ";"));

    @Test
    public void parseCommaSeparatedCSVTest() throws Exception {
        var connString = baseConnectionString.formatted(tempFolder.getRoot().getAbsolutePath(),"csv");
        System.setProperty("splinker.dbname", connString);
        var fileName = fileList.get("csv");
        var parser = new CsvFileParser(fileName);
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable("splinker", connString);
        var numberOfParsedRows = expected.size();
        assertEquals(rowCount, numberOfParsedRows);
        for (var map : expected) 
        {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);    
            assertNotNull(ccNum);        
            assertNotNull(bDate);      
        }
        
    }

    @Test
    public void parseTabSeparatedTSVTest() throws Exception {
        var connString = baseConnectionString.formatted(tempFolder.getRoot().getAbsolutePath(),"tsv");
        System.setProperty("splinker.dbname", connString);
        var fileName = fileList.get("tsv");
        var parser = new CsvFileParser(fileName);
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable("spLinker", connString);
        var numberOfParsedRows = expected.size();
        assertEquals(rowCount, numberOfParsedRows);
        for (var map : expected) 
        {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);    
            assertNotNull(ccNum);        
            assertNotNull(bDate);     
        }
    }

    @Test
    public void parseSemiColonSeparatedTXTTest() throws Exception {
        var connString = baseConnectionString.formatted(tempFolder.getRoot().getAbsolutePath(),"txt");
        System.setProperty("splinker.dbname", connString);
        var fileName = fileList.get("txt");
        var parser = new CsvFileParser(fileName);
        parser.createTableBasedOnSheet();
        parser.insertDataIntoTable();
        var expected = getParsedDataFromTable("spLinker", connString);
        var numberOfParsedRows = expected.size();
        assertEquals(rowCount, numberOfParsedRows);
        for (var map : expected) 
        {
            var name = map.get("name");
            var ccNum = map.get("credit_card");
            var bDate = map.get("birth_date");
            assertNotNull(name);    
            assertNotNull(ccNum);        
            assertNotNull(bDate);
        }
    }

    @BeforeClass
    public static void setUp() throws Exception
    {

        for(var element: separators.entrySet())
        {
            String extension = "csv";
            switch (element.getKey()) {
                case "tab":
                    extension = "tsv";
                    break;
            case "semicolon":
                    extension = "txt";
                    break;
                default:
                    break;
            }
            var fileName = "%s_separated.%s".formatted(element.getKey(), extension);
            var file = tempFolder.newFile(fileName);
            
            fileList.put(extension, file.getAbsolutePath());
            createCSVFiles(file, element.getValue());
        }
    }

    static void createCSVFiles(File file, String separator) throws Exception 
    {
        String name;
        String ccNum;
        String [] values;
        int i;
        var settings = new CsvWriterSettings();
        var formatter = new CsvFormat();
        formatter.setDelimiter(separator);
        settings.setFormat(formatter);
        
        var csvWriter = new CsvWriter(file, settings);
        var headers = Arrays.asList("Name", "Credit Card", "Birth Date");
        csvWriter.writeHeaders(headers);
        
        for (i = 0; i < rowCount; i++) 
        {
            name = faker.name().fullName();
            ccNum = faker.finance().creditCard();
            var bDay = faker.date().birthday().toString();
            values = new String[] { name, ccNum, bDay };
            csvWriter.writeRow(values);
        }
        
        csvWriter.close();
    }
}
