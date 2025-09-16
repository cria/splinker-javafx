package br.org.cria.splinkerapp.services.implementations;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class HttpService {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Type LIST_OF_MAP = new TypeToken<List<Map<String, Object>>>() {}.getType();

    public static Object getJson(String url) throws Exception {
        return getJson(url, /*insecureSkipTlsVerify=*/false);
    }

    /** Se insecureSkipTlsVerify=true, ignora CA e hostname (equivalente a curl -k). */
    public static Object getJson(String url, boolean insecureSkipTlsVerify) throws Exception {
        URI uri = new URI(url);

        // Permite Basic auth em túnel/proxy
        //System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        //System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");

        HttpClient client = buildHttpClient(insecureSkipTlsVerify, uri.getHost());

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "curl/8.7.1");

        if (ProxyConfigRepository.isBehindProxyServer()) {
            var cfg = ProxyConfigRepository.getConfiguration();
            String user = orEmpty(cfg.getUsername());
            String pass = orEmpty(cfg.getPassword());
            if (!user.isEmpty()) {
                String basic = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
                req.header("Proxy-Authorization", "Basic " + basic);
            }
        }

        HttpResponse<String> resp = client.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = resp.statusCode();
        if (status >= 200 && status < 300) {
            String body = resp.body() == null ? "" : resp.body().trim();
            return body.startsWith("[") ? GSON.fromJson(body, LIST_OF_MAP) : GSON.fromJson(body, HashMap.class);
        } else {
            throw new IOException("HTTP " + status + " ao acessar " + uri + " - body: " + resp.body());
        }
    }

    private static HttpClient buildHttpClient(boolean insecure, String targetHost)
            throws Exception {
        HttpClient.Builder b = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(90));

        // Proxy + autenticação (se configurado)
        if (ProxyConfigRepository.isBehindProxyServer() && !isLocalHost(targetHost)) {
            var cfg = ProxyConfigRepository.getConfiguration();
            String host = orEmpty(cfg.getAddress());
            int port = safeInt(orEmpty(cfg.getPort()));
            b.proxy(ProxySelector.of(new InetSocketAddress(host, port)));

            String user = orEmpty(cfg.getUsername());
            String pass = orEmpty(cfg.getPassword());
            if (!user.isEmpty()) {
                b.authenticator(new Authenticator() {
                    @Override protected PasswordAuthentication getPasswordAuthentication() {
                        return getRequestorType() == RequestorType.PROXY
                                ? new PasswordAuthentication(user, pass.toCharArray())
                                : null;
                    }
                });
            }
        }

        if (insecure) {
            // Trust-all (NÃO usar em produção)
            SSLContext ctx = SSLContext.getInstance("TLS");
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            ctx.init(null, trustAll, new SecureRandom());

            // Desabilita verificação de hostname
            SSLParameters params = new SSLParameters();
            params.setEndpointIdentificationAlgorithm(null);

            b.sslContext(ctx).sslParameters(params);
        }

        return b.build();
    }

    private static boolean isLocalHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1");
    }

    private static String orEmpty(String s) { return s == null ? "" : s; }
    private static int safeInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
}
