# CRIA - Centro de Referência em Informação Ambiental

Este é um projeto desenvolvido para a ONG CRIA ([Centro de Referência em Informação Ambiental](https://www.cria.org.br/index)) com o objetivo de facilitar o envio de informações de pesquisas por pesquisadores para um banco de dados central, integrado ao [speciesLink](https://specieslink.net/).

# Descrição do Projeto
O CRIA Research Data Uploader é uma aplicação JavaFX que oferece uma interface amigável para que pesquisadores possam:

* Inserir dados de suas pesquisas.
* Validar as informações antes do envio.
* Realizar o upload dos dados para o banco de dados central.
A aplicação foi projetada para apoiar a comunidade científica, agilizando o compartilhamento e a centralização de dados ambientais.

# Tecnologias Utilizadas
* JavaFX: para o desenvolvimento da interface gráfica.
* Java: como linguagem principal.
* SQL: para integração com o banco de dados central.
* Maven/Gradle: para gerenciamento de dependências.

# Funcionalidades
* Cadastro de Pesquisas
* Interface para inserir detalhes da pesquisa (nome do pesquisador, título da pesquisa, área de estudo, etc.).
* Validação de Dados: Sistema para verificar inconsistências nos dados antes do envio.
* Envio ao Banco de Dados: Integração com o banco de dados central do speciesLink.
* Histórico de Envios: Registro das pesquisas enviadas pelo usuário.

# Como Rodar o Projeto
## Pré-requisitos
* Java 17 ou superior.
* Um gerenciador de dependências como Maven ou Gradle.
* Um banco de dados configurado e acessível.
## Passos para Instalação
1. Clone este repositório: bash Copiar código git clone https://github.com/<usuario>/<nome-do-repositorio>.git

2. Acesse o diretório do projeto: bash
Copiar código
cd nome-do-repositorio
3. Compile e rode o projeto: Com Maven: bash
Copiar código
mvn javafx:run
Com Gradle:
bash
Copiar código
gradle run
Siga as instruções exibidas na interface do aplicativo.
