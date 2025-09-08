package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;

public class HttpService {

    public static Object getJson(String url) throws Exception {
        String line;
        HttpURLConnection connection;
        var urlConn = new URI(url).toURL();
        var response = new StringBuffer();
        var isBehindProxy = ProxyConfigRepository.isBehindProxyServer();

        // Garante suporte a protocolos TLS mais comuns
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        if (isBehindProxy) {
            var proxyConfig = ProxyConfigRepository.getConfiguration();
            var proxyHost = proxyConfig.getAddress();
            var proxyPort = Integer.parseInt(proxyConfig.getPort());
            var proxyUser = proxyConfig.getUsername();
            var proxyPass = proxyConfig.getPassword();

            var proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

            // Se houver usuário e senha, configura autenticação
            if (proxyUser != null && !proxyUser.isEmpty()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                        }
                        return null;
                    }
                });

                // Opcional: enviar credenciais no primeiro request para evitar 407
                String auth = proxyUser + ":" + proxyPass;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection = (HttpURLConnection) urlConn.openConnection(proxy);
                connection.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
            } else {
                connection = (HttpURLConnection) urlConn.openConnection(proxy);
            }

            connection.setDoOutput(true);
        } else {
            connection = (HttpURLConnection) urlConn.openConnection();
        }

        connection.setRequestMethod("GET");

        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        connection.disconnect();

        var stringResponse = response.toString();

        // Configura Gson para ser leniente com JSON mal formatado
        com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
        gsonBuilder.setLenient();
        com.google.gson.Gson gson = gsonBuilder.create();

        // Detecta se é um array ou objeto
        if (stringResponse.trim().startsWith("[")) {
            com.google.gson.reflect.TypeToken<List<Map<String, Object>>> typeToken =
                    new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>() {};
            return gson.fromJson(stringResponse, typeToken.getType());
        } else {
            return gson.fromJson(stringResponse, HashMap.class);
        }
    }


}
