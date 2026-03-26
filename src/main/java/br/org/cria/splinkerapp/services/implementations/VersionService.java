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


    public static String getVersion() throws IOException {
        InputStream in = VersionService.class.getResourceAsStream("/version.properties");
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("app.version");

        return version;
    }

    public static String getReleaseCurrentVersion() throws Exception {
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

        Map<String, Object> latestRelease = releases.get(0);

        String releaseName = latestRelease.get("name") != null
                ? latestRelease.get("name").toString()
                : null;

        if (releaseName == null || releaseName.isBlank()) {
            Object tagName = latestRelease.get("tag_name");
            if (tagName != null) {
                releaseName = tagName.toString();
            }
        }

        if (releaseName == null || releaseName.isBlank()) {
            return null;
        }

        return releaseName.replaceAll("[^0-9.]", "");
    }
}
