package br.org.cria.splinkerapp.repositories;

import com.google.common.base.Strings;

import br.org.cria.splinkerapp.models.CentralService;

public class CentralServiceRepository extends BaseRepository {
    public static CentralService getCentralServiceData() throws Exception {
        var cmd = "SELECT * FROM CentralServiceConfiguration LIMIT 1";
        try (var conn = openLocalConnection();
             var statement = conn.prepareStatement(cmd);
             var result = statement.executeQuery()) {
            if (result.next()) {
                var url = result.getString("central_service_url");
                var systemVersion = result.getString("last_system_version");
                return new CentralService(url, systemVersion);
            }
            return new CentralService(null, null);
        }
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
            try (var conn = openLocalConnection();
                 var statement = conn.prepareStatement(cmd)) {
                statement.setString(1, cserv.getCentralServiceUrl());
                statement.setString(2, cserv.getSystemVersion());
                statement.executeUpdate();
            }
        } else {
            throw new Exception("O campo não pode ser vazio");
        }
    }

    public static String getCurrentVersion() throws Exception {
        var cmd = "SELECT last_system_version FROM CentralServiceConfiguration LIMIT 1;";
        try (var conn = openLocalConnection();
             var statement = conn.prepareStatement(cmd);
             var result = statement.executeQuery()) {
            return result.next() ? result.getString("last_system_version") : null;
        }
    }
}
