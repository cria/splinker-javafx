package br.org.cria.splinkerapp.repositories;

import br.org.cria.splinkerapp.models.*;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import org.junit.Test;
import java.util.*;
import java.net.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RepositoriesTest extends RepositoryTestBase {
    @Test public void savesAndReplacesCentralServiceConfiguration() throws Exception {
        assertNotNull(CentralServiceRepository.getCentralServiceData().getCentralServiceUrl());
        CentralServiceRepository.saveCentralServiceData("https://service.invalid/api", "7.5");
        assertEquals("https://service.invalid/api", CentralServiceRepository.getCentralServiceData().getCentralServiceUrl());
        assertEquals("7.5", CentralServiceRepository.getCurrentVersion());
        assertThrows(Exception.class, () -> CentralServiceRepository.saveCentralServiceData("", "7.5"));
        assertThrows(Exception.class, () -> CentralServiceRepository.saveCentralServiceData(null, "7.5"));
        cleanTable("CentralServiceConfiguration"); assertNull(CentralServiceRepository.getCurrentVersion());
        assertNull(CentralServiceRepository.getCentralServiceData().getCentralServiceUrl());
    }
    @Test public void savesAndReplacesTransferConfiguration() throws Exception {
        assertEquals(0, TransferConfigRepository.getRSyncConfig().getrSyncPort());
        TransferConfigRepository.saveRSyncConfig(873, "rsync.invalid"); TransferConfigRepository.saveRSyncConfig(874, "new.invalid");
        var config = TransferConfigRepository.getRSyncConfig(); assertEquals(874, config.getrSyncPort()); assertEquals("new.invalid", config.getrSyncDestination());
    }
    @Test public void savesAndReplacesProxyConfiguration() throws Exception {
        assertNull(ProxyConfigRepository.getConfiguration());
        ProxyConfigRepository.saveProxyConfig(new ProxyConfiguration("proxy.invalid", "secret", "8080", "user"));
        var config = ProxyConfigRepository.getConfiguration(); assertEquals("proxy.invalid", config.getAddress());
        assertEquals("secret", config.getPassword()); assertEquals("8080", config.getPort()); assertEquals("user", config.getUsername());
    }
    @Test public void detectsSystemProxyWithoutNetwork() {
        var original = ProxySelector.getDefault(); var previous = System.getProperty("java.net.useSystemProxies");
        try {
            var selector = mock(ProxySelector.class); ProxySelector.setDefault(selector);
            when(selector.select(any())).thenReturn(List.of(Proxy.NO_PROXY)); assertFalse(ProxyConfigRepository.isBehindProxyServer());
            when(selector.select(any())).thenReturn(List.of(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("proxy.invalid", 8080))));
            assertTrue(ProxyConfigRepository.isBehindProxyServer());
            when(selector.select(any())).thenThrow(new IllegalStateException("simulated")); assertFalse(ProxyConfigRepository.isBehindProxyServer());
        } finally {
            ProxySelector.setDefault(original);
            if (previous == null) System.clearProperty("java.net.useSystemProxies"); else System.setProperty("java.net.useSystemProxies", previous);
        }
    }
    @Test public void savesAndReplacesEmailConfiguration() throws Exception {
        assertNull(DataSetService.getEmailConfiguration().getContact_email_send());
        EmailRepository.saveEmailData("from@example.invalid", "secret", "to@example.invalid");
        var config = DataSetService.getEmailConfiguration();
        assertEquals("from@example.invalid", config.getContact_email_send()); assertEquals("secret", config.getContact_email_token());
        assertEquals("to@example.invalid", config.getContact_email_recipient());
    }
    @Test public void tokenSelectionAndLookupHandleEmptyAndPopulatedDatabase() throws Exception {
        assertEquals("", TokenRepository.getCurrentToken()); assertEquals("", TokenRepository.getCurrentTokenSigla());
        System.clearProperty("splinker_token"); assertEquals("", TokenRepository.getCurrentTokenSigla());
        DataSetService.saveDataSet("token", DataSourceType.CSV, "ACR", "Name", 1);
        assertEquals("token", TokenRepository.getCurrentToken()); assertEquals("ACR", TokenRepository.getCurrentTokenSigla());
        assertEquals(List.of("ACR"), new ArrayList<>(TokenRepository.getTokens()));
        assertEquals(List.of("ACR"), new ArrayList<>(TokenRepository.getAcronyms()));
        assertEquals("token", TokenRepository.getCurrentTokenByAcronym("ACR")); assertEquals("", TokenRepository.getCurrentTokenByAcronym("missing"));
        sql("DROP TABLE DataSetConfiguration"); assertEquals("", TokenRepository.getCurrentTokenSigla());
        assertEquals("", TokenRepository.getCurrentTokenByAcronym("ACR"));
    }
    @Test public void decodesUtf8ApiBytes() {
        var bytes = "São Paulo".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var values = new ArrayList<Double>(); for (byte b : bytes) values.add((double)(b & 255));
        assertEquals("São Paulo", byteArrayToString(values)); assertEquals("", byteArrayToString(List.of()));
    }
}
