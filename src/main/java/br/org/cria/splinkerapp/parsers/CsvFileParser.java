package br.org.cria.splinkerapp.parsers;

import java.io.BufferedReader;
import java.io.File;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CsvFileParser extends FileParser {
    CsvParser parser;
    List<String> columns;

    Iterator<String[]> iterator;
    String filePath;

    public CsvFileParser(String filePath) throws Exception {

        this.filePath = filePath;
        var settings = new CsvParserSettings();
        settings.setDelimiterDetectionEnabled(true);
        var parser = new CsvParser(settings);
        iterator = parser.iterate(new File(filePath)).iterator();
        columns = normalizeAllColumns(Arrays.asList(iterator.next()));
    }

    List<String> normalizeAllColumns(List<String> columnList) {
        return columnList.stream().filter(e -> e != null)
                .map((e) -> makeColumnName(e)).toList();
    }

    @Override
    public List<String> getRowAsStringList(Object row, int numberOfColumns) {
        return row == null ? null : Arrays.asList((String[]) row);
    }

    @Override
    protected String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        dropTable("spLinker");
        var tableName = getTableName();

        if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return null;

        var columnNames = String.join(",", columns.stream().map((e) -> "%s VARCHAR(1)".formatted(e)).toList());
        var command = createTableCommand.formatted(tableName, columnNames).replace(",)", " );");
        return command;
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws Exception {
        String separator = detectSeparator(filePath);
        var conn = getConnection();
        conn.setAutoCommit(false);
        var tableName = getTableName();

        if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return;

        var valuesStr = makeValueString(columns.size());
        var columnNames = String.join(",", columns);
        var columnCount = columns.size();
        var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr)
                .replace(",)", ")");
        var statement = conn.prepareStatement(command);

        while (iterator.hasNext()) {
            var row = iterator.next();
            String[] rowValues = row[0].split(separator, -1);
            for (int j = 0; j < columnCount; j++) {
                var value = (j < rowValues.length) ? getCellValue(rowValues[j]) : null;
                statement.setString(j + 1, value);
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
        totalRowCount = currentRow;
        statement.executeBatch();
        conn.commit();
        statement.clearBatch();
        conn.setAutoCommit(true);
        conn.close();
    }
    public static String detectSeparator(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                if (firstLine.contains("\t")) {
                    return "\t";
                } else if (firstLine.contains(",")) {
                    return ",";
                } else if (firstLine.contains(";")) {
                    return ";";
                }
            }
        }
        throw new IOException("Não foi possível detectar o separador.");
    }
}