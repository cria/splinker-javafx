package br.org.cria.splinkerapp.repositories;

import java.sql.DriverManager;

import com.google.common.base.Strings;

import br.org.cria.splinkerapp.models.CentralService;

public class CentralServiceRepository extends BaseRepository {
    public static CentralService getCentralServiceData() throws Exception {
        var cmd = "SELECT * FROM CentralServiceConfiguration";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        var url = result.getString("central_service_url");
        var systemVersion = result.getString("last_system_version");
        result.close();
        conn.close();
        return new CentralService(url, systemVersion);
    }

    public static void saveCentralServiceData(String url, String systemVersion) throws Exception {
        saveCentralServiceData(new CentralService(url, systemVersion));

    }

    private static void saveCentralServiceData(CentralService cserv) throws Exception {
        var isEmptyUrl = Strings.isNullOrEmpty(cserv.getCentralServiceUrl());
        if (!isEmptyUrl) {
            cleanTable("CentralServiceConfiguration");
            var cmd = """
                    INSERT INTO CentralServiceConfiguration (central_service_url, last_system_version) 
                    VALUES(?,?)
                    """;
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var statement = conn.prepareStatement(cmd);
            statement.setString(1, cserv.getCentralServiceUrl());
            statement.setString(2, cserv.getSystemVersion());

            statement.executeUpdate();
            statement.close();
            conn.close();
        } else {
            throw new Exception("O campo não pode ser vazio");
        }
    }

    public static String getCurrentVersion() throws Exception {
        var cmd = "SELECT last_system_version FROM CentralServiceConfiguration;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        var system_version = result.getString("last_system_version");
        conn.close();
        return system_version;
    }
}
