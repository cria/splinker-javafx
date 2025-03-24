package br.org.cria.splinkerapp.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public static void setCurrentToken(String token) {
        System.setProperty("splinker_token", token);
    }

    public static String getCurrentTokenSigla() {
        String token = System.getProperty("splinker_token");
        if (token == null) {
            return "";
        }
        try (Connection conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
             PreparedStatement stmt = conn.prepareStatement("SELECT dataset_acronym FROM DataSetConfiguration WHERE token = ?")) {
            stmt.setString(1, token);
            try (ResultSet result = stmt.executeQuery()) {
                return result.next() ?
                        result.getString("dataset_acronym") :
                        "";
            }
        } catch (SQLException e) {
            return "";
        }
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
}
