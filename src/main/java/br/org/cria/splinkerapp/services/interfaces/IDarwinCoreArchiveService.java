package br.org.cria.splinkerapp.services.interfaces;

import br.org.cria.splinkerapp.models.DataSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import javafx.concurrent.Service;
public interface IDarwinCoreArchiveService {
    IDarwinCoreArchiveService generateTXTFile() throws IOException, SQLException;
    IDarwinCoreArchiveService generateZIPFile() throws IOException;
    IDarwinCoreArchiveService readDataFromSource(DataSource source) throws Exception;
    Service<Void> transferData() throws FileNotFoundException;
}
