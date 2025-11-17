package br.org.cria.splinkerapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ApplicationLog {

    private static final long MAX_SIZE = 1024 * 1024; // 1 MB
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Arquivo no diretório onde o app executa
    private static final Path LOG_FILE;

    static {
        String workDir = System.getProperty("user.dir");
        LOG_FILE = Paths.get(workDir, "splinker.log");
    }

    private static synchronized void write(String level, String message, Throwable t) {
        try {
            rotateIfNeeded(); // <-- verifica tamanho e reseta se necessário

            String timestamp = LocalDateTime.now().format(FORMATTER);
            String line = String.format("[%s] [%s] %s%n", timestamp, level, message);

            Files.write(LOG_FILE,
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                String stack = sw.toString();

                Files.write(LOG_FILE,
                        stack.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Apaga o conteúdo ao chegar em 1MB
    private static void rotateIfNeeded() throws IOException {
        if (Files.exists(LOG_FILE)) {
            long size = Files.size(LOG_FILE);

            if (size >= MAX_SIZE) {
                // Trunca o arquivo → limpa conteúdo mas mantém o mesmo arquivo
                Files.newOutputStream(LOG_FILE, StandardOpenOption.TRUNCATE_EXISTING).close();
            }
        }
    }

    public static void info(String message) {
        System.out.println(message);
        write("INFO", message, null);
    }

    public static void warn(String message) {
        System.out.println(message);
        write("WARN", message, null);
    }

    public static void error(String message) {
        System.err.println(message);
        write("ERROR", message, null);
    }

    public static void error(String message, Throwable t) {
        System.err.println(message);
        write("ERROR", message, t);
    }

    public static void debug(String message) {
        System.out.println(message);
        write("DEBUG", message, null);
    }
}
