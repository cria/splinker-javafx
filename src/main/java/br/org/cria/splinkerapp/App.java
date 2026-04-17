package br.org.cria.splinkerapp;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.LocalUpdateService;
import br.org.cria.splinkerapp.services.implementations.VersionService;
import br.org.cria.splinkerapp.utils.SQLiteTableExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class App {

    private static final String COMMAND_VERSION = "--version";
    private static final String COMMAND_CHECK_UPDATE = "--latest-version";
    private static final String COMMAND_UPDATE = "--update";
    private static final String COMMAND_HELP = "--help";
    private static final String COMMAND_EXAMPLE = "--example";

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            Main.main(args);
            return;
        }

        if (isCommandMode(args)) {
            processarComando(args[0]);
            return;
        }

        String filePath = args[0];
        Set<String> collectionsFilter = extractCollectionsFilter(args);

        ApplicationLog.info("                WELCOME TO SPLINK                 ");
        ApplicationLog.info("Starting transmission process");

        List<DataSetConfig> datasets;
        try {
            datasets = parseTxt(filePath);
        } catch (IOException e) {
            ApplicationLog.error("Error reading file: " + e.getMessage());
            ApplicationLog.info("Use --example to see the expected file format.");
            return;
        }

        if (datasets.isEmpty()) {
            ApplicationLog.warn("No datasets found in the file.");
            ApplicationLog.info("Use --example to see the expected file format.");
            return;
        }

        DatabaseSetup.iniciarDB();

        int total = 0;
        int sucesso = 0;
        int falha = 0;
        int ignorados = 0;

        for (DataSetConfig dataSetConfig : datasets) {
            total++;
            ProcessamentoResultado resultado = processarDataset(dataSetConfig, collectionsFilter);

            switch (resultado) {
                case SUCESSO:
                    sucesso++;
                    break;
                case FALHA:
                    falha++;
                    break;
                case IGNORADO:
                    ignorados++;
                    break;
                default:
                    break;
            }
        }

        ApplicationLog.info("==========================================");
        ApplicationLog.info("Processing finished.");
        ApplicationLog.info("Total processed: " + total);
        ApplicationLog.info("Success: " + sucesso);
        ApplicationLog.info("Failed: " + falha);
        ApplicationLog.info("Ignored: " + ignorados);
    }

    private static boolean isCommandMode(String[] args) {
        return args.length >= 1 && args[0] != null && args[0].startsWith("--");
    }

    private static void processarComando(String command) {
        try {
            switch (command) {
                case COMMAND_VERSION:
                    exibirVersaoLocal();
                    break;
                case COMMAND_CHECK_UPDATE:
                    exibirVersaoReleaseGithub();
                    break;
                case COMMAND_UPDATE:
                    atualizarAplicacao();
                    break;
                case COMMAND_EXAMPLE:
                    exibirExemploArquivo();
                    break;
                case COMMAND_HELP:
                    exibirAjuda();
                    break;
                default:
                    ApplicationLog.warn("Invalid command: " + command);
                    exibirAjuda();
                    break;
            }
        } catch (Exception e) {
            ApplicationLog.error("Error processing command " + command + ": " + e.getMessage());
        }
    }

    private static void exibirVersaoLocal() {
        String version = VersionService.getVersion();
        ApplicationLog.info(version);
    }

    private static void exibirVersaoReleaseGithub() throws Exception {
        String releaseVersion = VersionService.getReleaseCurrentVersion();

        if (releaseVersion == null || releaseVersion.isBlank()) {
            ApplicationLog.warn("Could not determine GitHub release version.");
            return;
        }

        ApplicationLog.info(releaseVersion);
    }

    private static void verificarSeExisteAtualizacao() throws Exception {
        String versaoLocal = VersionService.getVersion();
        String versaoGithub = VersionService.getReleaseCurrentVersion();

        if (versaoGithub == null || versaoGithub.isBlank()) {
            ApplicationLog.warn("Could not fetch GitHub release version.");
            return;
        }

        ApplicationLog.info("Local version: " + versaoLocal);
        ApplicationLog.info("GitHub release version: " + versaoGithub);

        if (VersionService.isNewerVersion(versaoLocal, versaoGithub)) {
            ApplicationLog.info("A new version is available.");
        } else {
            ApplicationLog.info("Your version is up to date.");
        }
    }

    private static void atualizarAplicacao() throws Exception {
        String versaoLocal = VersionService.getVersion();
        String versaoGithub = VersionService.getReleaseCurrentVersion();

        if (versaoGithub == null || versaoGithub.isBlank()) {
            ApplicationLog.warn("Could not fetch GitHub release version.");
            return;
        }

        ApplicationLog.info("Local version: " + versaoLocal);
        ApplicationLog.info("GitHub release version: " + versaoGithub);

        if (!VersionService.isNewerVersion(versaoLocal, versaoGithub)) {
            ApplicationLog.info("Your version is already up to date. No update will be executed.");
            return;
        }

        ApplicationLog.info("New version found. Starting update...");
        LocalUpdateService.executeUpdateFromGithubRelease();
    }

    private static void exibirAjuda() {
        ApplicationLog.info("Usage:");
        ApplicationLog.info("  java -jar splinker.jar <config-file.txt>");
        ApplicationLog.info("  java -jar splinker.jar <config-file.txt> --collections=collectionA,collectionB");
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_VERSION);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_CHECK_UPDATE);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_UPDATE);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_EXAMPLE);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_HELP);
    }

    private static void exibirExemploArquivo() {
        System.out.println("""
                Example configuration file:

                [dataset]
                token=TOKEN
                filePath=C:\\Users\\user\\Downloads\\file.xlsx

                [dataset]
                token=TOKEN
                host=localhost
                port=5432
                dbname=my_database
                user=postgres
                password=123456

                [dataset]
                token=TOKEN
                filePath=C:\\Users\\user\\Downloads\\base.accdb
                password=123456

                Notes:
                - Each [dataset] represents a configuration
                - The token is required
                - The remaining fields vary according to the data source type identified by the API
                """);
    }

    private static ProcessamentoResultado processarDataset(DataSetConfig dataSetConfig, Set<String> collectionsFilter) {
        String token = dataSetConfig.getToken();

        try {
            if (token == null || token.isBlank()) {
                ApplicationLog.error("Dataset ignored: token not provided.");
                return ProcessamentoResultado.FALHA;
            }

            ApplicationLog.info("==========================================");
            ApplicationLog.info("Processing token: " + token);

            var apiConfig = DataSetService.getConfigurationDataFromAPI(token);
            TokenRepository.setCurrentToken(token);

            String datasouceType = apiConfig.get("data_source_type").toString();
            DataSourceType dsType = DataSourceType.valueOf(datasouceType);

            String collName = apiConfig.get("dataset_name").toString();

            if (!collectionsFilter.isEmpty() && !collectionsFilter.contains(collName)) {
                ApplicationLog.info("Collection ignored by filter: " + collName);
                return ProcessamentoResultado.IGNORADO;
            }

            ApplicationLog.info("Type: " + datasouceType);
            ApplicationLog.info("Collection: " + collName);

            String datasetAcronym = apiConfig.get("dataset_acronym").toString();
            int id = (int) Double.parseDouble(apiConfig.get("dataset_id").toString());

            DataSetService.saveDataSet(token, dsType, datasetAcronym, collName, id);

            ConfigFacade.HandleBackendData(token, apiConfig);

            salvarFonte(token, dsType, dataSetConfig.getConfig());

            DataSet ds = DataSetService.getDataSet(token);

            if (ds.isFile() || ds.isAccessDb()) {
                ApplicationLog.info("Starting data import. Please wait...");
                FileSourceManager fileSourceManager = new FileSourceManager(ds);
                fileSourceManager.importData(
                        SQLiteTableExtractor.extrairTabelas(
                                DataSetService.getSQLCommandFromApi(ds.getToken())
                        )
                );
                ApplicationLog.info("Import finished");
            }

            DarwinCoreArchiveService service = new DarwinCoreArchiveService(ds);
            service.readDataFromSource()
                    .generateZIPFile()
                    .transferData()
                    .cleanData();

            ApplicationLog.info("Transmission completed successfully");
            DataSetService.deleteDataSet(token);
            return ProcessamentoResultado.SUCESSO;

        } catch (Exception e) {
            ApplicationLog.error("Error processing token " + token + ": " + e.getMessage());
            return ProcessamentoResultado.FALHA;
        }
    }

    private static void salvarFonte(String token, DataSourceType dsType, Map<String, String> cfg) throws Exception {
        switch (dsType) {
            case Access:
                validar(cfg, "filePath");
                DataSetService.saveAccessDataSource(token, cfg.get("filePath"), cfg.get("password"));
                break;

            case dBase:
            case Excel:
            case LibreOfficeCalc:
            case CSV:
            case Numbers:
                validar(cfg, "filePath");
                DataSetService.saveSpreadsheetDataSource(token, cfg.get("filePath"));
                break;

            case MySQL:
            case PostgreSQL:
            case SQLServer:
            case Oracle:
                validar(cfg, "host", "port", "dbname", "user", "password");
                DataSetService.saveSQLDataSource(token, cfg.get("host"), cfg.get("port"), cfg.get("dbname"), cfg.get("user"), cfg.get("password"));
                break;

            default:
                ApplicationLog.warn("Unhandled data source type: " + dsType);
                break;
        }
    }

    private static List<DataSetConfig> parseTxt(String filePath) throws IOException {
        List<DataSetConfig> datasets = new ArrayList<>();
        DataSetConfig current = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("[dataset]")) {
                    if (current != null) datasets.add(current);
                    current = new DataSetConfig();
                    continue;
                }

                if (current != null && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (key.equalsIgnoreCase("token")) {
                        current.setToken(value);
                    } else {
                        current.getConfig().put(key, value);
                    }
                }
            }

            if (current != null) datasets.add(current);
        }

        return datasets;
    }

    private static Set<String> extractCollectionsFilter(String[] args) {
        return Arrays.stream(args)
                .skip(1)
                .filter(arg -> arg.startsWith("--collections="))
                .map(arg -> arg.substring("--collections=".length()))
                .flatMap(valor -> Arrays.stream(valor.split(",")))
                .map(String::trim)
                .filter(valor -> !valor.isBlank())
                .collect(Collectors.toSet());
    }

    private static void validar(Map<String, String> cfg, String... campos) {
        for (String campo : campos) {
            String valor = cfg.get(campo);
            if (valor == null || valor.isBlank()) {
                throw new IllegalArgumentException("Required field not provided: " + campo);
            }
        }
    }

    private enum ProcessamentoResultado {
        SUCESSO,
        FALHA,
        IGNORADO
    }

    static class DataSetConfig {
        private String token;
        private final Map<String, String> config = new HashMap<>();

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Map<String, String> getConfig() { return config; }
    }
}