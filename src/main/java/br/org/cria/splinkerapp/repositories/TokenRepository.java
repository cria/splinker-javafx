package br.org.cria.splinkerapp.repositories;

import java.sql.DriverManager;

public class TokenRepository extends BaseRepository {

    public static String getCurrentToken() throws Exception 
    {
        var token = System.getProperty("splinker_token");
        if (token == null) 
        {
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

    public static void setCurrentToken(String token) {
        System.setProperty("splinker_token", token);
    }

}
