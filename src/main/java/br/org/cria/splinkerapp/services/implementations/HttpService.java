package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;

public class HttpService {

    public static Map<String, Object> getJson(String url) throws Exception {
        String line;
        HttpURLConnection connection;
        var urlConn = new URI(url).toURL();
        var response = new StringBuffer();
        var isBehindProxy = ProxyConfigRepository.isBehindProxyServer();
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        if (isBehindProxy) {
            var proxyConfig = ProxyConfigRepository.getConfiguration();
            var proxyHost = proxyConfig.getAddress();
            var proxyPort = Integer.valueOf(proxyConfig.getPort());
            var proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            connection = (HttpURLConnection) urlConn.openConnection(proxy);
            connection.setDoOutput(true);
        } else {
            connection = (HttpURLConnection) urlConn.openConnection();
        }

        connection.setRequestMethod("GET");
        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();
        var stringResponse = response.toString();
        var isList = stringResponse.charAt(0) == '[';
        var lastIndex = stringResponse.length() - 1;
        var content = isList ? stringResponse.substring(1, lastIndex) : stringResponse;
        HashMap<String, Object> json = new Gson().fromJson(content, HashMap.class);
        return json;
    }

}
