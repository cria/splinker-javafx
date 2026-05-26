package br.org.cria.splinkerapp.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import br.org.cria.splinkerapp.utils.DatabaseLogUtil;
import io.sentry.Sentry;
import javafx.concurrent.Task;
import br.org.cria.splinkerapp.utils.DbConnectionUtil;

public class DatabaseSetup {
    public static void deleteLocalDatabase() throws IOException {
        var path = Path.of(LocalDbManager.getDbFilePath());
        Files.delete(path);
    }

    public static Task<Void> initDb() {
        return new Task<>() {
            @Override
            protected Void call() {
                return iniciarDB();
            }
        };
    }

    public static Void iniciarDB() {
        try {
            var file = "/scripts/sql/create_tables.sql";
            var inputStream = Task.class.getResourceAsStream(file);
            var builder = new StringBuilder();

            if (inputStream != null) {
                var reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append("%s\n".formatted(line));
                }
            } else {
                System.err.println("SQL File not found!");
                throw new FileNotFoundException(file);
            }

            var content = builder.toString();
            var url = System.getProperty("splinker.connection", LocalDbManager.getLocalDbConnectionString());
            if (url != null && url.startsWith("jdbc:postgresql:")) {
                ApplicationLog.info("[POSTGRES] Inicializando schema via DatabaseSetup. script=%s, sqlLength=%s, url=%s"
                        .formatted(file, content.length(), DatabaseLogUtil.showJdbcUrlWithCredentials(url)));
            }
            try (var conn = DbConnectionUtil.getConnection(url);
                 var statement = conn.createStatement()) {
                statement.executeUpdate(content);
                if (url != null && url.startsWith("jdbc:postgresql:")) {
                    ApplicationLog.info("[POSTGRES] DatabaseSetup executou script de criacao/configuracao com sucesso. url=%s"
                            .formatted(DatabaseLogUtil.showJdbcUrlWithCredentials(url)));
                }
            }

        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }
}
