package br.org.cria.splinkerapp.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DatabaseSetup {
    private static ExecutorService executor 
      = Executors.newSingleThreadExecutor();
    public static void initDb() {
        
        executor.submit(() -> {
            try {
                    var folder = System.getProperty("user.dir");
                    var path = Paths.get(folder +"/SpLinkerApp/scripts/sql/create_tables.sql");
                    var lines = Files.readAllLines(path);
                    var url = "jdbc:sqlite:splinker.db";
                    var conn = DriverManager.getConnection(url);
                    var commands = String.join("", lines);
                    var statement = conn.createStatement();
                    var result = statement.executeUpdate(commands);
                    System.out.println(result);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
