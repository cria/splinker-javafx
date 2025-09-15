package br.org.cria.splinkerapp.repositories;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import br.org.cria.splinkerapp.models.ProxyConfiguration;
import io.sentry.Sentry;


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

    @SuppressWarnings("finally")
    public static boolean isBehindProxyServer() {
        boolean hasProxy = false;
        try {
            System.setProperty("java.net.useSystemProxies", "true");
            List<Proxy> proxies = ProxySelector.getDefault().select(
                    new URI("https://www.cria.org.br/"));

            for (Iterator<Proxy> iter = proxies.iterator(); iter.hasNext(); ) {

                var proxy = iter.next();
                var addr = (InetSocketAddress) proxy.address();
                hasProxy = addr != null;
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
        } finally {
            return false;
        }
    }


}
