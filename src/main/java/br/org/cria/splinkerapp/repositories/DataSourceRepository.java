package br.org.cria.splinkerapp.repositories;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.List;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.models.DataSourceType;

public class DataSourceRepository extends BaseRepository {

    public static boolean checkIfRecordsHaveDecreased()
    {
        /*
            var dsCount = *Executa o select salvo*;
            var fileLineCount = Files.lines(path).count() - 1; //header
            return fileLineCount >
         * 
        */
        return false;
    }

    public static DataSource getDataSource() throws Exception
    {
        var cmd = "SELECT * FROM DataSourceConfiguration;";
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        DataSource ds = null;

        while(result.next())
        {
                var host = result.getString("db_host");
                var port = result.getString("db_port");
                var dbName = result.getString("db_name");
                var table = result.getString("db_tablename");
                var user = result.getString("db_username");
                var pwd = result.getString("db_password");
                var filePath = result.getString("datasource_filepath");
                var type = DataSourceType.valueOf(result.getString("datasource_type"));
                ds = DataSource.factory(type, filePath, host,dbName, table,user, pwd, port);
            
        }
        return ds;
    }

    public static void saveSQLCommand(List<Double> cmd) throws Exception
    {
        var fileName = "%s/sql_command.sql".formatted(System.getProperty("user.dir"));
        var sqlCmd = byteArrayToString(cmd);
        Path path = Paths.get(fileName);
        byte[] strToBytes = sqlCmd.getBytes();
        if(Files.exists(path))
        {
            Files.delete(path);
        }
        Files.write(path, strToBytes);
    }

    public static String getSQLCommand() throws Exception 
    {
        var fileName = "%s/sql_command.sql".formatted(System.getProperty("user.dir"));
        String read = String.join("",Files.readAllLines(Path.of(fileName)));
        return read;
    }

    public static void saveDataSource(DataSourceType type, String filePath, String host, String port, 
                            String dbName, String table, String user, String password) throws Exception
    {
        cleanTable("DataSourceConfiguration");
        var cmd = """
                    INSERT INTO DataSourceConfiguration
                    (datasource_type, db_host, db_port, 
                    db_name, db_tablename, db_username, db_password, datasource_filepath)
                    VALUES(?,?,?,?,?,?,?,?);
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        stm.setString(1, type.name());
        stm.setString(2, host);
        stm.setString(3, port);
        stm.setString(4, dbName);
        stm.setString(5, table);
        stm.setString(6, user);
        stm.setString(7, password);
        stm.setString(8, filePath);
        stm.executeUpdate();
        stm.close();
        conn.close();

    }
    
    public static void saveDataSource(DataSourceType type, String filePath) throws Exception
    {
        saveDataSource(type,filePath, null,null,null,null,null,null);
    }
    public static void saveDataSource(DataSourceType type, String filePath, String username, String password) throws Exception
    {
        saveDataSource(type, filePath, null,null,null,null, username, password);
    }
}
