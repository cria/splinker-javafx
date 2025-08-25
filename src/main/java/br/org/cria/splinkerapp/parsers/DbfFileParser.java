package br.org.cria.splinkerapp.parsers;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.utils.StringStandards;
import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import io.sentry.Sentry;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DbfFileParser extends FileParser {
    private final List<String> columnNameList = new ArrayList<>();

    private final DBFReader reader;
    private final InputStream dbfStream;

    private final String fileName;

    private static final Charset DBF_CHARSET = Charset.forName("windows-1252");

    public DbfFileParser(String fileSourcePath) throws Exception{
        try {
            File dbfFile = new File(fileSourcePath);
            this.fileName = stripExt(dbfFile.getName());
            File memoDbtFile = new File(stripExt(dbfFile.getAbsolutePath()) + ".dbt");
            File memoFptFile = new File(stripExt(dbfFile.getAbsolutePath()) + ".fpt");

            this.dbfStream = new FileInputStream(dbfFile);
            this.reader = new DBFReader(this.dbfStream);
            this.reader.setCharactersetName(DBF_CHARSET.name());

            if (memoFptFile.exists()) {
                boolean ok = tryAttachMemoFile(this.reader, memoFptFile);
                ApplicationLog.info("Memo .fpt " + (ok ? "anexado" : "NÃO anexado"));
            } else if (memoDbtFile.exists()) {
                boolean ok = tryAttachMemoFile(this.reader, memoDbtFile);
                ApplicationLog.info("Memo .dbt " + (ok ? "anexado" : "NÃO anexado"));
            }
        } catch (DBFException e) {
            Sentry.captureException(e);
            throw e;
        } catch (IOException e) {
            Sentry.captureException(e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void insertDataIntoTable(Set<String> tabelas) throws SQLException {
        final String tableName = getTableName();
        if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return;

        if (reader == null) throw new IllegalStateException("DBFReader não inicializado.");

        final int numberOfColumns = reader.getFieldCount();
        final String valuesStr = String.join(",", Collections.nCopies(numberOfColumns, "?"));
        final String columnNames = String.join(",", columnNameList);
        final String command = insertIntoCommand.formatted(tableName, columnNames, valuesStr);

        final Connection conn = getConnection();
        try (PreparedStatement statement = conn.prepareStatement(command)) {
            conn.setAutoCommit(false);
            totalRowCount = reader.getRecordCount();
            currentRow = 0;

            Object[] rowObjects;
            while ((rowObjects = reader.nextRecord()) != null) {
                // JavaDBF retorna null para registros deletados -> pule
                if (rowObjects.length == 1 && rowObjects[0] == null) {
                    continue;
                }

                for (int i = 0; i < numberOfColumns; i++) {
                    DBFField field = reader.getField(i);
                    Object raw = (i < rowObjects.length) ? rowObjects[i] : null;
                    bindParamJavaDbf(statement, i + 1, field, raw);
                }

                statement.addBatch();
                currentRow++;

                if (currentRow % 10_000 == 0) {
                    statement.executeBatch();
                    conn.commit();
                    statement.clearBatch();
                    ApplicationLog.info("DBF/JavaDBF: %s - %d/%d registros".formatted(fileName, currentRow, totalRowCount));
                }
                readRowEventBus.post(currentRow);
            }

            statement.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            Sentry.captureException(e);
            throw e;
        } finally {
            closeQuietly(reader);
            closeQuietly(dbfStream);
            conn.close();
        }
    }

    @Override
    public String buildCreateTableCommand(Set<String> tabelas) throws Exception {
        final String tableName = getTableName();
        if (tabelas != null && !tabelas.contains(tableName.toLowerCase())) return null;

        if (reader == null) throw new IllegalStateException("DBFReader não inicializado.");

        dropTable(tableName);
        columnNameList.clear();

        int numberOfFields = reader.getFieldCount();
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));

        for (int i = 0; i < numberOfFields; i++) {
            DBFField f = reader.getField(i);
            String norm = StringStandards.normalizeString(f.getName());
            String col = "`%s`".formatted(norm);
            columnNameList.add(col);
            sb.append(col).append(" ").append(sqlDdlTypeForDbfField(f)).append(",");
        }

        return sb.toString().replaceAll(",$", "") + ");";
    }

    @Override
    protected List<String> getRowAsStringList(Object row, int numberOfColumns) {
        if (row == null) return Collections.emptyList();
        Object[] fullRow = (Object[]) row;
        String[] arr = new String[numberOfColumns];
        for (int col = 0; col < numberOfColumns; col++) {
            Object v = (col < fullRow.length) ? fullRow[col] : null;
            arr[col] = (v == null) ? "" : v.toString();
        }
        return Arrays.asList(arr);
    }

    @Override
    protected String getTableName() {
        return this.fileName;
    }

    private static String stripExt(String name) {
        int p = name.lastIndexOf('.');
        return (p >= 0) ? name.substring(0, p) : name;
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try { c.close(); } catch (Exception ignore) {}
    }

    private String buildColumnNamesFromReader() {
        int cols = reader.getFieldCount();
        List<String> list = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            DBFField f = reader.getField(i);
            String norm = StringStandards.normalizeString(f.getName());
            list.add("`" + norm + "`");
        }
        columnNameList.clear();
        columnNameList.addAll(list);
        return list.stream().collect(Collectors.joining(","));
    }

    private boolean tryAttachMemoFile(DBFReader r, File memoFile) {
        try {
            Method m = r.getClass().getMethod("setMemoFile", File.class);
            m.invoke(r, memoFile);
            return true;
        } catch (NoSuchMethodException nsme) {
            try {
                Method m2 = r.getClass().getMethod("setMemoFile", InputStream.class);
                try (var in = new FileInputStream(memoFile)) {
                    m2.invoke(r, in);
                }
                return true;
            } catch (Exception ignore) {
                return false;
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            return false;
        }
    }


    private static String typeName(DBFField f) {
        try {
            String lowerCase = f.getType().name().toLowerCase();
            return lowerCase;
        } catch (Exception ignore) {}
        try {
            char c = f.getType().getCharCode();
            return mapCode(c);
        } catch (Throwable t) {
            return "character";
        }
    }

    private static String mapCode(char c) {
        switch (Character.toUpperCase(c)) {
            case 'M': return "memo";
            case 'C': return "character";
            case 'N': return "numeric";
            case 'F': return "float";
            case 'Y': return "currency";
            case 'I': return "integer";
            case 'B':
            case 'O': return "double";
            case 'L': return "logical";
            case 'D': return "date";
            case 'T': return "datetime";
            case 'Q': return "varbinary";
            case 'V': return "varchar";   // VFP
            case 'W': return "blob";
            case 'G': return "general";
            case 'P': return "picture";
            default : return "character";
        }
    }

    private static boolean isMemo(DBFField f)      { return typeName(f).startsWith("memo"); }
    private static boolean isChar(DBFField f)      { String t = typeName(f); return t.startsWith("char") || t.startsWith("varchar"); }
    private static boolean isNum(DBFField f)       { String t = typeName(f); return t.startsWith("num") || t.startsWith("float") || t.startsWith("curr"); }
    private static boolean isInt(DBFField f)       { return typeName(f).startsWith("int"); }
    private static boolean isDbl(DBFField f)       { String t = typeName(f); return t.startsWith("double") || t.startsWith("float"); }
    private static boolean isBool(DBFField f)      { return typeName(f).startsWith("log"); }
    private static boolean isDate(DBFField f)      { return typeName(f).equals("date"); }
    private static boolean isDateTime(DBFField f)  { String t = typeName(f); return t.equals("datetime") || t.equals("timestamp"); }

    private static int safeLen(DBFField f, int def) {
        try { int l = f.getLength(); return (l > 0 ? l : def); }
        catch (Throwable t) { return def; }
    }
    private static int safeDec(DBFField f) {
        try { return Math.max(0, f.getDecimalCount()); }
        catch (Throwable t) { return 0; }
    }

    private static String sqlDdlTypeForDbfField(DBFField f) {
        if (isMemo(f)) return "LONGTEXT";
        if (isChar(f)) {
            int v = Math.max(1, Math.min(safeLen(f, 255), 65532));
            return "VARCHAR(" + v + ")";
        }
        if (isInt(f))  return "BIGINT";
        if (isDbl(f))  return "DOUBLE";
        if (isBool(f)) return "BOOLEAN";
        if (isDate(f)) return "DATE";
        if (isDateTime(f)) return "TIMESTAMP";
        if (isNum(f)) {
            int len = safeLen(f, 18);
            int dec = safeDec(f);
            if (dec > 0) {
                int p = Math.max(dec + 1, len);
                return "DECIMAL(" + p + "," + dec + ")";
            } else {
                return (len <= 18) ? "BIGINT" : "DECIMAL(" + Math.max(19, len) + ",0)";
            }
        }
        return "TEXT";
    }

    private void bindParamJavaDbf(PreparedStatement st, int idx, DBFField f, Object raw) throws SQLException {
        if (raw == null) {
            st.setString(idx, getCellValue(null));
            return;
        }

        if (isMemo(f)) {
            String memo;
            if (raw instanceof byte[]) {
                memo = new String((byte[]) raw, DBF_CHARSET);
            } else {
                memo = raw.toString();
            }
            st.setString(idx, getCellValue(memo));
            return;
        }

        if (isChar(f)) {
            st.setString(idx, getCellValue(raw.toString()));
            return;
        }

        if (isInt(f)) {
            if (raw instanceof Number) {
                st.setLong(idx, ((Number) raw).longValue());
            } else {
                String s = raw.toString().trim();
                if (s.isEmpty()) st.setNull(idx, Types.BIGINT);
                else st.setLong(idx, Long.parseLong(s));
            }
            return;
        }

        if (isDbl(f)) {
            if (raw instanceof Number) {
                st.setDouble(idx, ((Number) raw).doubleValue());
            } else {
                String s = raw.toString().trim().replace(',', '.');
                if (s.isEmpty()) st.setNull(idx, Types.DOUBLE);
                else st.setDouble(idx, Double.parseDouble(s));
            }
            return;
        }

        if (isBool(f)) {
            if (raw instanceof Boolean) {
                st.setBoolean(idx, (Boolean) raw);
            } else {
                String s = raw.toString().trim();
                if (s.isEmpty()) st.setNull(idx, Types.BOOLEAN);
                else {
                    boolean b = "Y".equalsIgnoreCase(s) || "T".equalsIgnoreCase(s) || "1".equals(s) || "true".equalsIgnoreCase(s);
                    st.setBoolean(idx, b);
                }
            }
            return;
        }

        if (isDate(f)) {
            if (raw instanceof java.util.Date) {
                st.setDate(idx, new java.sql.Date(((java.util.Date) raw).getTime()));
            } else {
                String s = raw.toString().trim();
                java.util.Date d = tryParse(s, "yyyyMMdd", "yyyy-MM-dd", "dd/MM/yyyy");
                if (d != null) st.setDate(idx, new java.sql.Date(d.getTime()));
                else st.setString(idx, s);
            }
            return;
        }

        if (isDateTime(f)) {
            if (raw instanceof java.util.Date) {
                st.setTimestamp(idx, new java.sql.Timestamp(((java.util.Date) raw).getTime()));
            } else {
                String s = raw.toString().trim();
                java.util.Date d = tryParse(s, "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss");
                if (d != null) st.setTimestamp(idx, new java.sql.Timestamp(d.getTime()));
                else st.setString(idx, s);
            }
            return;
        }

        if (isNum(f)) {
            String s = raw.toString().trim();
            if (s.isEmpty()) {
                st.setNull(idx, Types.DECIMAL);
            } else {
                try {
                    st.setBigDecimal(idx, new BigDecimal(s.replace(',', '.')));
                } catch (NumberFormatException nfe) {
                    st.setDouble(idx, Double.parseDouble(s.replace(',', '.')));
                }
            }
            return;
        }

        st.setString(idx, raw.toString());
    }

    private static java.util.Date tryParse(String s, String... fmts) {
        for (String f : fmts) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f);
                sdf.setLenient(false);
                return sdf.parse(s);
            } catch (Exception ignore) {}
        }
        return null;
    }
}
