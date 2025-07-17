package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import br.org.cria.splinkerapp.utils.StringStandards;

public class OdsFileParser extends FileParser {
    private String filePath;
    private SpreadSheet spreadSheet;

    public OdsFileParser(String filePath) throws Exception {
        this.filePath = filePath;
        var file = new File(this.filePath);
        //this.spreadSheet = new SpreadSheet(file);
        this.spreadSheet = new SpreadSheet(new FileInputStream(file));
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        int numberOfTabs = spreadSheet.getSheets().size();
        var conn = getConnection();
        var commandBase = "INSERT INTO %s (%s) VALUES (%s);";

        conn.setAutoCommit(false);

        for (int i = 0; i < numberOfTabs; i++) {
            var sheet = spreadSheet.getSheet(i);
            var tableName = StringStandards.normalizeString(sheet.getName());
            if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) continue;
            var columns = new ArrayList<String>();
            var numberOfColumns = sheet.getMaxColumns();
            var valuesStr = makeValueString(numberOfColumns);

            IntStream.range(0, numberOfColumns).forEach(n ->
            {
                var field = sheet.getRange(0, n);
                var value = field.getValue();
                if (value != null) {
                    columns.add(makeColumnName(value.toString()));
                }

            });

            var columnNames = String.join(",", columns);
            var command = commandBase.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
            var statement = conn.prepareStatement(command);
            totalRowCount = sheet.getMaxRows();


            numberOfColumns = columns.size(); // número real de colunas

            for (int j = 1; j < totalRowCount; j++) {
                currentRow = j;
                var sheetRow = new ArrayList<Range>();
                IntStream.range(0, numberOfColumns).forEach(n -> sheetRow.add(sheet.getRange(currentRow, n)));
                var row = getRowAsStringList(sheetRow, numberOfColumns);
                var valuesList = row.stream().toList();

                for (int k = 0; k < valuesList.size(); k++) {
                    var currentItem = valuesList.get(k);
                    var value = getCellValue(currentItem);
                    statement.setString(k + 1, value);
                }
                statement.addBatch();

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
        }
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {

        final var fullRow = (List<Range>) row;
        var arr = new String[numberOfColumns];

        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            var column = fullRow.get(colNum);
            var value = column.getValue();
            arr[colNum] = value == null ? "" : value.toString();
        }

        return Arrays.asList(arr);

    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        var builder = new StringBuilder();
        for (Sheet sheet : spreadSheet.getSheets()) {
            var numberOfColumns = sheet.getMaxColumns();
            var tableName = StringStandards.normalizeString(sheet.getName());
            dropTable(tableName);

            if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) continue;

            builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));

            for (int i = 0; i < numberOfColumns; i++) {
                var column = sheet.getRange(0, i);
                var value = column.getValue();
                if (value != null) {
                    String columnName = makeColumnName(value.toString());
                    builder.append("%s VARCHAR(1),".formatted(columnName));
                }

            }
            builder.append(");");
        }
        var command = builder.toString().replace(",);", ");");
        return command;
    }
}
