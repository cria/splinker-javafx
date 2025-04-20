package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import org.apache.commons.lang3.SystemUtils;

public class SpLinkerUpdateService {
    static String command;
    static String scriptPath = "/scripts/update/softwareUpdate.";
    static String operatingSystem = SystemUtils.OS_NAME.toLowerCase();
    static String fileExtension;
    static String params;
    static String githubURL = "https://api.github.com/repos/cria/splinker-javafx/releases";
    static String latestDownloadUrl = "";
    static String installerExtension = "msi";

    static {
        verifyOSVersion();
    }

    public static boolean hasNewVersion() throws Exception {
        try {
            verifyOSVersion();

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

            List<String> availableLinuxUrls = new ArrayList<>();
            String preferredUrl = null;

            if (latestRelease.containsKey("assets") && latestRelease.get("assets") instanceof List) {
                List<?> assets = (List<?>) latestRelease.get("assets");

                if (operatingSystem.contains("linux")) {
                    for (Object assetObj : assets) {
                        if (assetObj instanceof Map) {
                            Map<?, ?> asset = (Map<?, ?>) assetObj;
                            if (asset.containsKey("browser_download_url")) {
                                String downloadUrl = asset.get("browser_download_url").toString();

                                if (downloadUrl.endsWith(".deb") || downloadUrl.endsWith(".rpm") ||
                                        downloadUrl.endsWith(".AppImage")) {
                                    availableLinuxUrls.add(downloadUrl);
                                }

                                if (downloadUrl.endsWith("." + installerExtension)) {
                                    preferredUrl = downloadUrl;
                                    break;
                                }
                            }
                        }
                    }

                    if (preferredUrl != null) {
                        latestDownloadUrl = preferredUrl;
                    }
                    else if (!availableLinuxUrls.isEmpty()) {
                        latestDownloadUrl = availableLinuxUrls.get(0);
                        String ext = latestDownloadUrl.substring(latestDownloadUrl.lastIndexOf(".") + 1);
                        installerExtension = ext;
                    }
                } else if (operatingSystem.contains("windows")) {
                    for (Object assetObj : assets) {
                        if (assetObj instanceof Map) {
                            Map<?, ?> asset = (Map<?, ?>) assetObj;
                            if (asset.containsKey("browser_download_url")) {
                                String downloadUrl = asset.get("browser_download_url").toString();
                                if (downloadUrl.endsWith(".msi") || downloadUrl.endsWith(".exe")) {
                                    latestDownloadUrl = downloadUrl;
                                    installerExtension = downloadUrl.substring(downloadUrl.lastIndexOf(".") + 1);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    for (Object assetObj : assets) {
                        if (assetObj instanceof Map) {
                            Map<?, ?> asset = (Map<?, ?>) assetObj;
                            if (asset.containsKey("browser_download_url")) {
                                String downloadUrl = asset.get("browser_download_url").toString();
                                if (downloadUrl.endsWith(".dmg") || downloadUrl.endsWith(".pkg")) {
                                    latestDownloadUrl = downloadUrl;
                                    installerExtension = downloadUrl.substring(downloadUrl.lastIndexOf(".") + 1);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            String latestVersion = releaseName.replaceAll("[^0-9.]", "");
            return compareVersions(latestVersion, VersionService.getVersion()) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getInstallerExtension() {
        return installerExtension;
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

    public static String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }

    public static void verifyOSVersion() {
        if (operatingSystem.contains("windows")) {
            var versionNumber = operatingSystem.replaceAll("[^0-9]", "");
            var hasVersion = !versionNumber.isEmpty();
            var isOldWindowsVersion = hasVersion && Integer.parseInt(versionNumber) < 11;

            command = isOldWindowsVersion ? "cmd" : "Powershell.exe";
            fileExtension = isOldWindowsVersion ? "bat" : "ps1";
            params = isOldWindowsVersion ? "" : "-Command";
            installerExtension = "msi";
        } else if (operatingSystem.contains("linux")) {
            command = "bash";
            fileExtension = "sh";
            params = "";
            installerExtension = detectLinuxDistribution();
        } else {
            command = "bash";
            fileExtension = "sh";
            params = "";
            installerExtension = "dmg";
        }
    }

    public static void runSoftwareUpdate() {
        try {
            java.io.File homeDir = new java.io.File(System.getProperty("user.home"));
            java.io.File downloadDir = new java.io.File(homeDir, "Downloads");
            java.io.File outputFile = new java.io.File(downloadDir, "splinker_new_version." + installerExtension);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            if (operatingSystem.contains("windows")) {
                runWindowsUpdate(outputFile);
            } else if (operatingSystem.contains("linux")) {
                runLinuxUpdate(outputFile);
            } else {
                runMacOSUpdate(outputFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao iniciar atualização: " + e.getMessage(), e);
        }
    }

    private static void runWindowsUpdate(java.io.File outputFile) throws Exception {
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

        Path batPath = Files.createTempFile("splinker_update_", ".bat");
        Files.writeString(batPath, batContent);

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
    }

    private static void runLinuxUpdate(java.io.File outputFile) throws Exception {
        String shContent =
                "#!/bin/bash\n" +
                        "echo \"Iniciando download da atualização do spLinker...\"\n" +
                        "echo \"URL: " + latestDownloadUrl + "\"\n" +
                        "echo \"Destino: " + outputFile.getAbsolutePath() + "\"\n\n" +

                        "if command -v wget > /dev/null; then\n" +
                        "    echo \"Usando wget para download...\"\n" +
                        "    wget -O \"" + outputFile.getAbsolutePath() + "\" \"" + latestDownloadUrl + "\"\n" +
                        "    DOWNLOAD_STATUS=$?\n" +
                        "elif command -v curl > /dev/null; then\n" +
                        "    echo \"Usando curl para download...\"\n" +
                        "    curl -L \"" + latestDownloadUrl + "\" -o \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "    DOWNLOAD_STATUS=$?\n" +
                        "else\n" +
                        "    echo \"Nem wget nem curl estão disponíveis. Por favor, instale um deles e tente novamente.\"\n" +
                        "    exit 1\n" +
                        "fi\n\n" +

                        "if [ $DOWNLOAD_STATUS -eq 0 ] && [ -f \"" + outputFile.getAbsolutePath() + "\" ]; then\n" +
                        "    echo \"Download concluído com sucesso!\"\n" +
                        "    echo \"Iniciando instalação...\"\n" +
                        "    \n" +
                        "    if [[ \"" + outputFile.getAbsolutePath() + "\" == *.deb ]]; then\n" +
                        "        if command -v gdebi > /dev/null; then\n" +
                        "            echo \"Instalando com gdebi...\"\n" +
                        "            pkexec gdebi --non-interactive \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=$?\n" +
                        "        elif command -v apt > /dev/null; then\n" +
                        "            echo \"Instalando com apt...\"\n" +
                        "            pkexec apt install -y \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=$?\n" +
                        "        else\n" +
                        "            echo \"Não foi possível encontrar um instalador adequado para pacotes DEB.\"\n" +
                        "            echo \"Por favor, instale manualmente usando: sudo dpkg -i " + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=1\n" +
                        "        fi\n" +
                        "    elif [[ \"" + outputFile.getAbsolutePath() + "\" == *.rpm ]]; then\n" +
                        "        echo \"Detectado arquivo RPM. Verificando instaladores disponíveis...\"\n" +
                        "        INSTALL_STATUS=1\n" +
                        "        \n" +
                        "        if command -v dnf > /dev/null; then\n" +
                        "            echo \"Instalando com dnf...\"\n" +
                        "            pkexec dnf install -y \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=$?\n" +
                        "        elif command -v zypper > /dev/null; then\n" +
                        "            echo \"Instalando com zypper...\"\n" +
                        "            pkexec zypper --non-interactive install \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=$?\n" +
                        "        elif command -v yum > /dev/null; then\n" +
                        "            echo \"Instalando com yum...\"\n" +
                        "            pkexec yum install -y \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=$?\n" +
                        "        elif command -v rpm > /dev/null; then\n" +
                        "            echo \"Instalando com rpm...\"\n" +
                        "            pkexec rpm -i \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "            INSTALL_STATUS=$?\n" +
                        "        fi\n" +
                        "        \n" +
                        "        if [ $INSTALL_STATUS -ne 0 ]; then\n" +
                        "            echo \"Não foi possível instalar automaticamente o pacote RPM.\"\n" +
                        "            echo \"O arquivo foi baixado com sucesso em: " + outputFile.getAbsolutePath() + "\"\n" +
                        "            echo \"Por favor, instale manualmente usando o método apropriado para sua distribuição.\"\n" +
                        "            echo \"Por exemplo: sudo rpm -i " + outputFile.getAbsolutePath() + "\"\n" +
                        "            echo \"ou: sudo dnf install " + outputFile.getAbsolutePath() + "\"\n" +
                        "        fi\n" +
                        "    elif [[ \"" + outputFile.getAbsolutePath() + "\" == *.AppImage ]]; then\n" +
                        "        echo \"Preparando AppImage...\"\n" +
                        "        chmod +x \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "        echo \"Executando AppImage...\"\n" +
                        "        \"" + outputFile.getAbsolutePath() + "\" &\n" +
                        "        INSTALL_STATUS=$?\n" +
                        "        \n" +
                        "        if [ $INSTALL_STATUS -ne 0 ]; then\n" +
                        "            echo \"Não foi possível executar o AppImage automaticamente.\"\n" +
                        "            echo \"O arquivo foi baixado com sucesso em: " + outputFile.getAbsolutePath() + "\"\n" +
                        "            echo \"Por favor, execute manualmente com: chmod +x " + outputFile.getAbsolutePath() + " && " + outputFile.getAbsolutePath() + "\"\n" +
                        "        fi\n" +
                        "    else\n" +
                        "        echo \"Tipo de arquivo não reconhecido: " + outputFile.getAbsolutePath() + "\"\n" +
                        "        echo \"O arquivo foi baixado com sucesso, mas você precisa instalá-lo manualmente.\"\n" +
                        "        INSTALL_STATUS=1\n" +
                        "    fi\n" +
                        "    \n" +
                        "    if [ ${INSTALL_STATUS:-0} -eq 0 ]; then\n" +
                        "        echo \"Instalação concluída com sucesso!\"\n" +
                        "    else\n" +
                        "        echo \"A instalação automática falhou, mas o arquivo foi baixado com sucesso.\"\n" +
                        "        echo \"Você pode encontrar o instalador em: " + outputFile.getAbsolutePath() + "\"\n" +
                        "    fi\n" +
                        "    \n" +
                        "    echo \"O aplicativo será fechado em alguns segundos.\"\n" +
                        "    sleep 5\n" +
                        "    exit 0\n" +
                        "else\n" +
                        "    echo \"Falha no download. Código de erro: $DOWNLOAD_STATUS\"\n" +
                        "    echo \"Por favor, baixe manualmente em: " + latestDownloadUrl + "\"\n" +
                        "    echo \"Pressione Enter para sair...\"\n" +
                        "    read\n" +
                        "    exit 1\n" +
                        "fi\n";

        Path shPath = Files.createTempFile("splinker_update_", ".sh");
        Files.writeString(shPath, shContent);

        File shFile = shPath.toFile();
        shFile.setExecutable(true);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/bash", shPath.toAbsolutePath().toString());
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
    }

    private static String detectLinuxDistribution() {
        try {
            File osRelease = new File("/etc/os-release");
            if (osRelease.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileReader reader = new java.io.FileReader(osRelease)) {
                    props.load(reader);
                }

                String id = props.getProperty("ID", "").toLowerCase();

                if (id.contains("debian") || id.contains("ubuntu") || id.contains("mint") ||
                        id.contains("pop") || id.contains("elementary")) {
                    return "deb";
                }
                else if (id.contains("fedora") || id.contains("rhel") || id.contains("centos") ||
                        id.contains("suse") || id.contains("opensuse")) {
                    return "rpm";
                }
            }

            Process p1 = Runtime.getRuntime().exec("which apt");
            if (p1.waitFor() == 0) {
                return "deb";
            }

            Process p2 = Runtime.getRuntime().exec("which rpm");
            if (p2.waitFor() == 0) {
                return "rpm";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "AppImage";
    }

    private static void runMacOSUpdate(java.io.File outputFile) throws Exception {
        String shContent =
                "#!/bin/bash\n" +
                        "echo \"Iniciando download da atualização do spLinker...\"\n" +
                        "echo \"URL: " + latestDownloadUrl + "\"\n" +
                        "echo \"Destino: " + outputFile.getAbsolutePath() + "\"\n\n" +

                        "curl -L \"" + latestDownloadUrl + "\" -o \"" + outputFile.getAbsolutePath() + "\"\n\n" +

                        "if [ $? -eq 0 ] && [ -f \"" + outputFile.getAbsolutePath() + "\" ]; then\n" +
                        "    echo \"Download concluído com sucesso!\"\n" +
                        "    echo \"Iniciando instalação...\"\n" +
                        "    open \"" + outputFile.getAbsolutePath() + "\"\n" +
                        "    echo \"Instalador iniciado.\"\n" +
                        "else\n" +
                        "    echo \"Falha no download.\"\n" +
                        "    echo \"Por favor, baixe manualmente em: " + latestDownloadUrl + "\"\n" +
                        "    exit 1\n" +
                        "fi\n\n" +

                        "echo \"Pressione Enter para sair...\"\n" +
                        "read\n";

        Path shPath = Files.createTempFile("splinker_update_", ".sh");
        Files.writeString(shPath, shContent);

        shPath.toFile().setExecutable(true);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/bash", shPath.toAbsolutePath().toString());
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
    }
}