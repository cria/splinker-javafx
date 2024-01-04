package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;

import com.microsoft.sqlserver.jdbc.StringUtils;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.utils.StringStandards;

public class AccessFileParser extends FileParser {
    Database db;
    Set<String> tableNames;
    String filePath;
    
    public AccessFileParser(String filePath, String password) throws Exception
    {
        super();
        this.filePath = filePath;
        var hasPassword = !StringUtils.isEmpty(password);
        if(hasPassword)
        {
            System.setProperty("jackcess.password", password);
        }
        db = DatabaseBuilder.open(new File(filePath));
        tableNames = db.getTableNames();
    }

    @Override
    public void insertDataIntoTable() throws Exception 
    {
        var conn = getConnection();
        for(var name: tableNames)
        {
            try 
            {
                var table = db.getTable(name);
                var finalTableName = StringStandards.normalizeString(name);
                var headerRow = table.getColumns().stream().filter(e -> !StringUtils.isEmpty(e.getName())).toList();
                int numberOfColumns = headerRow.size();
                var columns = getRowAsStringList(headerRow.stream().map(e -> e.getName()).toList(), numberOfColumns)
                                            .stream().map((col) -> makeColumnName(col)).toList();
                var valuesStr = "?,".repeat(numberOfColumns);
                var columnNames = String.join(",", columns);
                var command = insertIntoCommand.formatted(finalTableName, columnNames, valuesStr).replace(",)", ")");
                var rows = table.stream().toList();
                for(var row: rows)
                {
                    if (row != null) 
                    {
                        var values = row.values().stream().map(e -> e == null? "": e.toString()).toList();
                        var valuesList = getRowAsStringList(values, numberOfColumns);
                        PreparedStatement statement;
                        try 
                        {
                            statement = conn.prepareStatement(command);    
                            for (int k = 0; k < valuesList.size(); k++) 
                            {
                                statement.setString(k + 1, valuesList.get(k));
                            }
                            statement.executeUpdate();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // table.stream().forEach(row ->
                // {
           
                // });      
            }catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                throw fnfe;
            }
             catch (Exception e) {
                e.printStackTrace();
                ApplicationLog.error(e.getLocalizedMessage());
            }
        }
        conn.close();
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        var values = (List<String>)row;  
        return values;
    }

    @Override
    protected String buildCreateTableCommand() throws Exception {
        var builder = new StringBuilder();
        for(var name: tableNames)
        {
            try 
            {
                var table = db.getTable(name);
                var finalTableName = StringStandards.normalizeString(name);
                var columns = table.getColumns();
                dropTable(finalTableName);
                builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(finalTableName));
                for(var column: columns)
                {
                    if(!StringUtils.isEmpty(column.getName()))
                    {
                        var columnName = makeColumnName(column.getName());
                        builder.append("%s VARCHAR(1),".formatted(columnName));
                    }
                }    
            }catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }
             catch (Exception e) {
                e.printStackTrace();
                ApplicationLog.error(e.getLocalizedMessage());
            }
        }
        builder.append(");");
        var command = builder.toString().replace(",);", ");");
        return command;
    }
}
