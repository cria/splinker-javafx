package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.microsoft.sqlserver.jdbc.StringUtils;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;

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
        conn.setAutoCommit(false);
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
                var statement = conn.prepareStatement(command);    
                var rows = table.iterator();
                
                currentRow = 0;
                totalRowCount = table.getRowCount();
                while(rows.hasNext())
                {
                    var row = rows.next();
                    
                    if (row != null) 
                    {
                        var cells = row.values().iterator();
                        var cellIndex = 1;
                        while (cells.hasNext()) 
                        {
                            var cell = cells.next();
                            var isNullCell = cell == null;
                            var value = getCellValue(isNullCell? null: cell.toString());
                            statement.setString(cellIndex, value);
                            cellIndex++;
                        }
                        statement.addBatch();
                        if ((currentRow % 10_000 == 0)) 
                        {
                            statement.executeBatch();
                            conn.commit();
                            statement.clearBatch();
                        }
                        currentRow++;
                        readRowEventBus.post(currentRow);
                    }
                }
                statement.executeBatch();
                conn.commit();
                statement.clearBatch();
                statement.close();
                totalColumnCount = currentRow;
            }catch (FileNotFoundException fnfe) {
                continue;
            }
             catch (Exception e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }
        db.close();
        conn.setAutoCommit(true);
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
                continue;
            }
             catch (Exception e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }
        builder.append(");");
        var command = builder.toString().replace(",);", ");");
        return command;
    }
}
