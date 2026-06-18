# Backlog sugerida - spLinker JavaFX

Analise feita sobre o projeto `splinker-javafx`, uma aplicacao Java 21/JavaFX com SQLite local, importadores de arquivos e bancos, geracao de Darwin Core Archive, transferencia via rsync, modo headless e release via GitHub Actions.

## Issues sugeridas

| Tipo | Complexidade | Titulo | Descricao |
|---|---:|---|---|
| Bug | M | Corrigir contadores de linhas dos parsers | Os testes de Access, DBF, ODS, XLS e XLSX esperam milhares de linhas, mas retornam `0`. Investigar fluxo de `currentRow`/`totalRowCount`, criacao das tabelas e importacao em batch. |
| Bug | P | Corrigir comparacao de `String` no Darwin Core export | Em `DarwinCoreArchiveService`, ha comparacoes com `!=` para `"null"`, `"\r"` e `"\t"`. Trocar por `.equals`, `.equalsIgnoreCase` ou normalizacao para evitar exportar valores invalidos no `occurrence.txt`. |
| Bug | M | Corrigir parsing de CSV com campos escapados | `CsvFileParser` usa Univocity, mas depois faz `row[0].split(separator, -1)`. Isso quebra CSV com aspas, separador dentro do campo e linhas ja tokenizadas. Usar diretamente o array retornado pelo parser. |
| Bug | P | Corrigir mensagem de teste/encoding em Google Drive | `GoogleDriveFileServiceTest` falha por diferenca entre `nao` e `nao` acentuado. Padronizar encoding UTF-8 e mensagens esperadas. |
| Bug | M | Evitar SQL injection na limpeza de arquivos enviados | `DarwinCoreArchiveService.deleteSentFiles()` concatena token direto no SQL. Trocar por `PreparedStatement` com placeholder `?`. |
| Bug | M | Validar nomes de campos dinamicos em queries | `DataSetService.getDataSetBy(String field, ...)` monta SQL com nome de coluna livre. Restringir a uma enum/lista de campos permitidos. |
| Bug | M | Impedir falha ao criar tabela quando filtro ignora tabela | `FileParser.createTableBasedOnSheet()` executa `statement.executeUpdate(command)` mesmo quando `buildCreateTableCommand()` pode retornar `null`. Tratar `null` como "sem acao". |
| Correcao | P | Remover duplicidade da dependencia `poi-ooxml` | O Maven alerta que `org.apache.poi:poi-ooxml` esta declarado duas vezes no `pom.xml`. Manter uma declaracao unica. |
| Correcao | M | Ajustar build local para nao exigir Sentry Auth Token | `mvnw test` falha por `sentry-maven-plugin` tentando `uploadSourceBundle` sem `SENTRY_AUTH_TOKEN`. Criar profile de release ou condicionar o upload apenas no CI/release. |
| Correcao | P | Corrigir workflow que diz rodar testes mas usa `-DskipTests` | O GitHub Actions nomeia etapas como execucao de testes, mas usa `mvn package -DskipTests`. Separar `mvn test` de `mvn package` e bloquear release se testes falharem. |
| Correcao | M | Corrigir nomes fixos de artefatos Linux no release | O workflow faz upload de `splinker-1.0-1.x86_64.rpm` e `splinker_1.0_amd64.deb`, mesmo com versao dinamica. Resolver nomes por glob ou variavel. |
| Seguranca | M | Nao armazenar senhas em texto puro no SQLite | `db_password`, `proxy_password` e tokens de e-mail sao persistidos localmente sem protecao. Usar keystore do sistema, criptografia local ou ao menos mascaramento e migracao segura. |
| Seguranca | P | Reduzir logs com dados sensiveis de conexao | Ha logs de URL JDBC, usuario, host e presenca de senha. Garantir mascaramento centralizado e remover logs especificos de diagnostico em producao. |
| Seguranca | G | Revisar atualizacao automatica por scripts gerados dinamicamente | `SpLinkerUpdateService` monta scripts `.bat`/`.sh` por concatenacao e executa instaladores baixados. Validar URL, checksum/assinatura e reduzir risco de injecao via caminhos/URLs. |
| Seguranca | M | Endurecer download de releases | A atualizacao consulta GitHub Releases, mas nao valida assinatura, checksum nem origem do asset alem da URL. Adicionar verificacao de integridade. |
| Melhoria | M | Unificar `SpLinkerUpdateService`, `LocalUpdateService` e `VersionService` | Ha sobreposicao de responsabilidades para versao, download e atualizacao. Consolidar estrategia por sistema operacional e reduzir divergencias. |
| Melhoria | M | Refatorar logica pesada dos controllers | Controllers como configuracao de banco e atualizacao contem logica de validacao, conexao e processo. Extrair services testaveis e manter controller focado em UI. |
| Melhoria | P | Substituir `printStackTrace` e `System.out` por logging estruturado | Ha varios `printStackTrace`, `System.out.println` e `System.exit`. Padronizar `ApplicationLog`/Log4j e retornar erros controlados para UI/headless. |
| Melhoria | M | Melhorar tratamento de erros HTTP/API | `HttpService` nao trata status HTTP, corpo de erro e timeout de leitura/conexao em todos os fluxos. Criar cliente HTTP com timeout, status code e mensagens de erro consistentes. |
| Melhoria | M | Melhorar suporte a modo headless | O modo headless processa config `.txt`, mas falhas sao genericas e nao ha exit code confiavel. Adicionar codigos de saida, validacao de schema e relatorio por dataset. |
| Otimizacao | M | Revisar performance da exportacao Darwin Core | `getDataSetRows()` escreve linha a linha e recalcula metadados dentro do loop. Mover metadata para fora, revisar buffer e medir datasets grandes. |
| Otimizacao | M | Otimizar importacao com transacoes e batch consistentes | Alguns parsers usam batch, outros podem ter comportamento divergente. Padronizar tamanho de lote, commits e progresso para arquivos grandes. |
| Otimizacao | P | Evitar chamadas repetidas a API para SQL command | `DataSetService.getSQLCommandFromApi()` pode ser chamado mais de uma vez no fluxo. Cachear por token durante uma transmissao para reduzir latencia e risco de inconsistencia. |
| Documentacao | P | Corrigir encoding do README e docs | O README exibido no terminal tem caracteres quebrados em alguns trechos. Garantir UTF-8 sem BOM e padronizar acentuacao nos arquivos. |
| Documentacao | M | Documentar formato atual do arquivo headless | O README mostra formato legado simples, enquanto `App.parseTxt()` espera blocos `[dataset]` com `key=value`. Atualizar exemplos reais para arquivo, banco e filtro `--collections`. |
| Documentacao | P | Documentar setup local com Maven Wrapper e Sentry | Incluir como rodar testes sem Maven global, como desabilitar upload Sentry localmente e quais variaveis sao necessarias apenas para release. |
| Documentacao | M | Criar guia de troubleshooting de importacao | Documentar problemas comuns por fonte: Access protegido, CSV com separador, Google Drive, permissoes de arquivo, proxy e rsync indisponivel. |
| Documentacao | M | Criar manual do usuario final | Produzir documentacao voltada ao usuario da aplicacao, cobrindo instalacao, primeira configuracao, cadastro de token, selecao de fonte de dados, transmissao, historico, atualizacao e contato com suporte. |
| Documentacao | M | Criar guia de operacao em modo headless | Documentar uso via linha de comando com exemplos completos para planilha, Access e bancos SQL, incluindo filtros por colecao, codigos de saida esperados e interpretacao dos logs. |
| Documentacao | M | Criar documentacao tecnica para o time de desenvolvimento | Documentar arquitetura, fluxo principal de transmissao, camadas MVC, modelos persistidos, configuracao local, execucao de testes, processo de release e convencoes de desenvolvimento. |
| Documentacao | P | Criar guia de onboarding para novos desenvolvedores | Incluir passo a passo para clonar o projeto, configurar Java 21, usar Maven Wrapper, rodar a aplicacao, executar testes, configurar banco local e lidar com Sentry/proxy. |
| Documentacao | M | Documentar contratos externos e dependencias operacionais | Registrar APIs speciesLink/GitHub, rsync/Yajsync, Sentry, formatos suportados, variaveis de ambiente, portas, permissao de rede e requisitos por sistema operacional. |
| Evolucao | G | Internacionalizar textos da interface JavaFX | Externalizar textos hardcoded dos controllers e arquivos FXML para bundles de idioma (`ResourceBundle`), permitindo traducao sem alterar codigo. Comecar por `pt-BR` e preparar estrutura para `en-US`/`es-ES`. |
| Evolucao | M | Criar seletor de idioma na aplicacao | Adicionar configuracao para o usuario escolher o idioma da interface e persistir a preferencia localmente, aplicando o idioma na inicializacao e nas telas carregadas via FXML. |
| Evolucao | M | Padronizar mensagens de erro, validacao e dialogos para i18n | Centralizar mensagens de erro, alertas, validacoes, botoes e textos de progresso em chaves traduziveis, evitando strings duplicadas ou misturadas em portugues/ingles. |
| Evolucao | M | Internacionalizar logs e saidas do modo headless | Definir estrategia para logs e mensagens CLI: manter logs tecnicos em idioma padrao ou permitir idioma configuravel. Documentar e aplicar padrao em `ApplicationLog` e comandos `--help`/`--example`. |
| Testes | M | Adicionar testes de cobertura para internacionalizacao | Criar testes que validem existencia das chaves obrigatorias nos bundles, carregamento de FXML com `ResourceBundle` e ausencia de textos hardcoded novos nas telas principais. |
| Bug | M | Tratar inicializacao sem internet de forma controlada | Quando nao houver conexao, a aplicacao nao deve falhar silenciosamente nem abrir em estado quebrado. Detectar indisponibilidade de rede/API no startup e exibir mensagem clara com opcoes de tentar novamente, configurar proxy ou sair. |
| Evolucao | M | Criar modo offline limitado | Definir e implementar comportamento offline: permitir acesso a configuracoes e historico local, bloquear transmissao/atualizacao/login que dependem da API e indicar claramente quais acoes exigem internet. |
| Melhoria | M | Adicionar verificacao de conectividade e proxy antes de chamadas remotas | Criar servico central de conectividade para testar internet, API speciesLink, GitHub Releases e proxy configurado, reaproveitando resultado nos fluxos de login, atualizacao e transmissao. |
| Melhoria | P | Melhorar mensagens de erro para falta de internet | Substituir erros genericos de `IOException`, timeout ou `UnknownHostException` por mensagens amigaveis para usuario final, com orientacao sobre rede, proxy e tentativa posterior. |
| Evolucao | M | Adicionar validacao previa de fonte de dados | Antes de transmitir, validar existencia/acesso de arquivo, extensao compativel, conexao DB, query da API e permissao de escrita local. Mostrar resultado por item. |
| Evolucao | M | Permitir gerar e baixar Darwin Core Archive sem enviar ao servidor | Adicionar fluxo na interface e no modo headless para importar os dados, executar a query, gerar o `dwca.zip` e permitir salvar/baixar o arquivo localmente sem chamar `transferData()`/rsync. O fluxo deve registrar resultado, manter limpeza controlada dos arquivos temporarios e deixar claro para o usuario que os dados nao foram transmitidos ao speciesLink. |
| Evolucao | M | Adicionar relatorio detalhado por transmissao | Registrar status, duracao, linhas importadas/exportadas, tamanho do ZIP, erro tecnico e mensagem amigavel no historico local. |
| Evolucao | G | Implementar fila de transmissao resiliente | Para multiplas colecoes, permitir retry, cancelamento, continuacao parcial e isolamento de falhas por dataset. |
| Evolucao | G | Criar testes de integracao com SQLite temporario e fixtures reais | A suite atual cobre parsers, mas falha em massa. Reestruturar fixtures, banco temporario, encoding e cenarios de CSV/Excel/Access/DBF grandes. |

## Priorizacao inicial recomendada

1. Corrigir build e testes: Sentry local, `-DskipTests` no CI e 11 testes falhando.
2. Corrigir bugs de importacao/exportacao: parsers, CSV e comparacao de `String`.
3. Corrigir seguranca basica: SQL concatenado, logs sensiveis e senhas locais.
4. Depois atacar refatoracoes e evolucoes de UX/headless.
