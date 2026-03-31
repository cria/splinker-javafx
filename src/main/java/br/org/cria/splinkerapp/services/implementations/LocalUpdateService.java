package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public class LocalUpdateService {

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;

    public static void executeUpdateFromGithubRelease() throws Exception {
        String downloadUrl = VersionService.getLatestReleaseDownloadUrlForCurrentOS();

        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalStateException("Não foi possível localizar um arquivo de download compatível com o sistema operacional atual.");
        }

        Path currentExecutable = resolveCurrentExecutable();
        validateCurrentExecutable(currentExecutable);

        Path appDir = currentExecutable.getParent();
        validateWritableDirectory(appDir);

        String fileName = extractFileName(downloadUrl);
        String currentName = currentExecutable.getFileName().toString();
        String downloadedSuffix = getFileExtension(fileName);

        Path downloadedFile = appDir.resolve("splinker" + downloadedSuffix);

        ApplicationLog.info("Baixando nova versão a partir do GitHub...");
        downloadFile(downloadUrl, downloadedFile);

        if (!Files.exists(downloadedFile) || Files.size(downloadedFile) == 0) {
            throw new IllegalStateException("O download da atualização falhou ou gerou um arquivo vazio.");
        }

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (downloadedFile.toString().endsWith(".jar") && currentName.endsWith(".jar")) {
            if (osName.contains("win")) {
                Path script = createWindowsJarUpdateScript(appDir, currentExecutable, downloadedFile);
                ApplicationLog.info("Atualização preparada com sucesso. A aplicação será encerrada para concluir a troca do arquivo.");
                launchWindowsScript(script);
            } else if (osName.contains("linux") || osName.contains("nux") || osName.contains("nix")) {
                Path script = createLinuxJarUpdateScript(appDir, currentExecutable, downloadedFile);
                ApplicationLog.info("Atualização preparada com sucesso. A aplicação será encerrada para concluir a troca do arquivo.");
                launchLinuxScript(script);
            } else {
                throw new IllegalStateException("Sistema operacional não suportado para atualização automática por substituição de JAR.");
            }

            System.exit(0);
            return;
        }

        throw new IllegalStateException(
                "A release encontrada não é um JAR compatível com substituição automática. " +
                        "Arquivo encontrado: " + downloadedFile.getFileName()
        );
    }

    private static Path resolveCurrentExecutable() throws Exception {
        Path path = Paths.get(
                LocalUpdateService.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        );

        if (!Files.exists(path)) {
            throw new IllegalStateException("Não foi possível localizar o executável atual da aplicação.");
        }

        return path;
    }

    private static void validateCurrentExecutable(Path currentExecutable) {
        if (!currentExecutable.getFileName().toString().endsWith(".jar")) {
            throw new IllegalStateException("A atualização automática só é suportada quando a aplicação é executada via arquivo JAR.");
        }
    }

    private static void validateWritableDirectory(Path appDir) throws Exception {
        if (appDir == null || !Files.exists(appDir)) {
            throw new IllegalStateException("Diretório da aplicação não encontrado.");
        }

        if (!Files.isDirectory(appDir)) {
            throw new IllegalStateException("O caminho da aplicação não aponta para um diretório válido.");
        }

        if (!Files.isWritable(appDir)) {
            throw new IllegalStateException("Sem permissão de escrita na pasta da aplicação: " + appDir);
        }
    }

    private static void downloadFile(String downloadUrl, Path destination) throws Exception {
        HttpURLConnection connection;

        boolean isBehindProxy = ProxyConfigRepository.isBehindProxyServer();
        if (isBehindProxy) {
            var proxyConfig = ProxyConfigRepository.getConfiguration();
            Proxy proxy = new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(proxyConfig.getAddress(), Integer.parseInt(proxyConfig.getPort()))
            );
            connection = (HttpURLConnection) new URL(downloadUrl).openConnection(proxy);
        } else {
            connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        }

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "SpLinker-App");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("Falha no download da atualização. HTTP " + statusCode);
        }

        try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(destination.toFile())) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    private static Path createWindowsJarUpdateScript(Path appDir, Path currentJar, Path newJar) throws Exception {
        Path script = appDir.resolve("splinker_update.bat");

        String currentJarPath = quoteWindows(currentJar.toAbsolutePath().toString());
        String newJarPath = quoteWindows(newJar.toAbsolutePath().toString());
        String backupJarPath = quoteWindows(currentJar.toAbsolutePath() + ".bak");
        String scriptPath = quoteWindows(script.toAbsolutePath().toString());

        String content =
                "@echo off\r\n" +
                        "setlocal enableextensions enabledelayedexpansion\r\n" +
                        "timeout /t 8 /nobreak > nul\r\n" +
                        "\r\n" +
                        "if not exist " + newJarPath + " goto :cleanup\r\n" +
                        "\r\n" +
                        "if exist " + backupJarPath + " del /f /q " + backupJarPath + " > nul 2>&1\r\n" +
                        "\r\n" +
                        "set /a tentativas=0\r\n" +
                        ":try_backup\r\n" +
                        "set /a tentativas+=1\r\n" +
                        "\r\n" +
                        "if exist " + currentJarPath + " (\r\n" +
                        "  move /y " + currentJarPath + " " + backupJarPath + " > nul 2>&1\r\n" +
                        "  if not errorlevel 1 goto :backup_ok\r\n" +
                        ")\r\n" +
                        "\r\n" +
                        "if !tentativas! GEQ 15 goto :cleanup\r\n" +
                        "timeout /t 2 /nobreak > nul\r\n" +
                        "goto :try_backup\r\n" +
                        "\r\n" +
                        ":backup_ok\r\n" +
                        "set /a tentativas_move=0\r\n" +
                        ":try_replace\r\n" +
                        "set /a tentativas_move+=1\r\n" +
                        "\r\n" +
                        "move /y " + newJarPath + " " + currentJarPath + " > nul 2>&1\r\n" +
                        "if not errorlevel 1 goto :replace_ok\r\n" +
                        "\r\n" +
                        "if !tentativas_move! GEQ 10 (\r\n" +
                        "  if exist " + backupJarPath + " move /y " + backupJarPath + " " + currentJarPath + " > nul 2>&1\r\n" +
                        "  goto :cleanup\r\n" +
                        ")\r\n" +
                        "\r\n" +
                        "timeout /t 2 /nobreak > nul\r\n" +
                        "goto :try_replace\r\n" +
                        "\r\n" +
                        ":replace_ok\r\n" +
                        "if exist " + backupJarPath + " del /f /q " + backupJarPath + " > nul 2>&1\r\n" +
                        "start \"\" java -jar " + currentJarPath + "\r\n" +
                        "\r\n" +
                        ":cleanup\r\n" +
                        "del /f /q " + scriptPath + " > nul 2>&1\r\n" +
                        "endlocal\r\n";

        writeScript(script, content);
        return script;
    }

    private static Path createLinuxJarUpdateScript(Path appDir, Path currentJar, Path newJar) throws Exception {
        Path script = appDir.resolve("splinker_update.sh");

        String currentJarPath = quoteUnix(currentJar.toAbsolutePath().toString());
        String newJarPath = quoteUnix(newJar.toAbsolutePath().toString());
        String backupJarPath = quoteUnix(currentJar.toAbsolutePath() + ".bak");

        String content =
                "#!/bin/sh\n" +
                        "echo \"Aguardando a aplicação encerrar...\"\n" +
                        "sleep 3\n" +
                        "\n" +
                        "if [ ! -f " + newJarPath + " ]; then\n" +
                        "  echo \"Falha: arquivo baixado não encontrado.\"\n" +
                        "  rm -- \"$0\"\n" +
                        "  exit 1\n" +
                        "fi\n" +
                        "\n" +
                        "rm -f " + backupJarPath + "\n" +
                        "if [ -f " + currentJarPath + " ]; then\n" +
                        "  mv " + currentJarPath + " " + backupJarPath + " || {\n" +
                        "    echo \"Falha ao mover a versão atual para backup.\"\n" +
                        "    rm -- \"$0\"\n" +
                        "    exit 1\n" +
                        "  }\n" +
                        "fi\n" +
                        "\n" +
                        "mv " + newJarPath + " " + currentJarPath + " || {\n" +
                        "  echo \"Falha ao substituir a versão atual.\"\n" +
                        "  if [ -f " + backupJarPath + " ]; then mv " + backupJarPath + " " + currentJarPath + "; fi\n" +
                        "  rm -- \"$0\"\n" +
                        "  exit 1\n" +
                        "}\n" +
                        "\n" +
                        "rm -f " + backupJarPath + "\n" +
                        "chmod +x " + currentJarPath + " 2>/dev/null\n" +
                        "\n" +
                        "echo \"Atualização concluída com sucesso. Iniciando nova versão...\"\n" +
                        "nohup java -jar " + currentJarPath + " >/dev/null 2>&1 &\n" +
                        "rm -- \"$0\"\n";

        writeScript(script, content);
        script.toFile().setExecutable(true);
        return script;
    }

    private static void launchWindowsScript(Path script) throws Exception {
        new ProcessBuilder("cmd.exe", "/c", script.toAbsolutePath().toString())
                .directory(script.getParent().toFile())
                .start();
    }

    private static void launchLinuxScript(Path script) throws Exception {
        new ProcessBuilder("/bin/sh", script.toAbsolutePath().toString())
                .directory(script.getParent().toFile())
                .start();
    }

    private static void writeScript(Path script, String content) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(
                        script,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                ), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }

    private static String extractFileName(String url) {
        int idx = url.lastIndexOf('/');
        if (idx >= 0 && idx < url.length() - 1) {
            return url.substring(idx + 1);
        }
        return "release-download.jar";
    }

    private static String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx >= 0) {
            return fileName.substring(idx);
        }
        return ".jar";
    }

    private static String quoteWindows(String value) {
        return "\"" + value + "\"";
    }

    private static String quoteUnix(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}