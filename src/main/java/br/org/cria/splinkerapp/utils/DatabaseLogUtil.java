package br.org.cria.splinkerapp.utils;

import br.org.cria.splinkerapp.models.DataSet;

import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseLogUtil {
    private DatabaseLogUtil() {
    }

    public static String showJdbcUrlWithCredentials(String url) {
        if (url == null) {
            return null;
        }

        return url;
    }

    public static String describeDataSet(DataSet ds) {
        if (ds == null) {
            return "dataset=null";
        }

        return "token=%s, id=%s, type=%s, host=%s, port=%s, dbName=%s, user=%s, hasPassword=%s, connectionString=%s"
                .formatted(
                        ds.getToken(),
                        ds.getId(),
                        ds.getType(),
                        ds.getDbHost(),
                        ds.getDbPort(),
                        ds.getDbName(),
                        ds.getDbUser(),
                        ds.getDbPassword() != null && !ds.getDbPassword().isBlank(),
                        showJdbcUrlWithCredentials(ds.getConnectionString())
                );
    }

    public static String describeConnectionProperties(Properties props) {
        if (props == null) {
            return "props=null";
        }

        Properties safeProps = new Properties();
        props.forEach((key, value) -> {
            String keyText = String.valueOf(key);
            String valueText = String.valueOf(value);
            safeProps.setProperty(keyText, valueText);
        });
        return safeProps.toString();
    }

    public static String describeSqlException(SQLException e) {
        if (e == null) {
            return "SQLException=null";
        }

        return "message=%s, sqlState=%s, errorCode=%s, class=%s"
                .formatted(e.getMessage(), e.getSQLState(), e.getErrorCode(), e.getClass().getName());
    }
}
