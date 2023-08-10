package br.org.cria.splinkerapp.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import br.org.cria.splinkerapp.models.DataSourceType;


public class DatabaseSetup {
    private static final ExecutorService executor
      = Executors.newSingleThreadExecutor();
    public static void initDb() {
        
        executor.submit(() -> {
            try {
                    var file = "%s/scripts/sql/create_tables.sql".formatted(System.getProperty("user.dir"));
                    var fullPath = Paths.get(file);
                    var lines = Files.readAllLines(fullPath);
                    var url = "jdbc:sqlite:splinker.db";
                    var conn = DriverManager.getConnection(url);
                    var commands = String.join("", lines);
                    var statement = conn.createStatement();
                    var result = statement.executeUpdate(commands);
                    System.out.println(result);

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
  
    private static String getDbConnectioString(DataSourceType datasource){

        return "";
    }
}
