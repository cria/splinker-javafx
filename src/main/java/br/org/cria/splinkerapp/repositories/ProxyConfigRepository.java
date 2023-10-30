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



public class ProxyConfigRepository {

    public static ProxyConfiguration getConfiguration() throws Exception
    {
        ProxyConfiguration proxyConfig = null;    
        var conn = DriverManager.getConnection("jdbc:sqlite:splinker.db");
        String sql = """
                        SELECT proxy_username, proxy_password, proxy_port, proxy_address 
                        FROM ProxyConfiguration LIMIT 1;
                        """;
        var ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while(rs.next())
        {
            var address = rs.getString("proxy_address");
            var password = rs.getString("proxy_password");
            var port = rs.getString("proxy_port");
            var username = rs.getString("proxy_username");
        
            proxyConfig = new ProxyConfiguration(address, password, port, username);            
        } 
        return proxyConfig;
    }

    
    public static void saveProxyConfig(ProxyConfiguration proxyConfig) throws Exception
    {
        var conn = DriverManager.getConnection("jdbc:sqlite:splinker.db");
        var statement = conn.createStatement();
        var sql = """
                DELETE FROM ProxyConfiguration; 
                INSERT INTO ProxyConfiguration (proxy_address, proxy_password, proxy_port,proxy_username)
                VALUES ('%s', '%s', '%s', '%s');""".formatted(
        proxyConfig.getAddress(),proxyConfig.getPassword(),
        proxyConfig.getPort(), proxyConfig.getUsername());
        statement.executeUpdate(sql);
        statement.close();
        conn.close();
    }

    public static boolean isBehindProxyServer() 
    {
        boolean hasProxy = false;
        try {
            System.setProperty("java.net.useSystemProxies","true");
            List<Proxy> proxies = ProxySelector.getDefault().select(
                        new URI("https://www.cria.org.br/"));
    
            for (Iterator<Proxy> iter = proxies.iterator(); iter.hasNext(); ) {
    
                var proxy = iter.next();
                var addr = (InetSocketAddress)proxy.address();
                hasProxy = addr != null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally
        {
            return hasProxy;
        }
    }
    
    
    
  
}
