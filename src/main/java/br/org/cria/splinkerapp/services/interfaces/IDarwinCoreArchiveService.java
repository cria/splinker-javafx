package br.org.cria.splinkerapp.services.interfaces;

import br.org.cria.splinkerapp.models.DataSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javafx.concurrent.Service;
public interface IDarwinCoreArchiveService {
    IDarwinCoreArchiveService generateTXTFile() throws IOException;
    IDarwinCoreArchiveService generateZIPFile() throws IOException;
    IDarwinCoreArchiveService readDataFromSource(DataSource source) throws SQLException, IOException, URISyntaxException;
    Service transferData() throws FileNotFoundException;
}
