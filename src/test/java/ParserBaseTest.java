import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import com.github.javafaker.Faker;
import br.org.cria.splinkerapp.utils.StringStandards;
public class ParserBaseTest {
    static boolean isRunningOnGithub = Boolean.valueOf(System.getProperty("IS_GITHUB_FLOW", "false"));
    static Faker faker  = new Faker();
    static String baseConnectionString = "jdbc:sqlite:splinker_%s.db";
    static int rowCount = 300000;
    static String baseDir = System.getProperty("CURRENT_DIR","src/test/java/datasources/");
    protected List<Map<String, String>> getParsedDataFromTable(String tableName, String connString) throws Exception
    {
        var values = new ArrayList<Map<String, String>>();
        var cmd = "SELECT * FROM %s;".formatted(StringStandards.normalizeString(tableName));
        var conn = DriverManager.getConnection(connString);
        var stm = conn.createStatement();
        var result = stm.executeQuery(cmd);
        while (result.next()) 
        {
            var name = result.getString("name");
            var ccNum = result.getString("credit_card");
            var bDay = result.getString("birth_date");
            var row = Map.ofEntries(entry("name", name),entry("credit_card", ccNum), 
                                    entry("birth_date",bDay));
            values.add(row);
        }
        result.close();
        conn.close();
        return values;
    }

    protected static void dropTable(String tableName, String connString) throws Exception
    {
        var cmd = "DROP TABLE IF EXISTS %s;".formatted(StringStandards.normalizeString(tableName));
        var conn = DriverManager.getConnection(connString);
        var stm = conn.createStatement();
        stm.executeUpdate(cmd);
        stm.close();
        conn.close();
    }
}
