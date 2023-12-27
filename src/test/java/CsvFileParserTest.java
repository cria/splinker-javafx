
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hsqldb.util.CSVWriter;
import org.junit.Before;
import org.junit.Test;
import br.org.cria.splinkerapp.parsers.CsvFileParser;
import static java.util.Map.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CsvFileParserTest extends ParserBaseTest {
    FileWriter writer;
    List<String[]> content = new ArrayList<String[]>();

    Map<String, String> separators = Map.ofEntries(entry("tab", "\t"),
            entry("comma", ","), entry("semicolon", ";"));

    @Test
    public void parseCommaSeparatedCSVTest() throws Exception {
        var fileName = "comma_separated.csv";
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
            assertNotNull(name);    
            assertNotNull(ccNum);        
        }
        
    }

    @Test
    public void parseTabSeparatedCSVTest() throws Exception {
        var fileName = "tab_separated.tsv";
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
            assertNotNull(name);    
            assertNotNull(ccNum);        
        }
    }

    @Test
    public void parseSemiColonSeparatedCSVTest() throws Exception {
        var fileName = "semicolon_separated.txt";
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
            assertNotNull(name);    
            assertNotNull(ccNum);        
        }
    }

    @Before
    public void setUp() throws Exception
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
            var filename = "%s_separated.%s".formatted(element.getKey(), extension);
            createCSVFiles(filename, element.getValue());
        }
    }

    void createCSVFiles(String fileName, String separator) throws Exception 
    {
        //var content = new ArrayList<String[]>();
        String name;
        String ccNum;
        String [] values;
        int i;
        var csvWriter = new CSVWriter(new File(fileName),"UTF-8");
        //var csvWriter = new CSVWriter(fileWriter ,separator.charAt(0),'"','\\',"\n");
        var headers = new String[] { "Name", "Credit Card"};
        csvWriter.writeHeader(headers);
        
        for (i = 0; i < rowCount; i++) 
        {
            name = faker.name().fullName();
            ccNum = faker.finance().creditCard();
            values = new String[] { name, ccNum };
            content.add(values);
            csvWriter.writeData(values);
        }
        
        csvWriter.close();
    }
}
