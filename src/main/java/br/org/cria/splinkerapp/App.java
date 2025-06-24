package br.org.cria.splinkerapp;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class App {
    public static void main(final String[] args) throws Exception {
        if (args.length > 0) {
            String filePath = args[0];
            ApplicationLog.info("                BEM VINDO AO SPLINK                 ");
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                ApplicationLog.info("Iniciando processo de transmissão");
                String token = reader.readLine();
                ApplicationLog.info("Token: " + token);

                DatabaseSetup.iniciarDB();
                var apiConfig = DataSetService.getConfigurationDataFromAPI(token);
                TokenRepository.setCurrentToken(token);
                String datasouceType = apiConfig.get("data_source_type").toString();
                DataSourceType dsType = DataSourceType.valueOf(datasouceType);
                ApplicationLog.info("Tipo: " + datasouceType);

                var collName = apiConfig.get("dataset_name").toString();
                ApplicationLog.info("Coleção: " + collName);
                var datasetAcronym = apiConfig.get("dataset_acronym").toString();
                var id = (int) Double.parseDouble(apiConfig.get("dataset_id").toString());
                DataSetService.saveDataSet(token, dsType, datasetAcronym, collName, id);

                ConfigFacade.HandleBackendData(token, apiConfig);
                switch (dsType) {
                    case Access:
                        String path = reader.readLine();
                        String password = reader.readLine();
                        DataSetService.saveAccessDataSource(token, path, password);
                        break;
                    case dBase:
                    case Excel:
                    case LibreOfficeCalc:
                    case CSV:
                    case Numbers:
                        String urlDataset = reader.readLine();
                        DataSetService.saveSpreadsheetDataSource(token, urlDataset);
                        break;
                    case MySQL:
                    case PostgreSQL:
                    case SQLServer:
                    case Oracle:
                        String host = reader.readLine();
                        String port = reader.readLine();
                        String dbname = reader.readLine();
                        String userName = reader.readLine();
                        String passwordDb = reader.readLine();
                        DataSetService.saveSQLDataSource(token, host, port, dbname, userName, passwordDb);
                    default:
                        break;
                }

                DataSet ds = DataSetService.getDataSet(token);
                ApplicationLog.info("Iniciando importação de dados. Aguarde alguns minutos.");
                FileSourceManager fileSourceManager = new FileSourceManager(ds);
                fileSourceManager.importData();
                ApplicationLog.info("Importação finalizada");

                DarwinCoreArchiveService service = new DarwinCoreArchiveService(ds);
                service.readDataFromSource().generateTXTFile().generateZIPFile().transferData().cleanData();
                ApplicationLog.info("Tranmissão finalizada com sucesso");
                DataSetService.deleteDataSet(token);
            } catch (IOException e) {
                ApplicationLog.error("Erro ao ler o arquivo: " + e.getMessage());
            }
        } else {
            Main.main(args);
        }
    }
}
