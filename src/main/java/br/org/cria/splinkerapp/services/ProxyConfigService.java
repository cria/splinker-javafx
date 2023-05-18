package br.org.cria.splinkerapp.services;

import java.sql.ResultSet;

import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.services.interfaces.IProxyConfigService;

public class ProxyConfigService extends BaseService implements IProxyConfigService {
    
    @Override
    public ProxyConfiguration getConfiguration() {
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
                //String address, String password, String port, String username
                proxyConfig = new ProxyConfiguration(rs.getString("address"), 
                                                        rs.getString("password"),
                                                        rs.getString("port"), 
                                                        rs.getString("username"));
                
            } 
            
        } catch (Exception e) {
            System.out.println(e.toString());
        }
            return proxyConfig;
     
        
    }

    @Override
    public boolean saveProxyConfig(ProxyConfiguration proxyConfig) {
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
    
}
