package br.org.cria.splinkerapp.repositories;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.models.ProxyConfiguration;


public class ProxyConfigRepository extends BaseRepository {

    public static ProxyConfiguration getConfiguration() throws Exception {
        ProxyConfiguration proxyConfig = null;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        String sql = "SELECT * FROM ProxyConfiguration LIMIT 1;";
        ResultSet rs = runQuery(sql, conn);
        while (rs.next()) {
            var address = rs.getString("proxy_address");
            var password = rs.getString("proxy_password");
            var port = rs.getString("proxy_port");
            var username = rs.getString("proxy_username");

            proxyConfig = new ProxyConfiguration(address, password, port, username);
        }
        conn.close();
        return proxyConfig;
    }


    public static void saveProxyConfig(ProxyConfiguration proxyConfig) throws Exception {
        cleanTable("ProxyConfiguration");
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var sql = """
                INSERT INTO ProxyConfiguration (proxy_address, proxy_password, proxy_port, proxy_username)
                VALUES (?,?,?,?);""";
        var statement = conn.prepareStatement(sql);
        statement.setString(1, proxyConfig.getAddress());
        statement.setString(2, proxyConfig.getPassword());
        statement.setString(3, proxyConfig.getPort());
        statement.setString(4, proxyConfig.getUsername());
        statement.executeUpdate();
        statement.close();
        conn.close();
    }

    public static boolean isBehindProxyServer() {
        try {
            boolean directConnection = testDirectConnection("https://specieslink.net/");

            System.setProperty("java.net.useSystemProxies", "true");
            boolean proxyConnection = testProxyConnection("https://specieslink.net/");

            if (directConnection != proxyConnection) {
                return true;
            }

            String[] proxyKeys = {"http.proxyHost", "https.proxyHost", "socksProxyHost",
                    "proxyHost", "http.proxyPort", "https.proxyPort"};

            for (String key : proxyKeys) {
                if (System.getProperty(key) != null) {
                    return true;
                }
            }

            if (System.getenv("http_proxy") != null || System.getenv("https_proxy") != null ||
                    System.getenv("HTTP_PROXY") != null || System.getenv("HTTPS_PROXY") != null) {
                return true;
            }
            try {
                List<Proxy> proxies = ProxySelector.getDefault().select(new URI("https://specieslink.net/"));
                for (Proxy proxy : proxies) {
                    if (proxy.type() != Proxy.Type.DIRECT) {
                        return true;
                    }
                }
            } catch (Exception e) {
            }

            try {
                URL url = new URL("https://specieslink.net/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("HEAD");

                conn.connect();
                Map<String, List<String>> headers = conn.getHeaderFields();

                // Procura por cabeçalhos que indiquem proxy
                for (String headerName : headers.keySet()) {
                    if (headerName != null &&
                            (headerName.toLowerCase().contains("proxy") ||
                                    headerName.toLowerCase().contains("via"))) {
                        return true;
                    }
                }

                conn.disconnect();
            } catch (Exception e) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean testDirectConnection(String urlStr) {
        try {
            System.clearProperty("http.proxyHost");
            System.clearProperty("https.proxyHost");
            System.clearProperty("socksProxyHost");

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("HEAD");
            conn.connect();

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean testProxyConnection(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("HEAD");
            conn.connect();

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private static void checkDirectProxyConnection(String host, int port) throws Exception {
        java.net.Socket socket = new java.net.Socket();
        socket.connect(new InetSocketAddress(host, port), 1000);
        socket.close();
    }


}
