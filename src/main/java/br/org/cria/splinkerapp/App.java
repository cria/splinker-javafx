package br.org.cria.splinkerapp;

import br.org.cria.splinkerapp.config.DatabaseSetup;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.services.implementations.VersionService;
import br.org.cria.splinkerapp.utils.SQLiteTableExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class App {

    private static final String COMMAND_VERSION = "--version";
    private static final String COMMAND_RELEASE_VERSION = "--release-version";
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

        ApplicationLog.info("                BEM VINDO AO SPLINK                 ");
        ApplicationLog.info("Iniciando processo de transmissão");

        List<DataSetConfig> datasets;
        try {
            datasets = parseTxt(filePath);
        } catch (IOException e) {
            ApplicationLog.error("Erro ao ler o arquivo: " + e.getMessage());
            return;
        }

        if (datasets.isEmpty()) {
            ApplicationLog.warn("Nenhum dataset encontrado no arquivo.");
            return;
        }

        DatabaseSetup.iniciarDB();

        for (DataSetConfig dataSetConfig : datasets) {
            processarDataset(dataSetConfig, collectionsFilter);
        }

        ApplicationLog.info("Processamento finalizado.");
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
                case COMMAND_RELEASE_VERSION:
                    exibirVersaoReleaseGithub();
                    break;
                case COMMAND_HELP:
                    exibirAjuda();
                    break;
                case COMMAND_EXAMPLE:
                    exibirExemploArquivo();
                    break;
                default:
                    ApplicationLog.warn("Comando inválido: " + command);
                    exibirAjuda();
                    break;
            }
        } catch (Exception e) {
            ApplicationLog.error("Erro ao processar comando " + command + ": " + e.getMessage());
        }
    }

    private static void exibirExemploArquivo() {
        System.out.println("""
                Exemplo de arquivo de configuração (config.txt):
                
                [dataset]
                token=SEU_TOKEN_AQUI
                caminhoArquivo=C:\\caminho\\arquivo.xlsx
                
                [dataset]
                token=SEU_TOKEN_AQUI
                enderecoHost=localhost
                porta=5432
                nomeBanco=meu_banco
                usuario=postgres
                senha=123456
                
                [dataset]
                token=SEU_TOKEN_AQUI
                caminhoArquivo=C:\\caminho\\base.accdb
                senha=senha_access
                
                Observações:
                - Cada [dataset] representa uma configuração
                - O token é obrigatório
                - Os campos variam conforme o tipo da fonte
                """);
    }

    private static void exibirVersaoLocal() throws IOException {
        String version = VersionService.getVersion();
        ApplicationLog.info("Versão atual: " + version);
    }

    private static void exibirVersaoReleaseGithub() throws Exception {
        String releaseVersion = VersionService.getReleaseCurrentVersion();

        if (releaseVersion == null || releaseVersion.isBlank()) {
            ApplicationLog.warn("Não foi possível identificar a versão da release no GitHub.");
            return;
        }

        ApplicationLog.info("Versão atual da release no GitHub: " + releaseVersion);
    }

    private static void exibirAjuda() {
        ApplicationLog.info("Uso:");
        ApplicationLog.info("  java -jar splinker.jar <arquivo-config.txt>");
        ApplicationLog.info("  java -jar splinker.jar <arquivo-config.txt> --collections=acronimoColecaoA,acronimoColecaoA");
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_VERSION);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_RELEASE_VERSION);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_HELP);
        ApplicationLog.info("  java -jar splinker.jar " + COMMAND_EXAMPLE);
    }

    private static void processarDataset(DataSetConfig dataSetConfig, Set<String> collectionsFilter) {
        String token = dataSetConfig.getToken();

        try {
            if (token == null || token.isBlank()) {
                ApplicationLog.error("Dataset ignorado: token não informado.");
                return;
            }

            ApplicationLog.info("==========================================");
            ApplicationLog.info("Processando token: " + token);

            var apiConfig = DataSetService.getConfigurationDataFromAPI(token);
            TokenRepository.setCurrentToken(token);

            String datasouceType = apiConfig.get("data_source_type").toString();
            DataSourceType dsType = DataSourceType.valueOf(datasouceType);

            String collName = apiConfig.get("dataset_name").toString();

            if (!collectionsFilter.isEmpty() && !collectionsFilter.contains(collName)) {
                ApplicationLog.info("Coleção ignorada: " + collName);
                return;
            }

            ApplicationLog.info("Tipo: " + datasouceType);
            ApplicationLog.info("Coleção: " + collName);

            String datasetAcronym = apiConfig.get("dataset_acronym").toString();
            int id = (int) Double.parseDouble(apiConfig.get("dataset_id").toString());

            DataSetService.saveDataSet(token, dsType, datasetAcronym, collName, id);

            ConfigFacade.HandleBackendData(token, apiConfig);

            salvarFonte(token, dsType, dataSetConfig.getConfig());

            DataSet ds = DataSetService.getDataSet(token);

            ApplicationLog.info("Iniciando importação de dados...");
            FileSourceManager fileSourceManager = new FileSourceManager(ds);
            fileSourceManager.importData(
                    SQLiteTableExtractor.extrairTabelas(
                            DataSetService.getSQLCommandFromApi(ds.getToken())
                    )
            );
            ApplicationLog.info("Importação finalizada");

            DarwinCoreArchiveService service = new DarwinCoreArchiveService(ds);
            service.readDataFromSource()
                    .generateZIPFile()
                    .transferData()
                    .cleanData();

            ApplicationLog.info("Transmissão finalizada com sucesso");

            DataSetService.deleteDataSet(token);

        } catch (Exception e) {
            ApplicationLog.error("Erro ao processar token " + token + ": " + e.getMessage());
        }
    }

    private static void salvarFonte(String token, DataSourceType dsType, Map<String, String> cfg) throws Exception {

        switch (dsType) {

            case Access:
                validar(cfg, "caminhoArquivo");
                DataSetService.saveAccessDataSource(
                        token,
                        cfg.get("caminhoArquivo"),
                        cfg.get("senha")
                );
                break;

            case dBase:
            case Excel:
            case LibreOfficeCalc:
            case CSV:
            case Numbers:
                validar(cfg, "caminhoArquivo");
                DataSetService.saveSpreadsheetDataSource(
                        token,
                        cfg.get("caminhoArquivo")
                );
                break;

            case MySQL:
            case PostgreSQL:
            case SQLServer:
            case Oracle:
                validar(cfg, "enderecoHost", "porta", "nomeBanco", "usuario", "senha");

                DataSetService.saveSQLDataSource(
                        token,
                        cfg.get("enderecoHost"),
                        cfg.get("porta"),
                        cfg.get("nomeBanco"),
                        cfg.get("usuario"),
                        cfg.get("senha")
                );
                break;

            default:
                ApplicationLog.warn("Tipo de fonte não tratado: " + dsType);
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
                    if (current != null) {
                        datasets.add(current);
                    }
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

            if (current != null) {
                datasets.add(current);
            }
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
                throw new IllegalArgumentException("Campo obrigatório não informado: " + campo);
            }
        }
    }

    static class DataSetConfig {
        private String token;
        private Map<String, String> config = new HashMap<>();

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Map<String, String> getConfig() {
            return config;
        }
    }
}