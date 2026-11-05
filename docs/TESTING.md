# Testes e cobertura

Execute com Java 21:

```powershell
.\mvnw.cmd -B clean test
```

Em Linux/macOS: `./mvnw -B clean test`. Os testes de FXML precisam de um ambiente gráfico; em Linux sem sessão gráfica, use `xvfb-run -a ./mvnw -B clean test` com JavaFX/GTK disponíveis.

O comando executa JUnit, gera `target/site/jacoco/index.html` e verifica cobertura de **linhas >= 90% no núcleo de dados**. O relatório HTML/XML/CSV inclui o projeto inteiro, sem excluir interface, inicialização ou atualização. O percentual do núcleo não deve ser apresentado como cobertura global nem como percentual de todos os casos possíveis.

O núcleo verificado em `pom.xml`, execução `data-source-coverage`, inclui todos os parsers, gerenciadores, repositórios, `DataSet`, `DataSourceType`, `DataSetService`, `GoogleDriveFileService` e `DarwinCoreArchiveService`, incluindo classes internas.

A meta de **90% global ainda não foi atingida**. As maiores lacunas estão nos controladores JavaFX, inicialização e atualização/instalação. Consulte o relatório por classe para ver linhas e ramificações pendentes. A suíte testa alguns fluxos reais de componentes (arquivos + SQLite), além dos testes unitários com mocks.

## Fontes simuladas

| Fonte | Simulação e verificações |
| --- | --- |
| MySQL, MariaDB, SQL Server, Oracle | JDBC simulado: classificação da fonte, URL, credenciais e retorno da conexão; nenhuma conexão com servidor externo |
| PostgreSQL | JDBC e diagnóstico TCP simulados: SSL obrigatório, fallback, credenciais ausentes e preservação das duas falhas |
| CSV, TSV, TXT | Arquivos temporários: delimitadores, LF, aspas, separadores em células, células multilinha, nulos, colunas ausentes, seleção e gravação em lotes |
| Excel XLS/XLSX | Planilhas temporárias reais: números, booleanos, fórmulas, datas, células/colunas esparsas, várias abas, seleção, contagens e lotes |
| LibreOffice ODS | Planilhas temporárias reais: várias abas, seleção, valores nulos, contagens, lotes e rollback |
| Access MDB/ACCDB | Bancos Jackcess temporários V2000/V2003/V2007/V2010; consultas salvas, aliases, tabelas reservadas/ausentes e recursos JDBC simulados |
| dBase DBF | Arquivo DBF temporário e leitor simulado para variantes de tipos: Windows-1252, memo, moeda, decimal, double, lógico, data, timestamp, nulos, lotes e falha SQL |
| Numbers | Processo `osascript` simulado: sucesso, erro e interrupção; importação efetiva do XLSX convertido sem exigir macOS |
| Google Sheets/Drive | HTTP simulado: exportação, download direto, confirmação, UUID, cookies, identificação binária, erros HTTP/rede/HTML e remoção de arquivos temporários |
| Serviço central/API | `HttpService` simulado: objeto/lista JSON, resposta inválida, comandos SQL e recursos Darwin Core Data Package |
| Configuração e histórico locais | SQLite temporário criado com o schema de produção: gravação, atualização, consulta e exclusão |
| E-mail | `Transport.send` simulado: destinatário, remetente, assunto, conteúdo, configuração incompleta e falha de envio |

Os testes não validam os dialetos SQL contra servidores reais. A exportação é exercitada com SQLite temporário e a configuração dos cinco bancos é testada com JDBC simulado. Os artefatos ZIP/TSV/CSV e o esquema `datapackage.json` são inspecionados pelo conteúdo.

## Isolamento e execução

- Arquivos e bancos de teste ficam em `TemporaryFolder`; propriedades globais alteradas são restauradas após os testes.
- Os testes executam sequencialmente porque o aplicativo usa propriedades globais, eventos e campos estáticos de configuração.
- O upload de fontes para Sentry fica desabilitado sem credenciais. O perfil `sentry-source-upload` preserva o envio quando `SENTRY_AUTH_TOKEN` estiver definido; use `-Dsentry.skipSourceBundle=true` para desabilitá-lo explicitamente.
- O JavaFaker e as bases aleatórias com centenas de milhares de linhas foram substituídos por fixtures determinísticas. Testes específicos atravessam os limites de 1.000 e 10.000 registros dos importadores.
- O workflow `unit-tests.yml` executa a suíte no Windows e publica os relatórios de teste e cobertura.

## Limitações observadas

A biblioteca SODS 1.6.4 apresentou perda de conteúdo ao reler linhas idênticas compactadas como `table:number-rows-repeated` em ODS. As fixtures correntes usam valores distintos por linha; esse comportamento da dependência ainda precisa de uma correção e regressão próprias. Também faltam testes completos de navegação e ações dos controladores, inicialização e atualização para atingir 90% global.
