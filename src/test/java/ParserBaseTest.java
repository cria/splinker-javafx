import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;    
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.github.javafaker.Faker;

import br.org.cria.splinkerapp.utils.StringStandards;

public class ParserBaseTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    Faker faker  = new Faker();
    final String connString = "jdbc:sqlite:splinker.db";
    int rowCount = 1000;
    
    protected List<Map<String, String>> getParsedDataFromTable(String tableName, String connString) throws Exception
    {
        var values = new ArrayList<Map<String, String>>();
        var cmd = "SELECT * FROM %s;".formatted(StringStandards.normalizeString(tableName));
        var conn = DriverManager.getConnection(connString);
        var stm = conn.createStatement();
        var result = stm.executeQuery(cmd);
        int count = 0;
        while (result.next()) 
        {
            count++;
            var name = result.getString("name");
            var ccNum = result.getString("credit_card");
            var row = Map.ofEntries(entry("name", name),entry("credit_card", ccNum));
            values.add(row);
        }
        result.close();
        conn.close();
        return values;
    }
}
