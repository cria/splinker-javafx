package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import org.apache.commons.lang3.SystemUtils;

public class SpLinkerUpdateService {
    static String command = "Powershell.exe";
    static String scriptPath = "/scripts/update/softwareUpdate.";
    static String operatingSystem = SystemUtils.OS_NAME.toLowerCase();
    static String fileExtension = "ps1";
    static String params = "-Command";
    static String githubURL = "https://api.github.com/repos/cria/splinker-javafx/releases";
    static String latestDownloadUrl = "";

    public static boolean hasNewVersion() throws Exception {
        try {
            URL url = new URL(githubURL);
            HttpURLConnection connection;

            var isBehindProxy = ProxyConfigRepository.isBehindProxyServer();
            if (isBehindProxy) {
                var proxyConfig = ProxyConfigRepository.getConfiguration();
                var proxyHost = proxyConfig.getAddress();
                var proxyPort = Integer.valueOf(proxyConfig.getPort());
                var proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "SpLinker-App");

            StringBuilder responseStr = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseStr.append(line);
                }
            }

            com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
            gsonBuilder.setLenient();
            com.google.gson.Gson gson = gsonBuilder.create();

            com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(
                    new java.io.StringReader(responseStr.toString()));
            jsonReader.setLenient(true);

            com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                    new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
            List<Map<String, Object>> releases = gson.fromJson(jsonReader, typeToken.getType());

            if (releases == null || releases.isEmpty()) {
                return false;
            }

            Map<String, Object> latestRelease = releases.get(0);
            String releaseName = (String) latestRelease.get("name");

            if (latestRelease.containsKey("assets") && latestRelease.get("assets") instanceof List) {
                List<?> assets = (List<?>) latestRelease.get("assets");
                for (Object assetObj : assets) {
                    if (assetObj instanceof Map) {
                        Map<?, ?> asset = (Map<?, ?>) assetObj;
                        if (asset.containsKey("browser_download_url")) {
                            String downloadUrl = asset.get("browser_download_url").toString();
                            if (downloadUrl.endsWith(".msi") || downloadUrl.endsWith(".exe")) {
                                latestDownloadUrl = downloadUrl;
                                break;
                            }
                        }
                    }
                }
            }

            String latestVersion = releaseName.replaceAll("[^0-9.]", "");

            return compareVersions(latestVersion, VersionService.getVersion()) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }


    private static int compareVersions(String version1, String version2) {
        version1 = version1.replaceAll("[^0-9.]", "");
        version2 = version2.replaceAll("[^0-9.]", "");

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int part1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int part2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;

            if (part1 < part2) {
                return -1;
            }
            if (part1 > part2) {
                return 1;
            }
        }
        return 0;
    }


    public static void verifyOSVersion() {

        if (operatingSystem.contains("windows")) {
            var versionNumber = operatingSystem.replaceAll("[^0-9]", "");
            var hasVersion = versionNumber != "";
            var isOldWindowsVersion = hasVersion && Integer.parseInt(versionNumber) < 11;

            if (isOldWindowsVersion) {
                fileExtension = "bat";
                command = "cmd"; //? 
                params = "";
            }
        } else {
            command = "bash";
            fileExtension = "sh";
        }


    }

    /**
     * Executa o script de atualização do software utilizando arquivos temporários para maior confiabilidade
     */
    public static void runSoftwareUpdate() {
        try {
            java.io.File homeDir = new java.io.File(System.getProperty("user.home"));
            java.io.File downloadDir = new java.io.File(homeDir, "Downloads");
            java.io.File outputFile = new java.io.File(downloadDir, "splinker_new_version.msi");

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            String batContent =
                    "@echo off\n" +
                            "echo Iniciando download da atualizacao do spLinker...\n" +
                            "echo URL: " + latestDownloadUrl + "\n" +
                            "echo Destino: " + outputFile.getAbsolutePath() + "\n\n" +

                            "REM Usar bitsadmin para download (ferramenta nativa do Windows)\n" +
                            "bitsadmin /transfer \"spLinkerUpdate\" \"" + latestDownloadUrl + "\" \"" + outputFile.getAbsolutePath() + "\"\n\n" +

                            "if exist \"" + outputFile.getAbsolutePath() + "\" (\n" +
                            "    echo Download concluido com sucesso!\n" +
                            "    echo Iniciando instalacao...\n" +
                            "    start \"\" \"" + outputFile.getAbsolutePath() + "\"\n" +
                            "    echo Instalador iniciado.\n" +
                            ") else (\n" +
                            "    echo Falha no download. Usando metodo alternativo...\n" +
                            "    powershell -Command \"& {try { Invoke-WebRequest -Uri '" + latestDownloadUrl + "' -OutFile '" + outputFile.getAbsolutePath() + "' } catch { Write-Host $_.Exception.Message }}\"\n" +
                            "    \n" +
                            "    if exist \"" + outputFile.getAbsolutePath() + "\" (\n" +
                            "        echo Download concluido com sucesso!\n" +
                            "        echo Iniciando instalacao...\n" +
                            "        start \"\" \"" + outputFile.getAbsolutePath() + "\"\n" +
                            "        echo Instalador iniciado.\n" +
                            "    ) else (\n" +
                            "        echo Falha em todos os metodos de download.\n" +
                            "        echo Por favor, baixe manualmente em: " + latestDownloadUrl + "\n" +
                            "        exit 1\n" +
                            "    )\n" +
                            ")\n" +
                            "echo Pressione qualquer tecla para sair...\n" +
                            "pause > nul\n";

            java.nio.file.Path batPath = java.nio.file.Files.createTempFile("splinker_update_", ".bat");
            java.nio.file.Files.writeString(batPath, batContent);

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", batPath.toAbsolutePath().toString());
            processBuilder.inheritIO();

            Process process = processBuilder.start();

            Thread.sleep(2000);

            if (process.isAlive()) {
                System.exit(0);
            } else {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new Exception("O script de atualização falhou com código: " + exitCode);
                }
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao iniciar atualização: " + e.getMessage(), e);
        }
    }
}
