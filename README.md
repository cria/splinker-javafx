
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

## Modo Headless

A aplicação pode ser executada via terminal (sem interface gráfica):

```bash
java -jar splinker.jar path_arquivo_configuracao/config.txt
```

O arquivo de configuração deve conter, por exemplo:

```
VALOR_TOKEN
PATH_ARQUIVO_EXCEL
```

Exemplo de arquivo de um token de excel

```
XPTO23454
C:\Users\cria\Downloads\DadosExtraidos.xls
```

Exemplo de arquivo de um token de banco mySql

```
XPTO23454
host
porta
db_name
user
password
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

