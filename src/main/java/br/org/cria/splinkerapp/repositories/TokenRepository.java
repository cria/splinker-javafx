package br.org.cria.splinkerapp.repositories;

import java.sql.Connection;
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
            try (var conn = openLocalConnection();
                 var statement = conn.prepareStatement(cmd);
                 var result = statement.executeQuery()) {
                token = result.next() && result.getString("token") != null ? result.getString("token") : "";
            }
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
        try (Connection conn = openLocalConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT dataset_acronym FROM DataSetConfiguration WHERE token = ?")) {
            stmt.setString(1, token);
            try (ResultSet result = stmt.executeQuery()) {
                return result.next() ?
                        result.getString("dataset_acronym") :
                        "";
            }
        } catch (SQLException e) {
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static Collection<String> getTokens() throws Exception {
        String query = "SELECT dataset_acronym FROM DataSetConfiguration;";
        Collection<String> tokens = new ArrayList<>();
        try (var conn = openLocalConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tokens.add(rs.getString("dataset_acronym"));
            }
        }
        return tokens;
    }

    public static Collection<String> getAcronyms() throws Exception {
        String query = "SELECT dataset_acronym FROM DataSetConfiguration;";
        Collection<String> tokens = new ArrayList<>();
        try (var conn = openLocalConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tokens.add(rs.getString("dataset_acronym"));
            }
        }
        return tokens;
    }

    public static String getCurrentTokenByAcronym(String acronym) {
        try (Connection conn = openLocalConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT token FROM DataSetConfiguration WHERE dataset_acronym = ?")) {
            stmt.setString(1, acronym);
            try (ResultSet result = stmt.executeQuery()) {
                return result.next() ?
                        result.getString("token") :
                        "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
