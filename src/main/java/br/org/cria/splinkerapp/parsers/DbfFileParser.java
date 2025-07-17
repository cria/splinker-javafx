package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.utils.StringStandards;
import io.sentry.Sentry;

public class DbfFileParser extends FileParser {
    String fileSourcePath;
    List<String> columnNameList = new ArrayList<String>();
    DBFReader reader = null;
    String fileName;

    public DbfFileParser(String fileSourcePath) throws Exception {
        this.fileSourcePath = fileSourcePath;
        try {
            var file = new File(fileSourcePath);
            var name = file.getName();
            var stream = new FileInputStream(file);

            this.reader = new DBFReader(stream);
            this.fileName = name.substring(0, name.lastIndexOf("."));
        } catch (DBFException e) {
            Sentry.captureException(e);
            e.printStackTrace();
        } catch (IOException e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws SQLException {
        Object[] rowObjects;
        var conn = getConnection();
        int numberOfColumns = reader.getFieldCount();
        var tableName = getTableName();

        if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return;

        var valuesStr = "?,".repeat(numberOfColumns);
        var columnNames = String.join(",", columnNameList);
        var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
        var statement = conn.prepareStatement(command);
        totalRowCount = reader.getRecordCount();
        conn.setAutoCommit(false);

        while ((rowObjects = reader.nextRecord()) != null) {

            var valuesList = getRowAsStringList(rowObjects, numberOfColumns).stream().toList();

            for (int k = 0; k < valuesList.size(); k++) {
                var currentItem = valuesList.get(k);
                var value = getCellValue(currentItem);
                statement.setString(k + 1, value);
            }
            statement.addBatch();
            currentRow++;

            if (currentRow % 10_000 == 0) {
                statement.executeBatch();
                conn.commit();
                statement.clearBatch();
            }
            readRowEventBus.post(currentRow);
        }
        statement.executeBatch();
        conn.commit();
        statement.clearBatch();
        conn.setAutoCommit(true);
        conn.close();
        reader.close();
    }

    @Override
    public String buildCreateTableCommand(Set<String> tabelas) throws Exception {

        int numberOfFields = reader.getFieldCount();

        var builder = new StringBuilder();
        var tableName = getTableName();
        builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
        dropTable(tableName);

        if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return null;

        for (int i = 0; i < numberOfFields; i++) {
            var field = reader.getField(i);
            var fieldName = field.getName();
            String columnName = "`%s`".formatted(StringStandards.normalizeString(fieldName));
            columnNameList.add(columnName);
            builder.append("%s VARCHAR(1),".formatted(columnName));

        }

        builder.append(");");
        var command = builder.toString().replace(",);", ");");
        return command;
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        var fullRow = (Object[]) row;
        var arr = new String[numberOfColumns];
        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            var value = fullRow[colNum];
            arr[colNum] = value == null ? "" : value.toString();
        }

        return Arrays.asList(arr);

    }

    @Override
    protected String getTableName() {
        return this.fileName;
    }

}
