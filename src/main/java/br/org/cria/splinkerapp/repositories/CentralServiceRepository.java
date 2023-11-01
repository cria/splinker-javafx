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
        var url = result.getString("central_service_url");
        result.close();
        conn.close();
        return new CentralService(url);
    };

    public static void saveCentralServiceData(String url) throws Exception
    {
        saveCentralServiceData(new CentralService(url));

    }

    public static void saveCentralServiceData(CentralService cserv) throws Exception
    {
        cleanTable("CentralServiceConfiguration");
        var url = cserv.getCentralServiceUrl();
        var isEmptyUrl = Strings.isNullOrEmpty(url);
        if(!isEmptyUrl)
        {
            cleanTable("CentralServiceConfiguration");
            var cmd = """
                    INSERT INTO CentralServiceConfiguration (central_service_url) 
                    VALUES(?)
                    """;
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var statement = conn.prepareStatement(cmd);
            statement.setString(1, url);
            
            statement.executeUpdate();
            statement.close();
            conn.close();
        }
        else
        {
            throw new Exception("O campo não pode ser vazio");
        }
        
    }
    

    
}
