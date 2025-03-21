PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS  DataSetConfiguration (
  -- CAMPOS VINDOS DO BACKEND
  id INT NOT NULL, -- ID da coleção
  token VARCHAR(50), -- Código da coleção
  dataset_name VARCHAR(50), -- nome da coleção do token correspondente
  dataset_acronym VARCHAR(50), -- acrônimo da coleção do token correspondente
  datasource_type VARCHAR(50) NOT NULL, -- tipo da fonte de dados(
  -- CAMPOS CONFIGURADOS PELO USUÁRIO
  datasource_filepath VARCHAR(50) NULL, -- caminho do arquivo quando a fonte de dados é um arquivo
  db_host VARCHAR(100) NULL, -- host do banco de dados quando a fonte de dados é um BD
  db_port VARCHAR(100) NULL, -- porta do banco de dados quando a fonte de dados é um BD
  db_name VARCHAR(100) NULL, -- nome do banco de dados quando a fonte de dados é um BD
  db_tablename VARCHAR(100) NULL,  -- nome da tabela do banco de dados quando a fonte de dados é um BD
  db_username VARCHAR(100) NULL,  -- nome de usuário do banco de dados quando a fonte de dados é um BD
  db_password VARCHAR(100) NULL,  -- senha do banco de dados quando a fonte de dados é um BD
  -- CAMPOS ATUALIZADOS AUTOMATICAMENTE
  last_rowcount INTEGER, -- quantidade de linhas no último sync
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME  CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS CentralServiceConfiguration (
  last_system_version VARCHAR(100) NOT NULL, --versão do spLinker
  central_service_url VARCHAR(100) NOT NULL, -- endereço do backend
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS TransferConfiguration (
  rsync_port INT NOT NULL,
  rsync_server_destination VARCHAR(500) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ProxyConfiguration (
  proxy_username VARCHAR(50) NOT NULL,
  proxy_password VARCHAR(100) NOT NULL,
  proxy_port VARCHAR(10) NOT NULL,
  proxy_address VARCHAR(100) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS TransferHistoryDataSet (
    token VARCHAR(20) NOT NULL,
    rowcount VARCHAR(10),
    send_date VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS EmailConfiguration (
    contact_email_send VARCHAR(50),
    contact_email_token VARCHAR(30),
    contact_email_recipient VARCHAR(50)
);

INSERT INTO CentralServiceConfiguration (central_service_url, last_system_version)
VALUES ('https://specieslink.net/ws/1.0/splinker/login', "1.0");