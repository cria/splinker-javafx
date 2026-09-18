
# splinker

Este software foi desenvolvido pelo CRIA ([Centro de Referência em Informação Ambiental](https://www.cria.org.br/index)) com o objetivo de facilitar o envio de dados para a rede [speciesLink](https://specieslink.net/) de forma rápida e eficiente a partir de bases de dados locais (planilhas ou sistemas de gerenciamento de coleções biológicas).

## Tecnologias Utilizadas

- **Java 21**: linguagem principal.
- **JavaFX**: construção da interface gráfica.
- **SQLite**: armazenamento local temporário dos dados.
- **Maven**: gerenciamento de dependências e empacotamento.
- **rsync**: transmissão segura de arquivos para a rede speciesLink.

## Arquitetura da Aplicação

A estrutura segue o padrão MVC:

- **View (FXML)**: arquivos `.fxml` localizados em `resources/br/org/cria/splinkerapp`, representam a interface gráfica.
- **Controller**: cada view tem um controller correspondente para manipulação da interface e interações com o usuário.
- **Service**: camada com a lógica de negócio da aplicação.
- **Model**: mapeamento das entidades persistidas localmente.

### Visão do fluxo geral da aplicação

1. O usuário insere o **token da coleção** fornecido pelo CRIA.
2. A aplicação acessa a API do speciesLink e obtém a configuração da coleção.
3. O usuário informa o caminho da fonte de dados local (arquivo ou banco de dados).
4. Ao transmitir, os dados são importados e salvos localmente em SQLite.
5. A query configurada no speciesLink é executada.
6. Os dados são transformados em **Darwin Core Archive**.
7. O arquivo é transmitido via **rsync** para o servidor speciesLink.
8. Um histórico local da transmissão é mantido.
9. O usuário pode alterar as configurações, cadastrar novas coleções ou contatar o suporte por e-mail.

## Funcionalidades

- Inclusão de uma ou mais coleções com base em tokens fornecidos pelo CRIA.
- Suporte a diversas fontes de dados (Excel, CSV, Access, MySQL, Oracle, PostgreSQL etc.).
- Transmissão automatizada de dados.
- Execução em modo gráfico ou linha de comando.
- Registro de histórico de transmissões.
- Suporte multiplataforma (Windows, Linux, macOS).

## Instalação

Os instaladores e o arquivo executável para linha de comando ficam disponíveis na página de releases do GitHub:

[https://github.com/cria/splinker-javafx/releases](https://github.com/cria/splinker-javafx/releases)

Na versão `v7.4.5`, os arquivos podem ser baixados em:

- **Windows**: [`spLinker-7.4.5.msi`](https://github.com/cria/splinker-javafx/releases/download/v7.4.5/spLinker-7.4.5.msi)
- **Ubuntu/Debian**: [`spLinker-7.4.5.deb`](https://github.com/cria/splinker-javafx/releases/download/v7.4.5/spLinker-7.4.5.deb)
- **Linha de comando**: [`splinker.jar`](https://github.com/cria/splinker-javafx/releases/download/v7.4.5/splinker.jar)

### Windows

1. Baixe o instalador `.msi` da release desejada.
2. Execute o arquivo baixado, por exemplo `spLinker-7.4.5.msi`.
3. Siga as instruções do instalador.
4. Abra o spLinker pelo menu iniciar ou pelo atalho criado durante a instalação.

### Ubuntu/Debian

1. Baixe o pacote `.deb` da release desejada.
2. Instale pelo terminal:

```bash
sudo apt install ./spLinker-7.4.5.deb
```

Também é possível instalar com `dpkg`:

```bash
sudo dpkg -i spLinker-7.4.5.deb
sudo apt -f install
```

Depois da instalação, abra o spLinker pelo menu de aplicativos do sistema.

### Linha de comando

Para executar o spLinker por linha de comando, baixe o arquivo `splinker.jar` da release desejada.

Antes de executar, o Java deve estar instalado e disponível no `PATH`. Verifique com:

```bash
java -version
```

Se o comando não for reconhecido, instale o Java antes de continuar. O projeto usa Java 21.

Execute informando o caminho do arquivo de configuração:

```bash
java -jar splinker.jar caminho/para/config.txt
```

Exemplo no Windows:

```powershell
java -jar splinker.jar C:\Users\cria\Documents\splinker-config.txt
```

Exemplo no Linux/macOS:

```bash
java -jar splinker.jar /home/usuario/splinker-config.txt
```

### Comandos disponíveis

Além de executar uma transmissão a partir de um arquivo de configuração, o `splinker.jar` aceita os comandos abaixo:

```bash
java -jar splinker.jar <config-file.txt>
java -jar splinker.jar <config-file.txt> --collections=collectionA,collectionB
java -jar splinker.jar --version
java -jar splinker.jar --latest-version
java -jar splinker.jar --update
java -jar splinker.jar --example
java -jar splinker.jar --help
```

- `java -jar splinker.jar <config-file.txt>`: executa a transmissão usando o arquivo de configuração informado.
- `java -jar splinker.jar <config-file.txt> --collections=collectionA,collectionB`: executa somente as coleções informadas no filtro.
- `java -jar splinker.jar --version`: exibe a versão local instalada.
- `java -jar splinker.jar --latest-version`: consulta e exibe a versão mais recente publicada no GitHub.
- `java -jar splinker.jar --update`: verifica se existe uma versão mais recente e executa a atualização quando houver.
- `java -jar splinker.jar --example`: exibe um exemplo de arquivo de configuração.
- `java -jar splinker.jar --help`: exibe a ajuda com os comandos disponíveis.

## Arquivo de configuração para linha de comando

O arquivo de configuração é um arquivo texto simples formado por um ou mais blocos `[dataset]`.
Cada bloco representa uma coleção a ser processada e deve conter pares no formato `chave=valor`.

A aplicação identifica o tipo da fonte de dados a partir do token informado e da configuração cadastrada na API do speciesLink.
Por isso, os campos necessários variam conforme o tipo da fonte de dados.


### Fonte Excel

O arquivo txt deve possuir o conteudo nesse formato:

```text
[dataset]
token=TOKEN_DA_COLECAO
filePath=CAMINHO_DO_ARQUIVO_EXCEL
```

Exemplo no Windows:

```text
[dataset]
token=XPTO23454
filePath=C:\Users\cria\Downloads\DadosExtraidos.xls
```

Exemplo no Linux:

```text
[dataset]
token=XPTO23454
filePath=/home/usuario/dados/DadosExtraidos.xlsx
```

O mesmo formato com `filePath` é usado para fontes do tipo `Excel`, `LibreOfficeCalc`, `CSV`, `dBase`, `Numbers` e `GoogleSheets`.

### Fonte Access

O arquivo txt deve possuir o conteudo nesse formato:

```text
[dataset]
token=TOKEN_DA_COLECAO
filePath=CAMINHO_DO_ARQUIVO_ACCESS
password=SENHA_DO_ARQUIVO
```

Exemplo:

```text
[dataset]
token=XPTO23454
filePath=C:\Users\cria\Downloads\base.accdb
password=senha_do_arquivo
```

O campo `password` pode ser omitido quando o arquivo Access não tiver senha.

### Fonte SQL

O arquivo txt deve possuir o conteudo nesse formato:

```text
[dataset]
token=TOKEN_DA_COLECAO
host=HOST
port=PORTA
dbname=NOME_DO_BANCO
user=USUARIO
password=SENHA
```

Exemplo MySQL:

```text
[dataset]
token=XPTO23454
host=localhost
port=3306
dbname=colecao_biologica
user=usuario_splinker
password=senha_splinker
```

O mesmo formato é usado para fontes do tipo `MySQL`, `MariaDB`, `PostgreSQL`, `SQLServer` e `Oracle`.

### Múltiplas coleções

Para processar mais de uma coleção no mesmo comando, adicione um bloco `[dataset]` para cada uma:

```text
[dataset]
token=TOKEN_EXCEL
filePath=C:\Users\cria\Downloads\DadosExtraidos.xlsx

[dataset]
token=TOKEN_MYSQL
host=localhost
port=3306
dbname=colecao_biologica
user=usuario_splinker
password=senha_splinker
```

## Execução do Projeto

### Requisitos

- Java 21 instalado.
- Maven.

### Rodando via Maven

```bash
mvn javafx:run
```

### Classe principal

```text
br.org.cria.splinkerapp.App
```

Não é necessário definir variáveis de ambiente.

## Geração de Release

### Boas práticas

- Criar uma **branch** a partir da `master`.
- Realizar o desenvolvimento.
- Criar uma **pull request (PR)**.
- Após aprovação e merge na `master`, o GitHub Actions dispara o workflow de build e release automaticamente.

### Sobre o Workflow

O arquivo de workflow no GitHub Actions realiza:

1. **Testes automatizados** com Maven.
2. **Builds nativos** para:
    - Windows (MSI)
    - Linux (DEB e RPM)
    - macOS (JAR executável)
3. **Upload automático** dos artefatos.
4. **Criação de uma nova Release** com os arquivos.

A versão da aplicação é definida no `pom.xml`. Para uma nova release:

- **Atualize o `<version>` no pom.xml** seguindo o padrão SemVer (`MAJOR.MINOR.PATCH`).
- Faça merge na `master` para disparar o fluxo.

## Repositório

O código-fonte está disponível em:  
[https://github.com/cria/splinker-javafx](https://github.com/cria/splinker-javafx)

## Dependência Yajsync

O **splinker-javafx** utiliza o componente **Yajsync** publicado no GitHub Packages sob o grupo:

```
com.github.cria
```

Caso seja necessário realizar ajustes no Yajsync (correções, melhorias ou novas funcionalidades), siga o fluxo abaixo:

1. Acesse o repositório do Yajsync:  
   🔗 https://github.com/cria/yajsync

2. Faça suas alterações e **gere uma nova release** utilizando controle de versão semântico incremental:
   - v1 → v2 → v3 → v4 …  
     *(Atualmente, o `splinker-javafx` utiliza a versão `v1`)*

3. Após gerar a nova release, atualize a dependência no `pom.xml` do **splinker-javafx**, conforme o exemplo:

```xml
<dependency>
    <groupId>com.github.cria</groupId>
    <artifactId>yajsync</artifactId>
    <version>v2</version> <!-- Ajustar para a versão publicada -->
</dependency>
```

> 💡 Sempre garanta que a release do Yajsync esteja publicada no GitHub Packages antes de atualizar a dependência no projeto.


