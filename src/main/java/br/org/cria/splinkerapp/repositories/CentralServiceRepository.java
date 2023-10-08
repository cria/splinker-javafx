package br.org.cria.splinkerapp.repositories;

import java.sql.DriverManager;

import com.google.common.base.Strings;

import br.org.cria.splinkerapp.models.CentralService;

public class CentralServiceRepository extends BaseRepository 
{
    public static CentralService getCentraServiceData() throws Exception
    {
        var cmd = "SELECT * FROM CentralServiceConfiguration";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        var uri = result.getString("central_service_uri");
        var url = result.getString("central_service_url");
        result.close();
        conn.close();
        return new CentralService(uri, url);
    };

    public static void saveCentralServiceData(CentralService cserv) throws Exception
    {
        var uri = cserv.getCentralServiceUri();
        var url = cserv.getCentralServiceUrl();
        var isEmptyUri = Strings.isNullOrEmpty(url);
        var isEmptyUrl = Strings.isNullOrEmpty(uri);
        if(!isEmptyUri && !isEmptyUrl)
        {
            cleanTable("CentralServiceConfiguration");
            var cmd = """
                    INSERT INTO CentralServiceConfiguration (central_service_uri, central_service_url) 
                    VALUES(?,?)
                    """;
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var statement = conn.prepareStatement(cmd);
            statement.setString(1, uri);
            statement.setString(2, url);
            statement.executeUpdate();
            conn.close();
        }
        else
        {
            throw new Exception("Os campos não podem ser vazios");
        }
        
    }
    

    
}
