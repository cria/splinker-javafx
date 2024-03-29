package br.org.cria.splinkerapp.services.implementations;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import com.google.gson.reflect.TypeToken;

import br.org.cria.splinkerapp.config.LockFileManager;
import br.org.cria.splinkerapp.models.GithubAPIResponse;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;

public class SpLinkerUpdateService 
{
    String command = "Powershell.exe";
    String scriptPath = "/scripts/update/softwareUpdate.";
    String operatingSystemName = SystemUtils.OS_NAME.toLowerCase();
    String scriptFileExtension = "ps1";
    String scriptArgs = "";
    String params = "-Command";
    boolean isRunningOnWindows = operatingSystemName.contains("windows");
    GithubAPIResponse lastRelease;
    String packageExtension = "msi";
    static final String githubURL = "https://api.github.com/repos/cria/splinker-javafx/releases";
    

    public SpLinkerUpdateService() throws Exception
    {
        getReleaseData();
    }

    private void getReleaseData() throws Exception
    {
        var type = new TypeToken<ArrayList<GithubAPIResponse>>(){}.getType();
        var releaseList = (ArrayList<GithubAPIResponse>) HttpService.getJson(githubURL, type);
        lastRelease = releaseList.get(0);
    }

    public boolean hasNewVersion() throws Exception
    {
        var strVersion = CentralServiceRepository.getCentralServiceData().getSystemVersion();
        var currentVersion = Integer.parseInt(strVersion.replaceAll("[^0-9]", ""));        
        var releaseName = lastRelease.getName();
        var lastVersion = getVersionNumber(releaseName);
        var needUpdates = lastVersion > currentVersion;

        return needUpdates;
    }

    private static int getVersionNumber(String releaseName)
    {
        var intPart = releaseName.replaceAll("[^0-9]", "");
        return Integer.parseInt(intPart);
    }

    private void defineOperatingSystemConfiguration()
    {
        if (isRunningOnWindows)
        {
            var versionNumber = operatingSystemName.replaceAll("[^0-9]", "");
            var hasVersion = versionNumber != "";
            var isOldWindowsVersion = true;//hasVersion && Integer.parseInt(versionNumber) < 11;
            
            if(isOldWindowsVersion)
            {
                scriptFileExtension = "bat";
                command = "cmd"; //? 
                params = "";
            }
            return;
        }
        
        command = "bash";
        scriptFileExtension = "sh";
    }

    private void definePackageExtension() throws IOException
    {
        if(!isRunningOnWindows)
        {
            var builder = new ProcessBuilder("which dpkg");
            var process = builder.start();
            var commandOutput = IOUtils.toString(process.getInputStream(), Charset.forName("UTF-8"));
            var hasDpkgInstall =  commandOutput.contains("dpkg");
            packageExtension = hasDpkgInstall ? ".deb": ".rpm";    
        }
    }

    
    private ProcessBuilder configureUpdateProcess() throws Exception
    {
        definePackageExtension();
        var scriptWithExtension = "%s%s".formatted(scriptPath, scriptFileExtension);
        var resource = getClass().getResource(scriptWithExtension);
        var resourcePath = resource.toURI().getPath();
        var fullScriptPath = isRunningOnWindows ? resourcePath.substring(1) :resourcePath;
        var assets = lastRelease.getAssets().stream();
        var asset = assets.filter(a -> a.getName().contains(packageExtension)).findFirst().get();
        var outputfile = asset.getName();
        var downloadLink = asset.getBrowserDownloadUrl();
        var values =  "%s \"%s\" \"%s\";".formatted(fullScriptPath, downloadLink, outputfile);
        var processBuilder = new ProcessBuilder();
        processBuilder.command(command, params, values);
        processBuilder.inheritIO();
        
        return processBuilder;
    }

    public void runSoftwareUpdate() throws Exception
    {
        getReleaseData();
        defineOperatingSystemConfiguration();
        var processBuilder = configureUpdateProcess();
        var shellProcess = processBuilder.start();
        LockFileManager.deleteLockfile();
        System.exit(0);
    }
}
