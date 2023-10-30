package br.org.cria.splinkerapp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class DatabaseSetup {
    
    public static void deleteLocalDatabase() throws IOException
    {
        var file = "%s/spLinker.db".formatted(System.getProperty("user.dir"));
        var path = Path.of(file);
        Files.delete(path);
    }

    public static Service initDb() {

        return new Service<Void>() {
            @Override
            protected Task<Void> createTask() 
            {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception 
                    {
                        try {
                                var file = "%s/scripts/sql/create_tables.sql"
                                            .formatted(System.getProperty("user.dir"));
                                var fullPath = Paths.get(file);
                                var content = Files.readString(fullPath);
                                var url = "jdbc:sqlite:splinker.db";
                                var conn = DriverManager.getConnection(url);
                                var statement = conn.createStatement();
                                var result = statement.executeUpdate(content);
                                System.out.println(result);     
                                
                            } 
                            catch (Exception e) 
                            {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            return null;
                    }
                };
            }
        };
    }
}
