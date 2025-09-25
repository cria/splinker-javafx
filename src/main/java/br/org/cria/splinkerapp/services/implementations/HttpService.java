package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;

public class HttpService {

    public static Object getJson(String url) throws Exception {
        String line;
        HttpURLConnection connection;
        var urlConn = new URI(url).toURL();
        var response = new StringBuffer();
        var isBehindProxy = ProxyConfigRepository.isBehindProxyServer();
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        ProxyConfiguration proxyConfig = ProxyConfigRepository.getConfiguration();
        if (isBehindProxy && !proxyConfig.getAddress().isEmpty()) {
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

        // Configurar Gson para ser leniente com JSON mal formatado
        com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
        gsonBuilder.setLenient();
        com.google.gson.Gson gson = gsonBuilder.create();

        // Identificar se é um array ou objeto e fazer o parse adequado
        if (stringResponse.trim().startsWith("[")) {
            // É um array JSON
            com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                    new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
            return gson.fromJson(stringResponse, typeToken.getType());
        } else {
            // É um objeto JSON
            return gson.fromJson(stringResponse, HashMap.class);
        }
    }

}
