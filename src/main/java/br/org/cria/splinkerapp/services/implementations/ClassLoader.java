package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.DataSourceType;

public class ClassLoader {

    static Class load(DataSourceType type) throws Exception
    {
        String name = null;
        switch(type)
        {
            case MySQL:
                name = "com.mysql.cj.jdbc.Driver";
            break;
            case PostgreSQL:
                 name = "org.postgresql.Driver";
            break;
            case Oracle:
                name = "oracle.jdbc.OracleDriver";
            break;
            case SQLServer:
            name = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            break;
            case Access:
            name = "net.ucanaccess.jdbc.UcanaccessDriver"; 
            break;
            default:
                name = "org.sqlite.JDBC"; 
                break;

        }
        return Class.forName(name);
        
    }
    
}
