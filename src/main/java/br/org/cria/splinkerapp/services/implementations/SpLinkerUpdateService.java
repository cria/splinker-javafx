package br.org.cria.splinkerapp.services.implementations;

import java.util.HashMap;

import org.apache.commons.lang3.SystemUtils;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;

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

    public static boolean hasNewVersion() throws Exception {
        var needUpdates = false;
        // var strVersion = CentralServiceRepository.getCentralServiceData().getSystemVersion();
        // //TODO: Remover os pontos dos números de versão, transformar em inteiro e comparar para ver quem é maior
        // var currentVersion = Double.parseDouble(strVersion.replaceAll("[^0-9]", ""));
        // var response = HttpService.getJson(githubURL);
        // var releaseName = response.get("name").toString();
        // var lastVersion = getVersionNumber(releaseName);

        // needUpdates = lastVersion > currentVersion;


        return needUpdates;
    }

    private static Double getVersionNumber(String releaseName) {
        var doublePart = releaseName.replaceAll("[^0-9]", "");
        return Double.parseDouble(doublePart);
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

    public static void runSoftwareUpdate() {
        try {
            var scriptWithExtension = "%s%s".formatted(scriptPath, fileExtension);
            var resource = SpLinkerUpdateService.class.getResource(scriptWithExtension);
            var resourcePath = resource.toURI().getPath();
            var fullScriptPath = operatingSystem.contains("windows") ? resourcePath.substring(1) : resourcePath;
            var home = System.getProperty("user.home");
            var outputPath = "%s/Downloads/splinker_new_version.msi".formatted(home);
            var downloadLink = "https://swupdate.openvpn.net/downloads/connect/openvpn-connect-3.3.7.2979_signed.msi";

            var processBuilder = new ProcessBuilder();
            var values = "%s \"%s\" \"%s\";".formatted(fullScriptPath, downloadLink, outputPath);
            processBuilder.command(command, params, values);
            processBuilder.inheritIO(); // Redirect PowerShell's input, output, and error streams to the Java application

            //processBuilder.environment().put("$env:DOWNLOAD_LINK", downloadUrl);

            Process shellProcess = processBuilder.start();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
