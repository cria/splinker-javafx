package br.org.cria.splinkerapp.services.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoogleDriveFileService {

    private static final String USER_AGENT = "spLinker/1.0";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final Pattern FILE_ID_IN_PATH = Pattern.compile("/file/d/([^/?]+)");
    private static final Pattern SPREADSHEET_ID_IN_PATH = Pattern.compile("/spreadsheets/d/([^/?]+)");
    private static final Pattern FILE_ID_IN_QUERY = Pattern.compile("[?&]id=([^&]+)");
    private static final Pattern CONFIRM_TOKEN = Pattern.compile("confirm=([0-9A-Za-z_-]+)");
    private static final Pattern CONFIRM_INPUT_TOKEN = Pattern.compile("name=\"confirm\"\\s+value=\"([^\"]+)\"");
    private static final Pattern UUID_TOKEN = Pattern.compile("name=\"uuid\"\\s+value=\"([^\"]+)\"");
    private static final byte[] XLS_MAGIC = new byte[] {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] ZIP_MAGIC = new byte[] { 'P', 'K' };

    private GoogleDriveFileService() {
    }

    public static void validateAccess(String pathOrUrl) throws Exception {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            throw new IllegalArgumentException("Informe um caminho ou URL.");
        }

        String trimmed = pathOrUrl.trim();
        if (!isRemotePath(trimmed)) {
            Path localPath = Path.of(trimmed);
            if (!Files.exists(localPath)) {
                throw new IllegalArgumentException("O arquivo informado não foi encontrado.");
            }
            if (!Files.isReadable(localPath)) {
                throw new IllegalArgumentException("Sem permissão para leitura do arquivo informado.");
            }
            return;
        }

        SpreadsheetRemoteFile remoteFile = downloadSpreadsheet(trimmed);
        Files.deleteIfExists(remoteFile.getLocalFile());
    }

    public static boolean isRemotePath(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }

    public static boolean isGoogleDriveUrl(String value) {
        if (!isRemotePath(value)) {
            return false;
        }

        String lowered = value.trim().toLowerCase(Locale.ROOT);
        return lowered.contains("drive.google.com") || lowered.contains("docs.google.com");
    }

    public static String extractFileId(String googleDriveUrl) {
        if (googleDriveUrl == null || googleDriveUrl.isBlank()) {
            return null;
        }

        Matcher spreadsheetByPath = SPREADSHEET_ID_IN_PATH.matcher(googleDriveUrl);
        if (spreadsheetByPath.find()) {
            return decode(spreadsheetByPath.group(1));
        }

        Matcher byPath = FILE_ID_IN_PATH.matcher(googleDriveUrl);
        if (byPath.find()) {
            return decode(byPath.group(1));
        }

        Matcher byQuery = FILE_ID_IN_QUERY.matcher(googleDriveUrl);
        if (byQuery.find()) {
            return decode(byQuery.group(1));
        }

        return null;
    }

    public static SpreadsheetRemoteFile downloadSpreadsheet(String googleDriveUrl) throws Exception {
        if (!isGoogleDriveUrl(googleDriveUrl)) {
            throw new IllegalArgumentException("O link remoto informado deve ser um link do Google Drive.");
        }

        String fileId = extractFileId(googleDriveUrl);
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("Não foi possível identificar o arquivo no link do Google Drive informado.");
        }

        if (isGoogleSheetsUrl(googleDriveUrl)) {
            HttpResponse exportResponse = executeRequest(buildGoogleSheetsExportUrl(fileId), null);
            if (!isBinaryResponse(exportResponse)) {
                throw new IllegalArgumentException(
                        "Não foi possível exportar a planilha do Google Sheets. Verifique se ela está compartilhada para acesso."
                );
            }
            return persistBinaryResponse(exportResponse);
        }

        String baseDownloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        HttpResponse firstResponse = executeRequest(baseDownloadUrl, null);

        if (isBinaryResponse(firstResponse)) {
            return persistBinaryResponse(firstResponse);
        }

        String html = firstResponse.readBodyAsString();
        String confirmToken = extractToken(html, CONFIRM_INPUT_TOKEN, CONFIRM_TOKEN);
        String uuid = extractToken(html, UUID_TOKEN);

        if (confirmToken == null || confirmToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Não foi possível baixar a planilha do Google Drive. Verifique se o arquivo está compartilhado para acesso."
            );
        }

        StringBuilder confirmedUrl = new StringBuilder(baseDownloadUrl)
                .append("&confirm=")
                .append(confirmToken);

        if (uuid != null && !uuid.isBlank()) {
            confirmedUrl.append("&uuid=").append(uuid);
        }

        HttpResponse confirmedResponse = executeRequest(confirmedUrl.toString(), firstResponse.cookieHeader);
        if (!isBinaryResponse(confirmedResponse)) {
            throw new IllegalArgumentException(
                    "O Google Drive não retornou um arquivo Excel válido. Verifique se o link aponta para um .xls ou .xlsx."
            );
        }

        return persistBinaryResponse(confirmedResponse);
    }

    public static boolean isGoogleSheetsUrl(String value) {
        if (!isGoogleDriveUrl(value)) {
            return false;
        }

        String lowered = value.trim().toLowerCase(Locale.ROOT);
        return lowered.contains("docs.google.com/spreadsheets/");
    }

    public static SpreadsheetFormat detectSpreadsheetFormat(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] header = input.readNBytes(8);
            if (header.length >= XLS_MAGIC.length && startsWith(header, XLS_MAGIC)) {
                return SpreadsheetFormat.XLS;
            }
            if (header.length >= ZIP_MAGIC.length && startsWith(header, ZIP_MAGIC)) {
                return SpreadsheetFormat.XLSX;
            }
            return SpreadsheetFormat.UNKNOWN;
        }
    }

    private static SpreadsheetRemoteFile persistBinaryResponse(HttpResponse response) throws Exception {
        Path tempFile = Files.createTempFile("splinker-gdrive-", ".tmp");
        try (InputStream input = response.connection.getInputStream()) {
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        SpreadsheetFormat format = detectSpreadsheetFormat(tempFile);
        if (format == SpreadsheetFormat.UNKNOWN) {
            Files.deleteIfExists(tempFile);
            throw new IllegalArgumentException("O arquivo remoto do Google Drive deve estar em formato .xls ou .xlsx.");
        }

        return new SpreadsheetRemoteFile(tempFile, format);
    }

    private static HttpResponse executeRequest(String url, String cookieHeader) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            connection.setRequestProperty("Cookie", cookieHeader);
        }

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw buildRequestException(statusCode);
        }

        return new HttpResponse(connection, extractCookies(connection));
    }

    private static boolean isBinaryResponse(HttpResponse response) {
        String contentType = response.connection.getContentType();
        String disposition = response.connection.getHeaderField("Content-Disposition");

        if (disposition != null && disposition.toLowerCase(Locale.ROOT).contains("attachment")) {
            return true;
        }

        if (contentType == null) {
            return true;
        }

        String lowered = contentType.toLowerCase(Locale.ROOT);
        return !lowered.contains("text/html");
    }

    private static String extractCookies(HttpURLConnection connection) {
        List<String> rawCookies = connection.getHeaderFields().get("Set-Cookie");
        if (rawCookies == null || rawCookies.isEmpty()) {
            return "";
        }

        List<String> cookies = new ArrayList<>();
        for (String rawCookie : rawCookies) {
            if (rawCookie == null || rawCookie.isBlank()) {
                continue;
            }
            String[] parts = rawCookie.split(";", 2);
            if (parts.length > 0 && !parts[0].isBlank()) {
                cookies.add(parts[0]);
            }
        }

        return String.join("; ", cookies);
    }

    private static String extractToken(String html, Pattern... patterns) {
        if (html == null || html.isBlank()) {
            return null;
        }

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String buildGoogleSheetsExportUrl(String fileId) {
        return "https://docs.google.com/spreadsheets/d/" + fileId + "/export?format=xlsx";
    }

    private static IllegalArgumentException buildRequestException(int statusCode) {
        if (statusCode == 401) {
            return new IllegalArgumentException("Você não tem permissão de leitura deste arquivo no Google Drive. Verifique se o link está compartilhado corretamente.");
        }
        if (statusCode == 403) {
            return new IllegalArgumentException("Sem permissão para acessar este arquivo no Google Drive. Verifique se o link está compartilhado.");
        }
        if (statusCode == 404) {
            return new IllegalArgumentException("Arquivo não encontrado no Google Drive. Verifique se o link está correto.");
        }
        return new IllegalArgumentException("Falha ao acessar o arquivo remoto no Google Drive. HTTP " + statusCode);
    }

    public enum SpreadsheetFormat {
        XLS,
        XLSX,
        UNKNOWN
    }

    public static final class SpreadsheetRemoteFile {
        private final Path localFile;
        private final SpreadsheetFormat format;

        public SpreadsheetRemoteFile(Path localFile, SpreadsheetFormat format) {
            this.localFile = localFile;
            this.format = format;
        }

        public Path getLocalFile() {
            return localFile;
        }

        public SpreadsheetFormat getFormat() {
            return format;
        }
    }

    private static final class HttpResponse {
        private final HttpURLConnection connection;
        private final String cookieHeader;

        private HttpResponse(HttpURLConnection connection, String cookieHeader) {
            this.connection = connection;
            this.cookieHeader = cookieHeader;
        }

        private String readBodyAsString() throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                return builder.toString();
            }
        }
    }
}
