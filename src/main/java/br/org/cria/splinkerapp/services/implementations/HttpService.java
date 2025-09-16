package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpService {

    public static Object getJson(String urlStr) throws Exception {
        // Faz a requisição GET
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Lê a resposta
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        String stringResponse = response.toString();

        // Converte JSON em Map
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
