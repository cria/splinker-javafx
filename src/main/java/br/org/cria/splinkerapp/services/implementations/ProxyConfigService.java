package br.org.cria.splinkerapp.services.implementations;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.services.BaseService;
import br.org.cria.splinkerapp.services.interfaces.IProxyConfigService;

public class ProxyConfigService extends BaseService implements IProxyConfigService {
    
    @Override
    public ProxyConfiguration getConfiguration() 
    {
        ProxyConfiguration proxyConfig = null;    
        try {
            var conn = getConnection();
            String sql = """
                        SELECT username, password, port, address 
                        FROM ProxyConfiguration LIMIT 1;
                        """;
            var ps = conn.prepareStatement(sql);

            //Executa o comando de consulta aonde guarda os dados retornados dentro do ResultSet.
            //Pelo fato de gerar uma lista de valores, é necessário percorrer os dados através do laço while
            ResultSet rs = ps.executeQuery();
            
            //Faz a verificação de enquanto conter registros, percorre e resgata os valores
            while(rs.next())
            {
                var address = rs.getString("address");
                var password = rs.getString("password");
                var port = rs.getString("port");
                var username = rs.getString("username");
                
                proxyConfig = new ProxyConfiguration(address, password, port, username);
                
            } 
            
        } catch (Exception e) {
            System.out.println(e);
        }
            return proxyConfig;
    }

    @Override
    public boolean saveProxyConfig(ProxyConfiguration proxyConfig) 
    {
        var result = false;
        try 
        {
            String sql = """
                INSERT INTO ProxyConfiguration (address, password, port,username)
                VALUES ('%s', '%s', '%s', '%s');""".formatted(
                proxyConfig.getAddress(),proxyConfig.getPassword(),
                proxyConfig.getPort(), proxyConfig.getUsername());
                var conn = getConnection();
                var statement = conn.createStatement();
                var affectedRows = statement.executeUpdate(sql);
                result = affectedRows > 0;
                System.out.println(result);   
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        return result;
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
        return hasProxy;
    }
    
}
