PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS BasicConfiguration (
  token VARCHAR(50) PRIMARY KEY, -- Código da coleção
  collection_name, -- nome da coleção do token correspondente
  last_rowcount INTEGER, -- quantidade de linhas no último sync
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS DataSourceConfiguration (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  token VARCHAR(50), -- Código da coleção
  datasource_filepath VARCHAR(50) NULL, -- caminho do arquivo quando a fonte de dados é um arquivo
  datasource_type VARCHAR(50) NOT NULL, -- tipo da fonte de dados
  db_host VARCHAR(100) NULL, -- host do banco de dados quando a fonte de dados é um BD
  db_port VARCHAR(100) NULL, -- porta do banco de dados quando a fonte de dados é um BD
  db_name VARCHAR(100) NULL, -- nome do banco de dados quando a fonte de dados é um BD
  db_tablename VARCHAR(100) NULL,  -- nome da tabela do banco de dados quando a fonte de dados é um BD
  db_username VARCHAR(100) NULL,  -- nome de usuário do banco de dados quando a fonte de dados é um BD
  db_password VARCHAR(100) NULL,  -- senha do banco de dados quando a fonte de dados é um BD
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
  FOREIGN KEY (token) REFERENCES BasicConfiguration (token)  -- chave primária
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

INSERT INTO TransferConfiguration (rsync_port, rsync_server_destination)
VALUES (10000, 'bruno@35.224.172.146');

INSERT INTO CentralServiceConfiguration (central_service_url, last_system_version)
VALUES ('https://specieslink.net/ws/1.0/splinker/', "1.0");