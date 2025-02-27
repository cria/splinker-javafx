package br.org.cria.splinkerapp.managers;

public class LocalDbManager {
    private static final String dbFilePath = "%s/spLinker.db".formatted(System.getProperty("user.home"));
    private static final String connString = "jdbc:sqlite:%s".formatted(dbFilePath);

    public static String getDbFilePath() {
        return dbFilePath;
    }

    public static String getLocalDbConnectionString() {
        return connString;
    }

}
