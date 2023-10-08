package br.org.cria.splinkerapp.repositories;

import java.nio.file.Path;
import java.sql.DriverManager;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.models.DataSourceType;

public class DataSourceRepository extends BaseRepository{

    public static DataSource getDataSource() throws Exception
    {
        var cmd = """
                SELECT datasource_filepath, datasource_type, db_host,
                db_port, db_name, db_tablename,
                db_username, db_password
                FROM DataSourceConfiguration
                LIMIT 1;
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var result = runQuery(cmd, conn);
        DataSource ds = null;

        while(result.next())
        {
            var filePath = result.getString(1);
            var type = DataSourceType.valueOf(result.getString(2));
            if(filePath == null)
            {
                var host = result.getString(3);
                var port = result.getString(4);
                var dbName = result.getString(5);
                var table = result.getString(6);
                var user = result.getString(7);
                var pwd = result.getString(8);
                ds = new DataSource(type, host, dbName, table, user, pwd, port);
            }
            else
            {
                ds = new DataSource(type, Path.of(filePath));   
            }
            
        }
        return ds;
    }

    public void saveDataSource(DataSourceType type, String host, String port, 
                            String dbName, String table, String user, String password) throws Exception
    {
        cleanTable("DataSourceConfiguration");
        var cmd = """
                    INSERT INTO DataSourceConfiguration
                    (datasource_type, db_host, db_port, 
                    db_name, db_tablename, db_username, db_password)
                    VALUES(?,?,?,?,?,?,?);
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
        stm.executeUpdate();
        stm.close();
        conn.close();

    }
    
    public void saveDataSource(DataSourceType type, Path filePath) throws Exception
    {
        cleanTable("DataSourceConfiguration");
        var cmd = """
                    INSERT INTO DataSourceConfiguration
                    (datasource_filepath, datasource_type)
                    VALUES(?,?);
                """;
        var conn = DriverManager.getConnection(LOCAL_DB_CONNECTION);
        var stm = conn.prepareStatement(cmd);
        var path = filePath.toAbsolutePath().toString();
        var sourceType = type.name();
        stm.setString(1, path);
        stm.setString(2, sourceType);
        stm.executeUpdate();
        stm.close();
        conn.close();
    }
}
