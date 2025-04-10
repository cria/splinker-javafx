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

    //1) faz request no github
    //2) Parsing de string para verificar a versão
    //3) verifica se tem versão nova disponível
    //4) Pega a URL do download
    //5) Roda o script

    // Adicione este campo estático para armazenar a URL de download
    static String latestDownloadUrl = "https://github.com/cria/splinker-javafx/releases/download/v0.8.4/spLinker-1.0.msi";

    public static boolean hasNewVersion() throws Exception {
        try {
            // Obter a versão atual do sistema
            var currentVersion = CentralServiceRepository.getCentralServiceData().getSystemVersion();

            // Fazer uma requisição manualmente para obter o array JSON completo
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

            // Ler a resposta
            StringBuilder responseStr = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseStr.append(line);
                }
            }

            // Configurar Gson para ser leniente
            com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
            gsonBuilder.setLenient();
            com.google.gson.Gson gson = gsonBuilder.create();

            // Analisar a resposta
            com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(
                    new java.io.StringReader(responseStr.toString()));
            jsonReader.setLenient(true);

            // Parse como um array de objetos
            com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                    new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
            List<Map<String, Object>> releases = gson.fromJson(jsonReader, typeToken.getType());

            // Verificar se temos releases
            if (releases == null || releases.isEmpty()) {
                return false;
            }

            // Pegar a primeira release (mais recente)
            Map<String, Object> latestRelease = releases.get(0);
            String releaseName = (String) latestRelease.get("name");

            // NOVO: Extrair a URL de download do assets
            if (latestRelease.containsKey("assets") && latestRelease.get("assets") instanceof List) {
                List<?> assets = (List<?>) latestRelease.get("assets");
                for (Object assetObj : assets) {
                    if (assetObj instanceof Map) {
                        Map<?, ?> asset = (Map<?, ?>) assetObj;
                        if (asset.containsKey("browser_download_url")) {
                            String downloadUrl = asset.get("browser_download_url").toString();
                            if (downloadUrl.endsWith(".msi") || downloadUrl.endsWith(".exe")) {
                                // Armazenar a URL para uso posterior
                                latestDownloadUrl = downloadUrl;
                                break;
                            }
                        }
                    }
                }
            }

            // Remover possível prefixo 'v' ou outras letras do nome da release
            String latestVersion = releaseName.replaceAll("[^0-9.]", "");

            // Comparar versões usando semântica de versionamento
            return compareVersions(latestVersion, "0.7") > 0;
        } catch (Exception e) {
            return false; // Em caso de erro, não forçar atualização
        }
    }

    // Método getter para a URL de download
    public static String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }

    /**
     * Compara duas versões seguindo o padrão semântico (major.minor.patch)
     * @param version1 Primeira versão para comparação
     * @param version2 Segunda versão para comparação
     * @return -1 se version1 < version2, 0 se forem iguais, 1 se version1 > version2
     */
    private static int compareVersions(String version1, String version2) {
        // Remover prefixos como "v" ou "release-"
        version1 = version1.replaceAll("[^0-9.]", "");
        version2 = version2.replaceAll("[^0-9.]", "");

        // Dividir as versões em componentes (major, minor, patch)
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        // Determinar o tamanho máximo dos arrays
        int length = Math.max(parts1.length, parts2.length);

        // Comparar cada componente
        for (int i = 0; i < length; i++) {
            // Se um componente não existir, considerar como 0
            int part1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int part2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;

            // Se os componentes são diferentes, retornar a comparação
            if (part1 < part2) {
                return -1;
            }
            if (part1 > part2) {
                return 1;
            }
        }

        // Se chegou até aqui, as versões são iguais
        return 0;
    }

    private static HashMap<String, String> getGithubRelease() {
        return new HashMap<>();
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
            String downloadUrl = "https://github.com/cria/splinker-javafx/releases/download/v0.8.4/spLinker-1.0.msi";

            String batContent =
                    "@echo off\n" +
                            "echo Iniciando download da atualizacao do spLinker...\n" +
                            "echo URL: " + downloadUrl + "\n" +
                            "echo Destino: " + outputFile.getAbsolutePath() + "\n\n" +

                            "REM Usar bitsadmin para download (ferramenta nativa do Windows)\n" +
                            "bitsadmin /transfer \"spLinkerUpdate\" \"" + downloadUrl + "\" \"" + outputFile.getAbsolutePath() + "\"\n\n" +

                            "if exist \"" + outputFile.getAbsolutePath() + "\" (\n" +
                            "    echo Download concluido com sucesso!\n" +
                            "    echo Iniciando instalacao...\n" +
                            "    start \"\" \"" + outputFile.getAbsolutePath() + "\"\n" +
                            "    echo Instalador iniciado.\n" +
                            ") else (\n" +
                            "    echo Falha no download. Usando metodo alternativo...\n" +
                            "    powershell -Command \"& {try { Invoke-WebRequest -Uri '" + downloadUrl + "' -OutFile '" + outputFile.getAbsolutePath() + "' } catch { Write-Host $_.Exception.Message }}\"\n" +
                            "    \n" +
                            "    if exist \"" + outputFile.getAbsolutePath() + "\" (\n" +
                            "        echo Download concluido com sucesso!\n" +
                            "        echo Iniciando instalacao...\n" +
                            "        start \"\" \"" + outputFile.getAbsolutePath() + "\"\n" +
                            "        echo Instalador iniciado.\n" +
                            "    ) else (\n" +
                            "        echo Falha em todos os metodos de download.\n" +
                            "        echo Por favor, baixe manualmente em: " + downloadUrl + "\n" +
                            "        exit 1\n" +
                            "    )\n" +
                            ")\n" +
                            "echo Pressione qualquer tecla para sair...\n" +
                            "pause > nul\n";

            java.nio.file.Path batPath = java.nio.file.Files.createTempFile("splinker_update_", ".bat");
            java.nio.file.Files.writeString(batPath, batContent);

            // Executar o bat file
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", batPath.toAbsolutePath().toString());
            processBuilder.inheritIO();

            // Iniciar o processo
            Process process = processBuilder.start();

            // Aguardar um pouco para garantir que o processo iniciou
            Thread.sleep(2000);

            // Encerrar o aplicativo para permitir a atualização continuar
            if (process.isAlive()) {
                System.exit(0);
            } else {
                // Se o processo já terminou, verificar código de saída
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
