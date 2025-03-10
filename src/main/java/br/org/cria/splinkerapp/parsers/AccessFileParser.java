package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.microsoft.sqlserver.jdbc.StringUtils;
import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;

public class AccessFileParser extends FileParser {
    Database db;
    Set<String> tableNames;
    String filePath;

    public AccessFileParser(String filePath, String password) throws Exception {
        super();
        var conn = getConnection();
        this.filePath = filePath;
        var hasPassword = !StringUtils.isEmpty(password);
        if (hasPassword) {
            System.setProperty("jackcess.password", password);
        }
        db = DatabaseBuilder.open(new File(filePath));
        tableNames = db.getTableNames();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"VIEW"});
            while (rs.next()) {
                String nomeView = rs.getString("TABLE_NAME");
                tableNames.add(nomeView);
            }
        conn.close();
    }

    @Override
    public void insertDataIntoTable() throws Exception {
        var conn = getConnection();
        conn.setAutoCommit(false);
        List<String> columns;
        String valuesStr;
        for (var name : tableNames) {
            try {
                var finalTableName = StringStandards.normalizeString(name);
                var table = db.getTable(name);
                if (table == null) {
                    DatabaseMetaData metaData = conn.getMetaData();
                    ResultSet rs = metaData.getColumns(null, null, name, null);
                    List<String> tempColumns = new ArrayList<>();
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        tempColumns.add(makeColumnName(columnName));
                    }
                    columns = tempColumns;
                    int numberOfColumns = columns.size();
                    valuesStr = "?,".repeat(numberOfColumns);
                    } else {
                        var headerRow = table.getColumns().stream().filter(e -> !StringUtils.isEmpty(e.getName())).toList();
                        int numberOfColumns = headerRow.size();
                        columns = getRowAsStringList(headerRow.stream().map(e -> e.getName()).toList(), numberOfColumns).stream()
                            .map(col -> makeColumnName(col))
                            .toList();
                        valuesStr = "?,".repeat(numberOfColumns);
                }
                var columnNames = String.join(",", columns);
                var command = insertIntoCommand.formatted(finalTableName, columnNames, valuesStr).replace(",)", ")");
                var statement = conn.prepareStatement(command);
                var rows = table.iterator();
                currentRow = 0;
                totalRowCount = table.getRowCount();
                while (rows.hasNext()) {
                    var row = rows.next();
                    if (row != null) {
                        var cells = row.values().iterator();
                        var cellIndex = 1;
                        while (cells.hasNext()) {
                            var cell = cells.next();
                            var isNullCell = cell == null;
                            var value = getCellValue(isNullCell ? null : cell.toString());
                            statement.setString(cellIndex, value);
                            cellIndex++;
                        }
                        statement.addBatch();
                        if ((currentRow % 10_000 == 0)) {
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
            } catch (FileNotFoundException fnfe) {
                continue;
            } catch (Exception e) {
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
        var values = (List<String>) row;
        return values;
    }

    @Override
    protected String buildCreateTableCommand() throws Exception {
        var builder = new StringBuilder();
        for (var name : tableNames) {
        List<String> columns = new ArrayList<>();
            try {
                var table = db.getTable(name);
                var finalTableName = StringStandards.normalizeString(name);
                if(table == null) {
                    var conn = getConnection();
                    DatabaseMetaData metaData = conn.getMetaData();
                    ResultSet rsColumns  = metaData.getColumns(null, null, name, null);
                    while (rsColumns.next()) {
                        columns.add(rsColumns .getString("COLUMN_NAME"));
                    }
                    conn.close();
                } else {
                    var columnTable = table.getColumns();
                    for (var c : columnTable){
                        columns.add(c.getName());
                    }
                }
                dropTable(finalTableName);
                builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(finalTableName));
                for (var column : columns) {
                    if (!StringUtils.isEmpty(column)) {
                        var columnName = makeColumnName(column);
                        builder.append("%s VARCHAR(1),".formatted(columnName));
                    }
                }
                builder.append(");");
            } catch (FileNotFoundException fnfe) {
                continue;
            } catch (Exception e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }
        var command = builder.toString().replace(",);", ");");
        return command;
    }
}
