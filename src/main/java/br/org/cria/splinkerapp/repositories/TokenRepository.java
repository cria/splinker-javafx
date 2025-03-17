package br.org.cria.splinkerapp.repositories;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

public class TokenRepository extends BaseRepository {

    public static String getCurrentToken() throws Exception {
        var token = System.getProperty("splinker_token");
        if (token == null) {
            var cmd = "SELECT token FROM DataSetConfiguration LIMIT 1;";
            var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
            var result = runQuery(cmd, conn);
            var hasToken = result.getString("token") != null;
            token = hasToken ? result.getString("token") : "";
            result.close();
            conn.close();
            setCurrentToken(token);
        }

        return token;
    }

    public static Collection<String> getTokens() throws Exception {
        String query = "SELECT dataset_acronym FROM DataSetConfiguration;";
        Collection<String> tokens = new ArrayList<>();
        try (var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
             ResultSet rs = runQuery(query, conn)) {
            while (rs.next()) {
                tokens.add(rs.getString("dataset_acronym"));
            }
        }
        return tokens;
    }


    public static void setCurrentToken(String token) {
        System.setProperty("splinker_token", token);
    }

}
