package br.org.cria.splinkerapp.services.implementations;

import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.services.interfaces.IDarwinCoreArchiveService;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import com.google.gson.Gson;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService implements IDarwinCoreArchiveService {
    static String zipFile = "%s/spLinker_dwca.zip".formatted(System.getProperty("user.dir"));
    static String textFile = "%s/spLinker_dwca.txt".formatted(System.getProperty("user.dir"));

    @Override
    public DarwinCoreArchiveService generateTXTFile() {
        var file = new File(textFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new UnsupportedOperationException("Unimplemented method 'generateTXTFile'");
    }

    @Override
    public DarwinCoreArchiveService generateZIPFile() {

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            File fileToZip = new File(textFile);
            zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
            Files.copy(fileToZip.toPath(), zipOut);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DarwinCoreArchiveService readDataFromSource(DataSource source)
            throws SQLException, IOException, URISyntaxException {
        String line;
        var command = "";
        var urlConn = new URI("http://localhost:8000/api/get_query").toURL();
        var response = new StringBuffer();
        var connection = (HttpURLConnection) urlConn.openConnection();
        connection.setRequestMethod("GET");
        
        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        // Disconnect the connection
        connection.disconnect();

        HashMap<String, List<Double>> json = new Gson().fromJson(response.toString(), HashMap.class);

        var strValues = json.get("cmd");
        var out = new ByteArrayOutputStream();
        strValues.forEach((e) -> out.write(e.byteValue()));
        command = new String(out.toByteArray(), StandardCharsets.UTF_8);

        switch (source.getType()) {
            case MySQL:
            case PostgreSQL:
            case SQLServer:
                break;
            default:
                var uri = "jdbc:sqlite:splinker.db";
                var conn = DriverManager.getConnection(uri);
                var statement = conn.createStatement();
                var resulSet = statement.executeQuery(command);
                while (resulSet.next()) {
                    var content = resulSet.getString(4);
                    System.out.println(content);
                }
                break;
        }
        return this;
    }

    @Override
    public Service transferData() {
        // TODO: Ler map.dat e db.dat das coleções.
        // Esses arquivos são combinados para montar o select dos dados
        // nas fontes de dados das coleções (DB, planilhas, Brahms, etc)
        // O que não for planilha, é banco sempre
        var port = ConfigurationData.getRSyncPort();
        var source = "/Users/brunobemfica/Downloads/dummy_data.dart";
        // var source =
        // "%s/splinker_dwca.zip".formatted(System.getProperty("user.dir"));
        var destination = ConfigurationData.getTransferDataDestination();
        var command = new String[] { "--port=%s".formatted(port), "-r", source, destination };
        var client = new YajsyncClient();
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        var session = client.start(command);
                        return null;
                    }
                };
            }
        };

    }
}
