package br.org.cria.splinkerapp.utils;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.managers.LocalDbManager;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SQLiteSource {
    private SQLiteSource() {
    }

    public static Path resolvePath(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException(I18n.text("file.sqlite.required"));
        }

        Path selectedPath;
        try {
            selectedPath = Path.of(filePath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(I18n.text("file.not.found"), e);
        }
        if (!Files.isRegularFile(selectedPath)) {
            throw new IllegalArgumentException(I18n.text("file.not.found"));
        }
        if (!Files.isReadable(selectedPath)) {
            throw new IllegalArgumentException(I18n.text("file.not.readable"));
        }

        Path path = selectedPath.toRealPath();
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path internalDatabase = Path.of(LocalDbManager.getDbFilePath());
        // Transfers clear the numeric dataset directories and temporary tables in the internal database.
        if (isExportPath(selectedPath, workingDirectory) || isExportPath(path, workingDirectory.toRealPath())
                || (Files.isRegularFile(internalDatabase) && Files.isSameFile(path, internalDatabase))) {
            throw new IllegalArgumentException(I18n.text("file.sqlite.reserved"));
        }
        return path;
    }

    private static boolean isExportPath(Path path, Path workingDirectory) {
        return path.startsWith(workingDirectory) && path.getNameCount() > workingDirectory.getNameCount() + 1
                && path.getName(workingDirectory.getNameCount()).toString().matches("[0-9]+");
    }

    public static String jdbcUrl(Path path) {
        var uri = path.toAbsolutePath().toUri();
        // SQLite expects UNC server names to appear in the path, with an empty URI authority.
        return "jdbc:sqlite:" + (uri.getRawAuthority() == null ? uri.toASCIIString()
                : "file:////" + uri.getRawAuthority() + uri.getRawPath());
    }

    public static Connection open(Path path) throws SQLException {
        var config = new SQLiteConfig();
        config.setReadOnly(true);
        return DriverManager.getConnection(jdbcUrl(path), config.toProperties());
    }
}
