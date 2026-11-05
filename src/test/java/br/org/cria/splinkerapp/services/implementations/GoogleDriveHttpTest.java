package br.org.cria.splinkerapp.services.implementations;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import br.org.cria.splinkerapp.services.implementations.GoogleDriveFileService.*;

public class GoogleDriveHttpTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private static final String DRIVE = "https://drive.google.com/file/d/test-id/view";
    private static final String SHEET = "https://docs.google.com/spreadsheets/d/test-id/edit";
    private static final byte[] XLS = {(byte)0xd0, (byte)0xcf, 0x11, (byte)0xe0, (byte)0xa1, (byte)0xb1, 0x1a, (byte)0xe1};
    private HttpURLConnection response(int status, String type, byte[] data) throws Exception {
        var c = mock(HttpURLConnection.class);
        when(c.getResponseCode()).thenReturn(status); when(c.getContentType()).thenReturn(type);
        when(c.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(data));
        when(c.getHeaderFields()).thenReturn(Map.of()); return c;
    }
    @Test public void downloadsDriveAndSheetsWithoutNetwork() throws Exception {
        try (var service = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            for (String link : List.of(DRIVE, SHEET)) {
                var c = response(200, "application/octet-stream", XLS);
                service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(c);
                var remote = GoogleDriveFileService.downloadSpreadsheet(link);
                try { assertEquals(SpreadsheetFormat.XLS, remote.getFormat()); assertArrayEquals(XLS, Files.readAllBytes(remote.getLocalFile())); }
                finally { Files.deleteIfExists(remote.getLocalFile()); }
                verify(c).setRequestMethod("GET"); verify(c).setConnectTimeout(15000); verify(c).setReadTimeout(60000);
            }
        }
    }
    @Test public void followsConfirmationWithUuidAndCookies() throws Exception {
        try (var service = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            var initial = response(200, "text/html", "<input name=\"confirm\" value=\"confirm-token\"><input name=\"uuid\" value=\"uuid-token\">".getBytes());
            when(initial.getHeaderFields()).thenReturn(Map.of("Set-Cookie", Arrays.asList(null, " ", "session=abc; path=/", "other=value; Secure")));
            var confirmed = response(200, null, XLS);
            service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(initial, confirmed);
            var remote = GoogleDriveFileService.downloadSpreadsheet(DRIVE);
            Files.deleteIfExists(remote.getLocalFile());
            service.verify(() -> GoogleDriveFileService.openConnection("https://drive.google.com/uc?export=download&id=test-id&confirm=confirm-token&uuid=uuid-token"));
            verify(confirmed).setRequestProperty("Cookie", "session=abc; other=value");
        }
    }
    @Test public void followsQueryConfirmationAndAttachment() throws Exception {
        try (var service = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            var initial = response(200, "text/html", "confirm=abc".getBytes());
            var confirmed = response(200, "text/html", new byte[]{'P', 'K', 3, 4});
            when(confirmed.getHeaderField("Content-Disposition")).thenReturn("attachment; filename=data.xlsx");
            service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(initial, confirmed);
            var remote = GoogleDriveFileService.downloadSpreadsheet(DRIVE);
            assertEquals(SpreadsheetFormat.XLSX, remote.getFormat()); Files.deleteIfExists(remote.getLocalFile());
            verify(confirmed, never()).setRequestProperty(eq("Cookie"), anyString());
        }
    }
    @Test public void reportsHttpErrorsAndNetworkFailures() throws Exception {
        try (var service = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            for (int code : List.of(401, 403, 404, 500, 302)) {
                var c = response(code, "text/html", new byte[0]);
                service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(c);
                assertThrows(IllegalArgumentException.class, () -> GoogleDriveFileService.downloadSpreadsheet(DRIVE));
            }
            service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenThrow(new IOException("offline"));
            assertThrows(IOException.class, () -> GoogleDriveFileService.downloadSpreadsheet(DRIVE));
        }
    }
    @Test public void rejectsHtmlInsteadOfSpreadsheet() throws Exception {
        try (var service = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            for (String link : List.of(DRIVE, SHEET)) {
                var c = response(200, "text/html", "Access denied".getBytes());
                service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(c);
                assertThrows(IllegalArgumentException.class, () -> GoogleDriveFileService.downloadSpreadsheet(link));
            }
            var c = response(200, "text/html", "confirm=token".getBytes());
            service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(c);
            assertThrows(IllegalArgumentException.class, () -> GoogleDriveFileService.downloadSpreadsheet(DRIVE));
            var binary = response(200, "application/octet-stream", "not a spreadsheet".getBytes());
            service.when(() -> GoogleDriveFileService.openConnection(anyString())).thenReturn(binary);
            assertThrows(IllegalArgumentException.class, () -> GoogleDriveFileService.downloadSpreadsheet(DRIVE));
        }
    }
    @Test public void validatesRemoteFileAndRemovesDownload() throws Exception {
        try (var service = mockStatic(GoogleDriveFileService.class, CALLS_REAL_METHODS)) {
            var path = temp.newFile().toPath();
            service.when(() -> GoogleDriveFileService.downloadSpreadsheet(SHEET)).thenReturn(new SpreadsheetRemoteFile(path, SpreadsheetFormat.XLSX));
            GoogleDriveFileService.validateAccess(SHEET); assertFalse(Files.exists(path));
        }
    }
    @Test public void rejectsInvalidPathsAndIds() throws Exception {
        for (String value : Arrays.asList(null, "", " ")) assertThrows(IllegalArgumentException.class, () -> GoogleDriveFileService.validateAccess(value));
        for (String value : List.of("https://example.org/a", "https://drive.google.com/", "https://drive.google.com/open?id=")) {
            assertThrows(IllegalArgumentException.class, () -> GoogleDriveFileService.downloadSpreadsheet(value));
        }
        assertFalse(GoogleDriveFileService.isRemotePath(null)); assertFalse(GoogleDriveFileService.isGoogleSheetsUrl("local.xlsx"));
        assertNull(GoogleDriveFileService.extractFileId(null)); assertNull(GoogleDriveFileService.extractFileId(" "));
        assertEquals("a b", GoogleDriveFileService.extractFileId("https://drive.google.com/open?id=a%20b"));
        assertEquals(SpreadsheetFormat.UNKNOWN, GoogleDriveFileService.detectSpreadsheetFormat(temp.newFile().toPath()));
    }
}
