package br.org.cria.splinkerapp.services.implementations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpService {

    public static Object getJson(String urlStr) throws Exception {
        // 1) Preferir IPv4 (evita rotas IPv6 problemáticas)
        System.setProperty("java.net.preferIPv4Stack", "true");

        // 2) Usar Proxy do sistema (PAC/WinINet) – ele quem sabe quando é DIRECT
        System.setProperty("java.net.useSystemProxies", "true");

        // 3) Tentar DIRECT primeiro com timeout curto; se falhar, tentar via proxy
        URI uri = URI.create(urlStr);
        int connectTimeoutMs = 7000;
        int readTimeoutMs    = 15000;

        // Diagnóstico: qual rota o sistema sugere?
        List<Proxy> routes = ProxySelector.getDefault() != null
                ? ProxySelector.getDefault().select(uri)
                : Collections.singletonList(Proxy.NO_PROXY);

        // 3.a) Tenta DIRECT (igual ao curl)
        try {
            return fetch(urlStr, Proxy.NO_PROXY, connectTimeoutMs, readTimeoutMs);
        } catch (SocketTimeoutException te) {
            // DIRECT time-out: tentaremos via proxy (se houver)
        } catch (IOException ioe) {
            // Se for erro rápido de conexão, ainda vale tentar proxy
        }

        // 3.b) Tentar via proxy(s) do sistema
        for (Proxy p : routes) {
            if (p == null || p.type() == Proxy.Type.DIRECT) continue; // já tentamos DIRECT
            try {
                // (Opcional) Se seu proxy exige auth, defina aqui:
                // Authenticator.setDefault(new Authenticator() {
                //   @Override protected PasswordAuthentication getPasswordAuthentication() {
                //     return new PasswordAuthentication("usuario", "senha".toCharArray());
                //   }
                // });

                return fetch(urlStr, p, connectTimeoutMs, readTimeoutMs);
            } catch (IOException ignored) {
                // tenta próximo proxy, se existir
            }
        }

        throw new IOException("Falha ao acessar a URL tanto DIRECT quanto via proxy.");
    }

    private static Object fetch(String urlStr, Proxy proxy, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) (
                proxy != null ? url.openConnection(proxy) : url.openConnection()
        );

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);

        // Alguns ambientes tratam melhor um UA “de navegador”
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) JavaHttpURLConnection");

        int code;
        try {
            code = conn.getResponseCode();
        } catch (SocketTimeoutException te) {
            conn.disconnect();
            throw te;
        }

        // Ler corpo (ok ou erro) para evitar conexões penduradas
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String body = readAll(is);
        conn.disconnect();

        if (code == 407) {
            throw new IOException("407 Proxy Authentication Required (tentativa via proxy).");
        }
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " ao acessar " + urlStr + " | body: " + safeTail(body, 500));
        }

        // Parse JSON
        Gson gson = new GsonBuilder().setLenient().create();
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            return gson.fromJson(trimmed, new TypeToken<List<Map<String, Object>>>() {}.getType());
        } else {
            return gson.fromJson(trimmed, HashMap.class);
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = in.readLine()) != null; ) sb.append(line);
            return sb.toString();
        }
    }

    private static String safeTail(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(s.length() - max);
    }
}
