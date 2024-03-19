package br.org.cria.splinkerapp.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import io.sentry.Sentry;
import javafx.concurrent.Task;

public class DatabaseSetup {
    public static void deleteLocalDatabase() throws IOException 
    {
        var path = Path.of(LocalDbManager.getDbFilePath());
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
                    var inputStream = Task.class.getResourceAsStream(file);
                    var builder = new StringBuilder();

                    if (inputStream != null) 
                    {
                        var reader = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        System.out.println("lendo arquivo SQL");
                        while ((line = reader.readLine()) != null) 
                        {
                            builder.append("%s\n".formatted(line));
                        }
                    } 
                    else 
                    {
                        System.err.println("SQL File not found!");
                        throw new FileNotFoundException(file);
                    }
            
                    var content = builder.toString();
                    var url = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());
                    System.out.println("criando BD SQLite");
                    var conn = DriverManager.getConnection(url);
                    var statement = conn.createStatement();
                    var result = statement.executeUpdate(content);
                    System.out.println(result);
                    statement.close();
                    conn.close();
                    System.out.println("BD SQLite criado");

                } 
                catch (Exception e) 
                {
                    Sentry.captureException(e);
                    ApplicationLog.error(e.getLocalizedMessage());
                    e.printStackTrace();
                    LockFileManager.deleteLockfile();
                    throw new RuntimeException(e);
                    //System.exit(1);
                }
                return null;
            }
        };
    }
}
