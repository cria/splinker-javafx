package br.org.cria.splinkerapp.parsers;

import br.org.cria.splinkerapp.utils.SQLKeywordChecker;
import br.org.cria.splinkerapp.utils.StringStandards;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.query.Query;
import com.microsoft.sqlserver.jdbc.StringUtils;
import io.sentry.Sentry;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AccessFileParser extends FileParser {
    Database db;
    Set<String> tableNames;
    String filePath;
    String password;
    Statement stmt;

    public AccessFileParser(String filePath, String password) throws Exception {
        super();
        var conn = getConnection();
        this.filePath = filePath;
        this.password = password;
        var hasPassword = !StringUtils.isEmpty(password);
        if (hasPassword) {
            System.setProperty("jackcess.password", password);
        }
        db = DatabaseBuilder.open(new File(filePath));
        tableNames = db.getTableNames();
        List<Query> queries = db.getQueries();
        for (Query query : queries) {
            if (!query.getName().contains("sq")) {
                tableNames.add(query.getName());
            }
        }
        conn.close();

        String url = "jdbc:ucanaccess://" + filePath;
        hasPassword = !StringUtils.isEmpty(this.password);
        if (hasPassword) {
            url = url + ";password=" + this.password;
        }
        Connection connection = DriverManager.getConnection(url);
        stmt = connection.createStatement();
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
                if (SQLKeywordChecker.isReservedSQLKeyword(finalTableName)) continue;
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
                    if (numberOfColumns == 0) {
                        continue;
                    }
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
                if (table == null) {
                    List<Query> queries = db.getQueries();
                    for (Query query : queries) {
                        if (query.getName().equals(name)) {
                            inserirDadosViaQuery(query.toSQLString(), statement, conn);
                            break;
                        }
                    }
                } else {
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
                }
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

    private void inserirDadosViaQuery(String query, PreparedStatement statement, Connection conn) {
        try {
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            currentRow = 0;
            totalRowCount = columnCount;

            while (rs.next()) {
                var cellIndex = 1;
                for (int i = 1; i <= columnCount; i++) {
                    //String columnName = metaData.getColumnName(i);
                    String columnValue = rs.getString(i);
                    var isNullCell = columnValue == null;
                    var value = getCellValue(isNullCell ? null : columnValue);
                    statement.setString(cellIndex, value);
                    cellIndex++;
                    //System.out.print(columnName + ": " + columnValue + " | ");*/
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

            statement.executeBatch();
            conn.commit();
            statement.clearBatch();
            statement.close();
            totalColumnCount = currentRow;
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                Table table = db.getTable(name);
                var finalTableName = StringStandards.normalizeString(name);
                if (SQLKeywordChecker.isReservedSQLKeyword(finalTableName)) continue;
                if (table == null) {
                    List<Query> queries = db.getQueries();
                    for (Query query : queries) {
                        if (query.getName().equals(name)) {
                            List<String> colunasDasViews = gerarColunasDasViews(query.toSQLString());
                            if (colunasDasViews != null) columns.addAll(colunasDasViews);
                            break;
                        }
                    }
                } else {
                    var columnTable = table.getColumns();
                    for (var c : columnTable) {
                        columns.add(c.getName());
                    }
                }
                dropTable(finalTableName);
                if (columns.isEmpty()) continue;
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


    public List<String> gerarColunasDasViews(String query) {
        try {
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                List<String> colunas = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    colunas.add(columnName);
                }
                return colunas;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
