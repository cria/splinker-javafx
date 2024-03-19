package br.org.cria.splinkerapp.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import br.org.cria.splinkerapp.ApplicationLog;
import io.sentry.Sentry;
import javafx.concurrent.Task;

public class DatabaseSetup {
    private static String userHome = System.getProperty("user.home");
    private static String dbFilePath = "%s/splinker.db".formatted(userHome);
    private static String connString =  "jdbc:sqlite:%s".formatted(dbFilePath);

    public static void deleteLocalDatabase() throws IOException {
        var path = Path.of(dbFilePath);
        Files.delete(path);
    }

    public static Task<Void> initDb() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception 
            {
                try 
                {
                    var file = "/scripts/sql/create_tables.sql";
                    var inputStream = getClass().getResourceAsStream(file);
                    var builder = new StringBuilder();

                    if (inputStream != null) {
                        // Read the content from the input stream
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                            String line;
                            while ((line = reader.readLine()) != null) 
                            {
                                builder.append("%s\n".formatted(line));
                            }
                        } catch (IOException e) {
                            e.printStackTrace(); // Handle the IOException appropriately
                        }
                    } else {
                        System.err.println("File not found!");
                    }
            
                    var content = builder.toString();
                    var url = System.getProperty("splinker.connection", connString);
                    var conn = DriverManager.getConnection(url);
                    var statement = conn.createStatement();
                    var result = statement.executeUpdate(content);
                    System.out.println(result);
                    statement.close();
                    conn.close();

                } 
                catch (Exception e) 
                {
                    Sentry.captureException(e);
                    ApplicationLog.error(e.getLocalizedMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                return null;
            }
        };
    }
}
