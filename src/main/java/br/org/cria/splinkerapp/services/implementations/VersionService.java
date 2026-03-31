package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class VersionService {

    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/cria/splinker-javafx/releases";

    public static String getVersion() {
        InputStream in = VersionService.class.getResourceAsStream("/version.properties");
        Properties props = new Properties();
        try {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String version = props.getProperty("app.version");

        return version;
    }

    public static String getReleaseCurrentVersion() throws Exception {
        Map<String, Object> latestRelease = getLatestRelease();
        if (latestRelease == null) {
            return null;
        }

        String releaseName = getString(latestRelease.get("name"));
        if (releaseName == null || releaseName.isBlank()) {
            releaseName = getString(latestRelease.get("tag_name"));
        }

        if (releaseName == null || releaseName.isBlank()) {
            return null;
        }

        return normalizeVersion(releaseName);
    }

    public static String getLatestReleaseDownloadUrlForCurrentOS() throws Exception {
        Map<String, Object> latestRelease = getLatestRelease();
        if (latestRelease == null) {
            return null;
        }

        Object assetsObject = latestRelease.get("assets");
        if (!(assetsObject instanceof List)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assets = (List<Map<String, Object>>) assetsObject;

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return selectWindowsAsset(assets);
        }

        if (osName.contains("linux") || osName.contains("nux") || osName.contains("nix")) {
            return selectLinuxAsset(assets);
        }

        return null;
    }

    public static boolean isNewerVersion(String localVersion, String remoteVersion) {
        String safeLocal = normalizeVersion(localVersion);
        String safeRemote = normalizeVersion(remoteVersion);

        String[] localParts = safeLocal.split("\\.");
        String[] remoteParts = safeRemote.split("\\.");

        int length = Math.max(localParts.length, remoteParts.length);

        for (int i = 0; i < length; i++) {
            int localPart = i < localParts.length ? parseVersionPart(localParts[i]) : 0;
            int remotePart = i < remoteParts.length ? parseVersionPart(remoteParts[i]) : 0;

            if (remotePart > localPart) {
                return true;
            }
            if (remotePart < localPart) {
                return false;
            }
        }

        return false;
    }

    private static int parseVersionPart(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "0.0.0";
        }

        String normalized = version.replaceAll("[^0-9.]", "");
        return normalized.isBlank() ? "0.0.0" : normalized;
    }

    private static Map<String, Object> getLatestRelease() throws Exception {
        URL url = new URL(GITHUB_RELEASES_URL);
        HttpURLConnection connection;

        boolean isBehindProxy = ProxyConfigRepository.isBehindProxyServer();
        if (isBehindProxy) {
            var proxyConfig = ProxyConfigRepository.getConfiguration();
            String proxyHost = proxyConfig.getAddress();
            int proxyPort = Integer.parseInt(proxyConfig.getPort());
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            connection = (HttpURLConnection) url.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SpLinker-App");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("Falha ao consultar releases no GitHub. HTTP " + statusCode);
        }

        StringBuilder responseStr = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                responseStr.append(line);
            }
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setLenient();
        Gson gson = gsonBuilder.create();

        JsonReader jsonReader = new JsonReader(new StringReader(responseStr.toString()));
        jsonReader.setLenient(true);

        TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
        List<Map<String, Object>> releases = gson.fromJson(jsonReader, typeToken.getType());

        if (releases == null || releases.isEmpty()) {
            return null;
        }

        return releases.get(0);
    }

    private static String selectWindowsAsset(List<Map<String, Object>> assets) {
        for (Map<String, Object> asset : assets) {
            String url = getString(asset.get("browser_download_url"));
            if (url == null) {
                continue;
            }
            if (url.endsWith(".jar")) {
                return url;
            }
        }

        for (Map<String, Object> asset : assets) {
            String url = getString(asset.get("browser_download_url"));
            if (url == null) {
                continue;
            }
            if (url.endsWith(".msi") || url.endsWith(".exe")) {
                return url;
            }
        }

        return null;
    }

    private static String selectLinuxAsset(List<Map<String, Object>> assets) {
        for (Map<String, Object> asset : assets) {
            String url = getString(asset.get("browser_download_url"));
            if (url == null) {
                continue;
            }
            if (url.endsWith(".jar")) {
                return url;
            }
        }

        for (Map<String, Object> asset : assets) {
            String url = getString(asset.get("browser_download_url"));
            if (url == null) {
                continue;
            }
            if (url.endsWith(".deb") || url.endsWith(".rpm") || url.endsWith(".AppImage")) {
                return url;
            }
        }

        return null;
    }

    private static String getString(Object value) {
        return value != null ? value.toString() : null;
    }
}