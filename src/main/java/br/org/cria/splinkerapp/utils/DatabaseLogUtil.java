package br.org.cria.splinkerapp.utils;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.models.DataSet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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

        StringBuilder description = new StringBuilder("message=%s, sqlState=%s, errorCode=%s, class=%s"
                .formatted(e.getMessage(), e.getSQLState(), e.getErrorCode(), e.getClass().getName()));

        if (e.getCause() != null) {
            description.append(", causes=").append(describeThrowableChain(e.getCause()));
        }

        SQLException next = e.getNextException();
        if (next != null) {
            description.append(", nextException=").append(describeSqlException(next));
        }

        Throwable[] suppressed = e.getSuppressed();
        if (suppressed.length > 0) {
            description.append(", suppressed=");
            for (int i = 0; i < suppressed.length; i++) {
                if (i > 0) {
                    description.append(" | ");
                }
                description.append(describeThrowable(suppressed[i]));
            }
        }

        return description.toString();
    }

    public static void logTcpProbe(String label, String host, String port, int timeoutMs) {
        long start = System.currentTimeMillis();
        try {
            int parsedPort = Integer.parseInt(port);
            InetAddress[] addresses = InetAddress.getAllByName(host);
            ApplicationLog.info("[POSTGRES] TCP probe %s: host=%s, port=%s, timeoutMs=%s, resolvedAddresses=%s"
                    .formatted(label, host, port, timeoutMs, describeAddresses(addresses)));

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, parsedPort), timeoutMs);
                long elapsed = System.currentTimeMillis() - start;
                ApplicationLog.info("[POSTGRES] TCP probe %s OK: remote=%s:%s, local=%s, elapsedMs=%s"
                        .formatted(label, host, port, socket.getLocalSocketAddress(), elapsed));
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            ApplicationLog.info("[POSTGRES] TCP probe %s FALHOU: host=%s, port=%s, timeoutMs=%s, elapsedMs=%s, erro=%s"
                    .formatted(label, host, port, timeoutMs, elapsed, describeThrowableChain(e)));
        }
    }

    public static String describeThrowableChain(Throwable throwable) {
        if (throwable == null) {
            return "throwable=null";
        }

        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 8) {
            if (depth > 0) {
                builder.append(" -> ");
            }
            builder.append(describeThrowable(current));
            current = current.getCause();
            depth++;
        }
        return builder.toString();
    }

    private static String describeThrowable(Throwable throwable) {
        return "%s: %s".formatted(throwable.getClass().getName(), throwable.getMessage());
    }

    private static String describeAddresses(InetAddress[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < addresses.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(addresses[i].getHostAddress());
        }
        builder.append("]");
        return builder.toString();
    }
}
