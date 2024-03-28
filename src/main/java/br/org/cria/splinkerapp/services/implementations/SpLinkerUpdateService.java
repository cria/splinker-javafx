package br.org.cria.splinkerapp.services.implementations;

import java.util.HashMap;

import org.apache.commons.lang3.SystemUtils;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;

public class SpLinkerUpdateService 
{
    static String command = "Powershell.exe";
    static String scriptPath = "/scripts/update/softwareUpdate.";
    static String operatingSystem = SystemUtils.OS_NAME.toLowerCase();
    static String fileExtension = "ps1";
    static String params = "-Command";
    static String githubURL = "https://api.github.com/repos/cria/splinker-javafx/releases";
    
    public static boolean hasNewVersion() throws Exception
    {
        var needUpdates = false;
        var strVersion = CentralServiceRepository.getCentralServiceData().getSystemVersion();
        var currentVersion = Double.parseDouble(strVersion.replaceAll("[^0-9]", ""));
        var response = HttpService.getJson(githubURL);
        var releaseName = response.get("name").toString();
        var lastVersion = getVersionNumber(releaseName);
        
        needUpdates = lastVersion > currentVersion;
        

        return needUpdates;
    }

    private static Double getVersionNumber(String releaseName)
    {
        var doublePart = releaseName.replaceAll("[^0-9]", "");
        return Double.parseDouble(doublePart);
    }

    private static HashMap<String, String> getGithubRelease()
    {
        return new HashMap<>();
    }

    public static void verifyOSVersion()
    {
        
        if (operatingSystem.contains("windows"))
        {
            var versionNumber = operatingSystem.replaceAll("[^0-9]", "");
            var hasVersion = versionNumber != "";
            var isOldWindowsVersion = hasVersion && Integer.parseInt(versionNumber) < 11;
            
            if(isOldWindowsVersion)
            {
                fileExtension = "bat";
                command = "cmd"; //? 
                params = "";
            }
        }
        else
        {
            command = "bash";
            fileExtension = "sh";
        }

      
    }
    
    public static void runSoftwareUpdate()
    {
        try 
        {
        
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
